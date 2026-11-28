# Owned G-NAF address pack (ATNA-109 / ATNA-111)

Truckie’s stop-geocode **source of truth** is a versioned **G-NAF-only** JSON pack.
Nominatim is typeahead UI at most until a pack hit resolves — never Navigate SoT
when the owned pack covers the address.

| Flag | Role |
| --- | --- |
| `ff_owned_address_pack` | Owned pack SoT + local-first download/refresh |
| `ff_gnaf_address_geocode` | Interim #69 QLD sample path (superseded when owned pack is on) |
| `ff_address_autocomplete` | Verified suggestions required for stops |

Plan: [`docs/au-address-datasource.md`](au-address-datasource.md).
Gherkin: [`features/owned_au_address_pack.feature`](../features/owned_au_address_pack.feature).

## Scope (this release)

| Pack | Contents | Size strategy |
| --- | --- | --- |
| **Bootstrap (APK)** | QLD **Kallangur / 4503** dogfood slice (`0.1.0-kallangur-sample`) | Tiny (~81 premises) — includes `GAQLD163067541` (U21 Elsie St) |
| **Remote QLD** | Full QLD G-NAF via ETL → GitHub Release | Download like bridges; **not** APK-bundled |
| **National** | Later state slices → AU | Never full dump in APK |

## Pack schema

Envelope:

```json
{
  "manifest": {
    "region": "QLD",
    "version": "0.1.0-kallangur-sample",
    "generatedAt": "2026-11-28",
    "schemaVersion": 1,
    "recordCount": 81,
    "source": {
      "name": "Geoscape G-NAF",
      "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
      "licence": "CC-BY 4.0 (G-NAF EULA — no bulk mail sending)",
      "retrievedAt": "2026-11-28"
    }
  },
  "addresses": []
}
```

Row: `id` = G-NAF `ADDRESS_DETAIL_PID`, `lat`/`lon`, `formatted`, `number`, `street`,
`locality`, `state`, `postcode`, `precision` (`unit`|`house`|`building`),
`source{name,url,retrievedAt}`, optional `unit` / `gnafPid`. Prefer null over inventing.

Bootstrap asset: `app/src/main/assets/addresses/au_sample_addresses.json`.

## Run the ETL

```bash
# Self-test (CI / local)
python3 tools/gnaf-etl/gnaf_to_owned_pack.py --self-test

# Kallangur dogfood from OpenAddresses-style G-NAF JSON (still G-NAF PIDs)
python3 tools/gnaf-etl/gnaf_to_owned_pack.py \
  --input app/src/main/assets/gnaf/qld_sample_addresses.json \
  --state QLD --locality Kallangur \
  --version 0.1.0-kallangur-sample \
  --out app/src/main/assets/addresses/au_sample_addresses.json

# Full QLD from Geoscape G-NAF Core PSV extract (streaming filter)
# Download G-NAF Core from data.gov.au, unzip, then:
python3 tools/gnaf-etl/gnaf_to_owned_pack.py \
  --gnaf-dir /path/to/G-NAF/G-NAF\ Core/ \
  --state QLD \
  --version 0.1.0 \
  --out tools/gnaf-etl/out/qld-gnaf-0.1.0.json
```

**Hard rule:** G-NAF only — do not merge OpenAddresses / OSM / cadastral as second datasets.
OpenAddresses-style JSON is accepted only as a **G-NAF redistribution** (same `ADDRESS_DETAIL_PID`).

### Full QLD size / CI

| Concern | Mitigation |
| --- | --- |
| Full QLD ≈ millions of rows | Stream PSV; write Release JSON; do not commit full pack to git |
| CI / APK size | Tests use Kallangur fixture + ETL `--self-test` |
| Refresh | `CachedAddressPackRepository.refresh` only when remote version newer |

## Publish / download path (bridges pattern)

1. Run ETL → `qld-gnaf-<semver>.json`
2. Create GitHub Release tag `address-pack-v<semver>` on [potsed/truckie](https://github.com/potsed/truckie)
3. Attach `qld-gnaf-<semver>.json`
4. Bump `OwnedAddressPack.REMOTE_VERSION` / `DOWNLOAD_URL` in a follow-up PR if needed

Client:

- `loadActive` — on-device only (bundled → `filesDir/address_packs/`)
- `refresh(remoteManifest)` — HTTPS when `ff_owned_address_pack` on and remote newer
- Autocomplete: `OwnedAddressAutocomplete` — pack hits are SoT; Nominatim typeahead on miss

## Dogfood

1. Debug build (flags on: `ff_address_autocomplete`, `ff_owned_address_pack`, `ff_async_routing_io`)
2. Add stop → type `U21 1-31 Elsie Street Kallangur`
3. Expect verified suggestion `GAQLD163067541` at approx `-27.25532783, 152.99620218`

## Licence

Geoscape G-NAF / G-NAF Core: **CC-BY 4.0** + G-NAF EULA (**no bulk mail sending**).
Fine for navigation geocoding. See [`docs/licenses.md`](licenses.md).
