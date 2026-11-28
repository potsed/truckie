Feature: Verified address autocomplete
  As a single-owner-operator driver
  I want stop addresses chosen from verified autocomplete suggestions
  So that every route stop has a formatted address with lat/lng (no free-text-only unverified addresses)

  # Product toggle: ff_address_autocomplete
  # Default provider: Nominatim (OpenStreetMap) with Australia country bias.
  # Compose Material3 map-home add/find stops UI only.

  Scenario: Address autocomplete is feature-flagged
    Given the feature flag ff_address_autocomplete is off
    Then the map-home add-stop path may still accept free-text labels
    When ff_address_autocomplete is enabled
    Then stop adds require a verified autocomplete suggestion with lat/lng

  Scenario: Pluggable provider with Nominatim AU default
    Given ff_address_autocomplete is enabled
    Then AddressAutocomplete is a pluggable interface
    And the default provider is Nominatim biased to Australia (countrycodes=au)
    And suggestions include a formatted display name plus latitude and longitude

  Scenario: Map-home add or find stops uses verified suggestions
    Given ff_map_home and ff_address_autocomplete are enabled
    And an active route on the map home sheet
    When the driver types into the add-or-find stops field
    Then verified suggestions are shown from the autocomplete provider
    And choosing a suggestion persists formatted address and lat/lng on the Stop
    And the free-text Add button cannot add an unverified address

  Scenario: Provider can be swapped without changing the Stop contract
    Given a fake AddressAutocomplete that returns a verified Brisbane suggestion
    When the driver selects that suggestion for pickup/load
    Then the RouteStop stores the formatted label with non-null latitude and longitude

  # House / unit numbers on street-level Nominatim hits:
  # see features/address_autocomplete_housenumber.feature (ATNA-108).
