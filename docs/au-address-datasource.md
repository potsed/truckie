# Owned AU address datasource (ATNA-109)

Plan for Truckie’s **first-party Australia address pack** — **G-NAF data first only** as the source of truth for stop geocoding (lat/lon + high-quality formatted address). Daniel reviews schema before full national ETL.

**Related:** [#69](https://github.com/potsed/truckie/pull/69) interim QLD G-NAF sample + `ff_gnaf_address_geocode`; Gherkin `features/owned_au_address_pack.feature`; flag `ff_owned_address_pack`.

---

## Goals

| Goal | Requirement |
| --- | --- |
| **Premise coords** | Verified lat/lon at **unit / house / building** precision from **G-NAF** — never invent; never navigate to a street centroid when a G-NAF premise point exists |
| **HQ formatted address** | Unit (optional) + number + street + locality + state + postcode (AU display conventions) |
| **Provenance** | Stable G-NAF citation (dataset name, URL, retrievedAt) on every row |
| **Stable id** | Durable address id = G-NAF `ADDRESS_DETAIL_PID` (e.g. `GAQLD163067541`) |
| **Fast search** | One G-NAF-backed pack/search path; autocomplete usable offline / local-first |
| **OSS** | Pack + ETL under G-NAF CC-BY / EULA + Truckie Apache-2.0 |

**Hard rule:** **G-NAF only** for address data. Do **not** design multi-dataset merges (OpenAddresses, OSM address points, state cadastral joins, etc.) that slow ETL or search. One pack, one search path.

Stop contract stays the same: selecting a suggestion yields a verified `Place` (label + lat/lon) for `RouteStop` / Navigate.

---

## Architecture: one G-NAF path

Daniel clarifications:

1. **Nominatim-as-engine is fine only when** the data behind search is Truckie’s better AU data — but the **data is G-NAF**, not a multi-source blend.
2. **G-NAF data first only** — no multi-dataset merges that slow things down.
3. **Nominatim = typeahead UI at most** until G-NAF resolves the hit.

### Chosen path (A′): Owned G-NAF local pack + search

- Versioned **G-NAF-only** JSON packs (QLD → state slices → AU), downloadable like the bridges pack
- Client FTS / Room index over that pack — **the only geocode SoT**
- `AddressAutocomplete` consults the G-NAF pack for the verified hit

### Nominatim role (thin typeahead only)

| Allowed | Not allowed |
| --- | --- |
| Typeahead / suggestion **labels** while the driver types, until a G-NAF pack hit resolves | Using public OSM/Nominatim coords as Navigate SoT |
| Optional self-hosted Nominatim **only if** it is loaded from the **same G-NAF pack** (no OSM address merge) | Merging Nominatim + G-NAF coords or ranking OSM premises over G-NAF |
| Fallback “no pack coverage yet” UX that still **blocks** unverified free-text | Multi-source geocode ladder as long-term architecture |

Interim (#69): Nominatim query ladder + QLD G-NAF sample may still help dogfood until the owned pack covers QLD — but the **target architecture is one G-NAF path**; Nominatim shrinks to typeahead-only.

### Rejected / deferred

| Option | Why |
| --- | --- |
| Multi-dataset ETL (G-NAF + OpenAddresses + OSM + …) | Slows pipeline; Daniel: G-NAF first only |
| Public Nominatim/OSM as SoT | AU units often missing (Kallangur U21) |
| Parallel “best of Nominatim vs G-NAF” merge at query time | Two paths, slower, fights SoT clarity |

Optional later OSS publish of a **G-NAF-fed** Nominatim instance is fine as a **distribution** of the same pack — not a second dataset.

---

## Schema (JSON primary)

Canonical wire format is **JSON only**. Optional on-device Room/FTS is a **derived index**, not a second SoT.

### Pack envelope

```json
{
  "manifest": {
    "region": "QLD",
    "version": "0.1.0-sample",
    "generatedAt": "2026-11-28",
    "source": {
      "name": "Geoscape G-NAF / G-NAF Core",
      "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
      "licence": "CC-BY 4.0 (G-NAF EULA — no bulk mail sending)",
      "retrievedAt": "2026-11-28"
    },
    "recordCount": 0,
    "schemaVersion": 1
  },
  "addresses": []
}
```

### Address row (required vs optional)

| Field | Required | Notes |
| --- | --- | --- |
| `id` | yes | G-NAF `ADDRESS_DETAIL_PID` |
| `lat` / `lon` | yes | G-NAF premise point; never null; never invented |
| `formatted` | yes | unit + number + street + locality + state + postcode |
| `number` | yes* | House / lot number from G-NAF |
| `street` | yes | Street name (+ type) |
| `locality` | yes | Suburb / locality |
| `state` | yes | `QLD` / `NSW` / … |
| `postcode` | yes | 4-digit AU |
| `unit` | no | When G-NAF has a unit |
| `precision` | yes | `unit` \| `house` \| `building` — from G-NAF level; avoid shipping `street` as SoT |
| `gnafPid` | no | Echo of PID if `id` is ever namespaced; default = same as `id` |
| `source` | yes | G-NAF citation `{ name, url, retrievedAt }` |

Prefer **null / omit** over inventing. No non-G-NAF address rows in the pack.

### Optional device index (Room / FTS)

Derived from the G-NAF JSON after download — FTS on normalised fields; rank `unit` > `house` > `building`.

---

## Source stack

| Source | Role |
| --- | --- |
| **G-NAF Core** (Geoscape / data.gov.au) | **Only** seed for packs |
| **Full G-NAF** | Only if Core lacks a field Truckie needs — still G-NAF family, not a merge with other address datasets |

**Update cadence:** Geoscape G-NAF / Core releases → Truckie pack semver via PR. Attribution: pack `source` + Licenses screen + `docs/licenses.md`.

---

## ETL pipeline shape

Mirror **au-low-bridges** / bridges pack, **G-NAF-only**:

1. Ingest G-NAF (Core first) → normalise → Truckie JSON (no other address datasets)
2. Region slices: `QLD` → states → `AU` (**not** APK-bundled national)
3. Bootstrap asset: empty envelope or tiny G-NAF sample under `assets/addresses/`
4. Versioned GitHub Release JSON; PR-based updates
5. Client: `loadActive` never networks; `refresh` only when remote newer

Flag: **`ff_owned_address_pack`**. Interim #69 `ff_gnaf_address_geocode` evolves into / is superseded by this.

---

## Client search API

```text
query → typeahead (Nominatim labels OK for UX only)
      → G-NAF pack index resolves hit (ff_owned_address_pack)
      → AddressSuggestion { label, placeId=G-NAF id, lat, lon from G-NAF }
      → driver selects → verified Place on RouteStop
```

Rules:

- **G-NAF resolves the hit** before Navigate uses coords
- Nominatim must not supply the Navigate coordinate when a G-NAF match exists or when the pack is the active SoT path
- Free-text-only unverified addresses stay blocked when `ff_address_autocomplete` is on
- Coords never invented; empty / wait for G-NAF > wrong premise

---

## How this relates to #69

| Layer | Target (ATNA-109) | #69 interim |
| --- | --- | --- |
| **G-NAF pack** | SoT | QLD sample via `ff_gnaf_address_geocode` |
| **Nominatim** | Typeahead UI at most until G-NAF resolves | Ladder + ranking still used for dogfood misses |
| **`AddressPrecision`** | Keep | Keep |
| **Multi-source merge** | **Out** | Do not expand |

---

## Size / performance notes

| Pack | Strategy |
| --- | --- |
| Empty / tiny sample | Bootstrap APK asset only |
| QLD G-NAF extract | Download + FTS; not multi-source enrich |
| National (~16M) | Region slices; **never** full dump in APK |

Keeping ETL single-source is part of the performance plan — no merge tax.

---

## Scaffold in this PR

- Flag `ff_owned_address_pack` (off by default; on in debug)
- Empty G-NAF pack JSON envelope + schema types
- Catalog when flag off = do not consult pack
- Unit tests + Gherkin — **no** national G-NAF dump

## Next build steps (after Daniel approves schema)

1. Confirm JSON fields / precision / `ADDRESS_DETAIL_PID` as `id`
2. QLD-first **G-NAF-only** ETL + Release hosting
3. `CachedAddressPackRepository` + FTS (TDD)
4. Wire: Nominatim typeahead → G-NAF resolve for the hit
5. Replace #69 sample with versioned QLD G-NAF pack
6. Attribution in Licenses + `docs/licenses.md`
