Feature: Address autocomplete preserves street numbers
  As a single-owner-operator driver
  I want verified autocomplete suggestions to keep the unit and house number I typed
  So that stops show a usable delivery address, not only the street name

  # Extends ff_address_autocomplete (Nominatim AU). No new product toggle.
  # Nominatim/OSM often has the street but not individual AU housenumbers;
  # Truckie keeps the verified place coordinates and preserves typed premise in the label.

  Scenario: Typed unit and house number appear on street-level Nominatim hits
    Given ff_address_autocomplete is enabled
    And Nominatim returns a verified Elsie Street Kallangur place without housenumber
    When the driver types "U21 1-31 Elsie Street Kallangur"
    Then suggestions remain verified Nominatim places with real coordinates
    And each suggestion label includes unit "U21" and house number "1-31"
    And coordinates are not invented

  Scenario: Full unit query falls back to street search when Nominatim returns empty
    Given ff_address_autocomplete is enabled
    And Nominatim returns no results for the full unit+number query
    And Nominatim returns Elsie Street when queried without the premise
    When the driver types "U21 1-31 Elsie Street Kallangur"
    Then a verified street-level suggestion is shown
    And the suggestion label preserves "U21" and "1-31"

  Scenario: Nominatim house-level hits keep their display name unchanged
    Given ff_address_autocomplete is enabled
    And Nominatim returns a place whose display name already includes house number "1"
    When the driver types "1 Queen Street Brisbane"
    Then the suggestion label is the Nominatim display name without duplicating the house number
