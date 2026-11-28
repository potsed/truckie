#!/usr/bin/env python3
"""G-NAF → Truckie owned address pack ETL (ATNA-111).

G-NAF data first only — no multi-dataset merges.
Supports:
  1. OpenAddresses-style G-NAF JSON extracts (id = gnafPid / ADDRESS_DETAIL_PID)
  2. Geoscape G-NAF Core PSV directory (ADDRESS_DETAIL + GEOCODE + STREET_LOCALITY + LOCALITY)

QLD-first: filter with --state QLD; optionally --locality / --postcode for dogfood slices.
Full QLD is large — stream/filter; do not APK-bundle national dumps.

Usage:
  python3 tools/gnaf-etl/gnaf_to_owned_pack.py \\
    --input tools/gnaf-etl/testdata/gnaf_openaddresses_mini.json \\
    --state QLD --out /tmp/qld-gnaf.json

  python3 tools/gnaf-etl/gnaf_to_owned_pack.py --self-test

Docs: docs/owned-address-pack.md
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from pathlib import Path
from typing import Any, Iterable, Iterator

GNAF_DATASET_URL = (
    "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc"
)
GNAF_SOURCE_NAME = "Geoscape G-NAF"
DEFAULT_RETRIEVED = "2026-11-28"
SCHEMA_VERSION = 1
UNIT_RE = re.compile(r"(?i)(?:unit\s*|u\.?\s*)?(\d+[A-Za-z]?)")


def unit_digits(raw: str | None) -> str | None:
    if not raw:
        return None
    m = UNIT_RE.search(str(raw).strip())
    return m.group(1) if m else None


def format_address(
    unit: str | None,
    number: str,
    street: str,
    locality: str,
    state: str,
    postcode: str,
) -> str:
    parts = []
    if unit:
        parts.append(f"U{unit}")
    if number:
        parts.append(number)
    parts.extend([p for p in (street, locality, state, postcode) if p])
    return " ".join(parts)


def row_from_openaddresses(raw: dict[str, Any], retrieved_at: str) -> dict[str, Any] | None:
    pid = (raw.get("gnafPid") or raw.get("id") or "").strip()
    if not pid:
        return None
    lat = raw.get("latitude", raw.get("lat"))
    lon = raw.get("longitude", raw.get("lon"))
    if lat is None or lon is None:
        return None
    number = str(raw.get("houseNumber") or raw.get("number") or "").strip()
    street = str(raw.get("street") or "").strip()
    locality = str(raw.get("locality") or "").strip()
    state = str(raw.get("state") or "").strip().upper()
    postcode = str(raw.get("postcode") or "").strip()
    if not street or not locality or not state or not postcode:
        return None
    unit = unit_digits(raw.get("unit"))
    precision = "unit" if unit else "house"
    source = {
        "name": GNAF_SOURCE_NAME,
        "url": GNAF_DATASET_URL,
        "retrievedAt": retrieved_at,
    }
    row: dict[str, Any] = {
        "id": pid,
        "lat": float(lat),
        "lon": float(lon),
        "formatted": format_address(unit, number, street, locality, state, postcode),
        "number": number,
        "street": street,
        "locality": locality,
        "state": state,
        "postcode": postcode,
        "precision": precision,
        "gnafPid": pid,
        "source": source,
    }
    if unit:
        row["unit"] = unit
    return row


def iter_openaddresses_json(path: Path) -> Iterator[dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        yield from data
        return
    if isinstance(data, dict) and "addresses" in data:
        yield from data["addresses"]
        return
    raise SystemExit(f"Unrecognised JSON shape in {path}")


def find_psv(gnaf_dir: Path, prefix: str) -> Path:
    matches = sorted(gnaf_dir.rglob(f"{prefix}_*.psv")) + sorted(
        gnaf_dir.rglob(f"{prefix}.psv")
    )
    if not matches:
        raise SystemExit(f"Missing {prefix}*.psv under {gnaf_dir}")
    return matches[0]


def load_psv_map(path: Path, key: str) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    with path.open(newline="", encoding="utf-8", errors="replace") as fh:
        reader = csv.DictReader(fh, delimiter="|")
        for row in reader:
            kid = (row.get(key) or "").strip()
            if kid:
                out[kid] = row
    return out


def iter_gnaf_core_psv(gnaf_dir: Path) -> Iterator[dict[str, Any]]:
    """Stream Geoscape G-NAF Core PSV joins into OpenAddresses-like dicts."""
    detail_path = find_psv(gnaf_dir, "ADDRESS_DETAIL")
    geocode_path = find_psv(gnaf_dir, "ADDRESS_DEFAULT_GEOCODE")
    street_path = find_psv(gnaf_dir, "STREET_LOCALITY")
    locality_path = find_psv(gnaf_dir, "LOCALITY")

    geocodes = load_psv_map(geocode_path, "ADDRESS_DETAIL_PID")
    streets = load_psv_map(street_path, "STREET_LOCALITY_PID")
    localities = load_psv_map(locality_path, "LOCALITY_PID")

    with detail_path.open(newline="", encoding="utf-8", errors="replace") as fh:
        reader = csv.DictReader(fh, delimiter="|")
        for detail in reader:
            pid = (detail.get("ADDRESS_DETAIL_PID") or "").strip()
            if not pid:
                continue
            geo = geocodes.get(pid)
            if not geo:
                continue
            try:
                lat = float(geo.get("LATITUDE") or "")
                lon = float(geo.get("LONGITUDE") or "")
            except ValueError:
                continue
            street_pid = (detail.get("STREET_LOCALITY_PID") or "").strip()
            locality_pid = (detail.get("LOCALITY_PID") or "").strip()
            street_row = streets.get(street_pid, {})
            locality_row = localities.get(locality_pid, {})
            street_name = (street_row.get("STREET_NAME") or "").strip()
            street_type = (street_row.get("STREET_TYPE_CODE") or "").strip()
            street = f"{street_name} {street_type}".strip().title() if street_name else ""
            if street_type and street_name:
                street = f"{street_name.title()} {street_type.title()}"
            number_first = (detail.get("NUMBER_FIRST") or "").strip()
            number_last = (detail.get("NUMBER_LAST") or "").strip()
            number = (
                f"{number_first}-{number_last}"
                if number_first and number_last and number_last != number_first
                else number_first
            )
            flat = (detail.get("FLAT_NUMBER") or "").strip()
            flat_type = (detail.get("FLAT_TYPE_CODE") or "").strip()
            unit = f"{flat_type} {flat}".strip() if flat else None
            yield {
                "gnafPid": pid,
                "houseNumber": number,
                "street": street,
                "unit": unit,
                "latitude": lat,
                "longitude": lon,
                "locality": (locality_row.get("LOCALITY_NAME") or "").strip().title(),
                "postcode": (detail.get("POSTCODE") or locality_row.get("POSTCODE") or "").strip(),
                "state": (locality_row.get("STATE_ABBREVIATION") or "").strip().upper(),
            }


def filter_rows(
    rows: Iterable[dict[str, Any]],
    *,
    state: str | None,
    locality: str | None,
    postcode: str | None,
) -> list[dict[str, Any]]:
    state_u = state.upper() if state else None
    locality_l = locality.lower() if locality else None
    out: list[dict[str, Any]] = []
    for raw in rows:
        st = str(raw.get("state") or "").upper()
        if state_u and st != state_u:
            continue
        if locality_l and str(raw.get("locality") or "").lower() != locality_l:
            continue
        if postcode and str(raw.get("postcode") or "") != postcode:
            continue
        out.append(raw)
    return out


def build_pack(
    raw_rows: list[dict[str, Any]],
    *,
    region: str,
    version: str,
    generated_at: str,
    retrieved_at: str,
) -> dict[str, Any]:
    addresses: list[dict[str, Any]] = []
    for raw in raw_rows:
        row = row_from_openaddresses(raw, retrieved_at)
        if row is None:
            continue
        addresses.append(row)
    return {
        "manifest": {
            "region": region,
            "version": version,
            "generatedAt": generated_at,
            "schemaVersion": SCHEMA_VERSION,
            "recordCount": len(addresses),
            "source": {
                "name": GNAF_SOURCE_NAME,
                "url": GNAF_DATASET_URL,
                "licence": "CC-BY 4.0 (G-NAF EULA — no bulk mail sending)",
                "retrievedAt": retrieved_at,
            },
        },
        "addresses": addresses,
    }


def run_self_test() -> None:
    fixture = Path(__file__).resolve().parent / "testdata" / "gnaf_openaddresses_mini.json"
    raw = filter_rows(
        list(iter_openaddresses_json(fixture)),
        state="QLD",
        locality=None,
        postcode=None,
    )
    pack = build_pack(
        raw,
        region="QLD",
        version="0.0.0-test",
        generated_at=DEFAULT_RETRIEVED,
        retrieved_at=DEFAULT_RETRIEVED,
    )
    assert pack["manifest"]["region"] == "QLD"
    assert pack["manifest"]["recordCount"] == 2
    assert all(a["state"] == "QLD" for a in pack["addresses"])
    assert all(a["id"].startswith("GAQLD") for a in pack["addresses"])
    u21 = next(a for a in pack["addresses"] if a["id"] == "GAQLD163067541")
    assert u21["unit"] == "21"
    assert abs(u21["lat"] - (-27.25532783)) < 1e-8
    assert abs(u21["lon"] - 152.99620218) < 1e-8
    assert u21["precision"] == "unit"
    assert "source" in u21 and u21["source"]["url"] == GNAF_DATASET_URL
    print("gnaf_to_owned_pack self-test OK")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, help="OpenAddresses-style G-NAF JSON")
    parser.add_argument(
        "--gnaf-dir",
        type=Path,
        help="Geoscape G-NAF Core extract directory (PSV files)",
    )
    parser.add_argument("--state", default="QLD", help="State filter (default QLD)")
    parser.add_argument("--locality", default=None, help="Optional locality filter")
    parser.add_argument("--postcode", default=None, help="Optional postcode filter")
    parser.add_argument("--version", default="0.1.0", help="Pack semver")
    parser.add_argument("--generated-at", default=DEFAULT_RETRIEVED)
    parser.add_argument("--retrieved-at", default=DEFAULT_RETRIEVED)
    parser.add_argument("--out", type=Path, help="Output owned-pack JSON path")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        run_self_test()
        return 0

    if not args.out:
        parser.error("--out is required unless --self-test")
    if bool(args.input) == bool(args.gnaf_dir):
        parser.error("Provide exactly one of --input or --gnaf-dir")

    if args.input:
        raw_iter: Iterable[dict[str, Any]] = iter_openaddresses_json(args.input)
    else:
        raw_iter = iter_gnaf_core_psv(args.gnaf_dir)

    filtered = filter_rows(
        raw_iter,
        state=args.state,
        locality=args.locality,
        postcode=args.postcode,
    )
    region = args.state.upper() if args.state else "AU"
    pack = build_pack(
        filtered,
        region=region,
        version=args.version,
        generated_at=args.generated_at,
        retrieved_at=args.retrieved_at,
    )
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(pack, indent=2) + "\n", encoding="utf-8")
    print(
        f"Wrote {pack['manifest']['recordCount']} G-NAF rows "
        f"region={region} version={args.version} → {args.out}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
