Feature: Address autocomplete preserves street numbers
  As a single-owner-operator driver
  I want verified autocomplete to resolve a precise premise coordinate
  So that Navigate goes to the unit or house, not a street centroid

  # Extends ff_address_autocomplete (Nominatim AU).
  # Precise G-NAF premise geocode is gated by ff_gnaf_address_geocode.
  # Never invent coordinates — Nominatim and/or G-NAF only.

  Scenario: Prefer Nominatim hits that include housenumber or building
    Given ff_address_autocomplete is enabled
    And Nominatim can return both a highway street hit and a housenumber landuse hit
    When the driver types an address that includes house number "1-31"
    Then suggestions prefer the housenumber or building result over the street centroid
    And coordinates come from the housenumber hit

  Scenario: Number-keeping Nominatim query finds complex when free-text is street-only
    Given ff_address_autocomplete is enabled
    And free-text "1-31 Elsie Street Kallangur" returns only the street
    And the number-keeping query "1-31, Kallangur" returns Elsium Rise with house_number "1-31"
    When the driver types "1-31 Elsie Street Kallangur"
    Then the selected suggestion uses the Elsium Rise housenumber coordinates

  Scenario: G-NAF resolves unit premise when OSM lacks the unit
    Given ff_address_autocomplete and ff_gnaf_address_geocode are enabled
    And Nominatim has no unit-level hit for "U21 1-31 Elsie Street Kallangur"
    And the local G-NAF QLD sample pack contains UNIT 21 at 1-31 Elsie Street Kallangur
    When the driver types "U21 1-31 Elsie Street Kallangur"
    Then a verified suggestion uses the G-NAF UNIT 21 latitude and longitude
    And the label includes unit 21 and house number 1-31
    And coordinates are not invented

  Scenario: Without G-NAF flag street-only Nominatim is not treated as precise premise
    Given ff_address_autocomplete is enabled
    And ff_gnaf_address_geocode is off
    And only a highway street hit is available for a typed housenumber
    When number-keeping Nominatim queries also fail
    Then Truckie must not claim a street centroid is a housenumber premise
