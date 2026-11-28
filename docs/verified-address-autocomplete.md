# Verified address autocomplete (Phase B)

Feature flags:

- **`ff_address_autocomplete`** (on in debug) — verified Nominatim AU suggestions
- **`ff_gnaf_address_geocode`** (on in debug) — G-NAF premise points when OSM lacks unit/housenumber
- **`ff_owned_address_pack`** (on in debug) — owned G-NAF pack SoT (ATNA-109 / ATNA-111 QLD ETL); see [`docs/owned-address-pack.md`](owned-address-pack.md)

Gherkin:

- [`features/verified_address_autocomplete.feature`](../features/verified_address_autocomplete.feature)
- [`features/address_autocomplete_housenumber.feature`](../features/address_autocomplete_housenumber.feature)
- [`features/owned_au_address_pack.feature`](../features/owned_au_address_pack.feature)

## Behaviour

| Surface | What ships |
| --- | --- |
| **Provider** | Pluggable `AddressAutocomplete`; default **Nominatim** (`countrycodes=au`) |
| **Precision** | Prefer housenumber / building Nominatim hits over highway street centroids |
| **G-NAF** | When flagged, local G-NAF pack resolves unit/housenumber to premise lat/lon |
| **Suggestions** | Formatted label + lat/lon + `AddressPrecision` (unit/house/building/street) |
| **Map-home add/find stops** | Typing shows suggestions; selecting one adds a stop |
| **Free text** | **Blocked** while autocomplete is on — no unverified typed-only addresses |
| **Persist** | `RouteStop.addressLabel` + non-null `latitude` / `longitude` via `RouteSession` |

Hosted on the Compose map-home drag sheet only (no new XML screens).

## Swap provider

Inject another `AddressAutocomplete` (or `AddressHttpClient` for Nominatim tests). The Stop contract stays the same: formatted label + coordinates.

## Nominatim

- Base: `https://nominatim.openstreetmap.org/search`
- Bias: `countrycodes=au`
- Params: `format=json`, `addressdetails=1`, `limit=5`
- User-Agent: `TruckieAndroid/0.8 (https://github.com/potsed/truckie; address-autocomplete)`
- Query ladder: free-text → number-keeping (`{house}, {locality}`) → structured `street`+`city` → street remainder
- Ranking: housenumber / building / landuse beats `highway` street centroids

Respect Nominatim usage policy (identify the app; avoid heavy polling).

**SoT:** **G-NAF data first only** (`ff_owned_address_pack`, ATNA-111) — [`docs/owned-address-pack.md`](owned-address-pack.md). One G-NAF pack/search path (`OwnedAddressAutocomplete`); no multi-dataset merges. **Nominatim = typeahead UI at most** until G-NAF resolves the hit. Interim `ff_gnaf_address_geocode` remains only when the owned pack flag is off.

## G-NAF (premise-level)

OSM often has the street (or a named complex) but **not** every AU unit. Truckie loads a
**local-first QLD sample pack** from G-NAF (CC-BY 4.0 via data.gov.au / OpenAddresses AU):

- Asset: `app/src/main/assets/gnaf/qld_sample_addresses.json`
- Flag: `ff_gnaf_address_geocode`
- Composer: `PremiseGeocodeAutocomplete` prefers G-NAF **UNIT** / **HOUSE** points over Nominatim street centroids
- Full Geoscape **G-NAF Core** or a state extract can replace the bootstrap JSON without changing `GnafAddressIndex`

### Kallangur example (`U21 1-31 Elsie Street Kallangur`)

| Source | Result |
| --- | --- |
| Nominatim free-text / street | Street centroid only (or empty with unit) |
| Nominatim `1-31, Kallangur` | Elsium Rise complex, house_number `1-31`, ~`-27.2544437, 152.9963696` (building) |
| **G-NAF UNIT 21** | `GAQLD163067541` → **`-27.25532783, 152.99620218`** (unit premise) |

Coordinates are never invented.

## Hard limits

- Public Nominatim / Photon cannot supply unit-level points missing from OSM.
- Shipping full national G-NAF (~16M rows) in-APK is impractical — use QLD-scoped / SEQ packs or a downloadable refresh (same pattern as bridge packs).
- G-NAF EULA is CC-BY-based with a **mail-sending** restriction (Geoscape); fine for navigation geocoding.
