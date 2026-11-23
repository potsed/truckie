#!/usr/bin/env python3
"""Merge TMR Structure Clearances + BCC road-restrictions into AU bridge pack.

ATNA-46 — retrieved 2026-11-23. Prefer published GIS heights; never invent.
De-dupe by id, clearanceSourceId, and ~35 m proximity (keep CBD/QR first).
Physical roadside signs override pack data.
"""
from __future__ import annotations

import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets/bridges"
RAW = Path(__file__).resolve().parent
RETRIEVED = "2026-11-23"
DEDUP_METRES = 35.0
TMR_DATASET = "https://www.data.qld.gov.au/dataset/structure-clearances-queensland"
TMR_REST = (
    "https://spatial-gis.information.qld.gov.au/arcgis/rest/services/"
    "Transportation/StateRoadInformation/MapServer"
)
BCC_DATASET = "https://data.brisbane.qld.gov.au/explore/dataset/road-restrictions/"
TMR_PROVIDER = "tmr_structure_clearances"
BCC_PROVIDER = "bcc_road_restrictions"


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlmb = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlmb / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def slug(text: str) -> str:
    s = re.sub(r"[^a-zA-Z0-9]+", "-", text.strip().lower()).strip("-")
    return s or "unknown"


def near_existing(lat: float, lng: float, pack: list[dict]) -> bool:
    for row in pack:
        if haversine_m(lat, lng, row["lat"], row["lng"]) < DEDUP_METRES:
            return True
    return False


def source_ids(pack: list[dict]) -> set[str]:
    return {r["clearanceSourceId"] for r in pack if r.get("clearanceSourceId")}


def ids(pack: list[dict]) -> set[str]:
    return {r["id"] for r in pack}


def add_row(pack: list[dict], row: dict) -> bool:
    if row["id"] in ids(pack):
        return False
    sid = row.get("clearanceSourceId")
    if sid and sid in source_ids(pack):
        return False
    if near_existing(row["lat"], row["lng"], pack):
        return False
    pack.append(row)
    return True


def bcc_rows() -> list[dict]:
    raw = json.loads((RAW / "bcc_road_restrictions.json").read_text())
    out: list[dict] = []
    for r in raw:
        height = r.get("signed_clearance_min")
        if height is None or height <= 0 or height > 4.6:
            continue
        rid = (r.get("restriction_id") or "").strip()
        if not rid:
            continue
        desc = (r.get("description") or rid).strip()
        lat = float(r["lat"])
        lng = float(r["long"])
        out.append(
            {
                "id": f"qld-bcc-{slug(rid)}",
                "name": desc,
                "lat": lat,
                "lng": lng,
                "clearanceHeightM": float(height),
                "clearanceStatus": "verified",
                "clearanceSource": (
                    "Brisbane City Council Open Data — Road height restrictions "
                    f"(signed_clearance_min). {BCC_DATASET}"
                ),
                "clearanceSourceId": rid,
                "clearanceRetrievedAt": RETRIEVED,
                "clearanceProvider": BCC_PROVIDER,
            }
        )
    return out


def tmr_rows() -> list[dict]:
    rows: list[dict] = []
    for name in ("tmr_lt_4_6.json", "tmr_4_6_to_5_4.json"):
        for r in json.loads((RAW / name).read_text()):
            height = r.get("vertical_clearance_m")
            sid = str(r.get("structure_id") or "").strip()
            lat = r.get("latitude")
            lng = r.get("longitude")
            if height is None or not sid or lat is None or lng is None:
                continue
            if float(height) <= 0 or float(height) >= 5.4:
                continue
            sname = (r.get("structure_name") or f"TMR structure {sid}").strip()
            on_roads = (r.get("over_roads") or r.get("on_roads") or "").strip()
            label = sname if not on_roads else f"{sname} ({on_roads.title()})"
            rows.append(
                {
                    "id": f"qld-tmr-{slug(sid)}",
                    "name": label[:120],
                    "lat": float(lat),
                    "lng": float(lng),
                    "clearanceHeightM": float(height),
                    "clearanceStatus": "verified",
                    "clearanceSource": (
                        "TMR Structure Clearances in Queensland (machine-readable spatial) "
                        f"— {TMR_DATASET} via StateRoadInformation MapServer "
                        f"({TMR_REST}). Prefer published TMR vertical_clearance_m; "
                        "do not invent heights."
                    ),
                    "clearanceSourceId": f"TMR-{sid}",
                    "clearanceRetrievedAt": RETRIEVED,
                    "clearanceProvider": TMR_PROVIDER,
                }
            )
    return rows


def main() -> None:
    pack = json.loads((ASSETS / "au_sample_bridges.json").read_text())
    before = len(pack)
    added_bcc = 0
    for row in bcc_rows():
        if add_row(pack, row):
            added_bcc += 1
    added_tmr = 0
    for row in tmr_rows():
        if add_row(pack, row):
            added_tmr += 1

    # Unique ids sanity
    assert len(pack) == len({r["id"] for r in pack})

    (ASSETS / "au_sample_bridges.json").write_text(
        json.dumps(pack, indent=2, ensure_ascii=False) + "\n"
    )

    meta = {
        "region": "AU",
        "pack": "brisbane-cbd-qr-seq-tmr-bcc",
        "version": "0.6.0-sample",
        "retrievedAt": RETRIEVED,
        "clearancePolicy": (
            "Use only published signed road vertical clearances for vehicles "
            "(TMR Structure Clearances spatial layers, BCC Open Data road-restrictions, "
            "Queensland Rail SEQ low bridges, TMR Bridge/Culvert Restrictions notices). "
            "Prefer TMR/BCC/QR published figures over invented heights. "
            "Physical roadside signs always override digital pack data — packs can be "
            "inaccurate or stale (e.g. after resurfacing). The in-app clearance buffer "
            "exists in part for that data lag versus signs, not only vehicle/cargo variance. "
            "If a crossing has no published road clearance (high open river spans), omit "
            "clearanceHeightM and set clearanceStatus to unknown. Pedestrian/cycle-only "
            "bridges are not_applicable. Never invent mock demo heights or heights for "
            "bridges absent from the cited sources. Never treat Maritime Safety Queensland "
            "vessel air draft above HAT as a truck road clearance."
        ),
        "sources": [
            (
                f"TMR Structure Clearances in Queensland, retrieved {RETRIEVED}: "
                f"{TMR_DATASET} (ArcGIS StateRoadInformation MapServer layers 21 "
                f"<4.6 m and 22 ≥4.6–<5.4 m: {TMR_REST})"
            ),
            (
                f"TMR Queensland — Bridge and culvert restrictions hub, retrieved {RETRIEVED}: "
                "https://www.tmr.qld.gov.au/business-industry/heavy-vehicles/"
                "bridge-and-culvert-restrictions (prefer with Structure Clearances; "
                "not used as invented height)"
            ),
            (
                f"Brisbane City Council Open Data — Road height restrictions "
                f"(dataset road-restrictions), retrieved {RETRIEVED}: {BCC_DATASET}"
            ),
            (
                "Queensland Rail — Low rail bridges in South East Queensland, "
                f"retrieved {RETRIEVED}: "
                "https://www.queenslandrail.com.au/forcustomers/safetysecurity/railbridges "
                "(road-under-rail signed clearances; location map coordinates from the same page)"
            ),
            (
                "BCC B0001 - 13 / B0001 - 14 — Kerbside Lane under Bradfield Highway/Story Bridge "
                "— signed_clearance_min 3.8 m (validation_date 2020-10-08)"
            ),
            (
                "BCC B0002 - 298 — South-East Arterial Road under Victoria Bridge — Southeast "
                "— signed_clearance_min 4.4 m (validation_date 2020-10-08); "
                "B0002 - 213 Cultural Centre Tunnel under Victoria Bridge also 4.4 m"
            ),
            (
                "Maritime Safety Queensland — Port Procedures and Information for Shipping "
                "– Brisbane (August 2024) Table 23 air draft/bridge heights above HAT "
                "— vessel navigation only; explicitly excluded from road clearanceHeightM"
            ),
        ],
    }
    (ASSETS / "au_sample_bridges_meta.json").write_text(
        json.dumps(meta, indent=2, ensure_ascii=False) + "\n"
    )
    print(
        f"pack {before} → {len(pack)} "
        f"(+BCC {added_bcc}, +TMR {added_tmr}); "
        f"dedupe radius {DEDUP_METRES} m"
    )


if __name__ == "__main__":
    main()
