# Verified address autocomplete (Phase B)

Feature flag: **`ff_address_autocomplete`** (on in debug). Gherkin: [`features/verified_address_autocomplete.feature`](../features/verified_address_autocomplete.feature).

## Behaviour

| Surface | What ships |
| --- | --- |
| **Provider** | Pluggable `AddressAutocomplete`; default **Nominatim** (`countrycodes=au`) |
| **Suggestions** | Formatted `display_name` + `lat` / `lon` (verified only) |
| **Map-home add/find stops** | Typing shows suggestions; selecting one adds a stop |
| **Free text** | **Blocked** while the flag is on — no unverified typed-only addresses |
| **Persist** | `RouteStop.addressLabel` + non-null `latitude` / `longitude` via `RouteSession` |

Hosted on the Compose map-home drag sheet only (no new XML screens).

## Swap provider

Inject another `AddressAutocomplete` (or `AddressHttpClient` for Nominatim tests). The Stop contract stays the same: formatted label + coordinates.

## Nominatim

- Base: `https://nominatim.openstreetmap.org/search`
- Bias: `countrycodes=au`
- User-Agent: `TruckieAndroid/0.8 (https://github.com/potsed/truckie; address-autocomplete)`

Respect Nominatim usage policy (identify the app; avoid heavy polling).
