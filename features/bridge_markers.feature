Feature: Bridge markers on home map
  As a single-owner-operator driver
  I want low-bridge markers on the MapLibre home map
  So that I can see clearance constraints before planning a route

  # Product toggle: ff_bridge_markers
  # Long-term data: downloadable updatable AU pack (see bridge_pack.feature).
  # Baked-in sample asset is bootstrap only when the pack cache is empty.
  # Rendering: GeoJSONSource + style symbol layer (ATNA-117) — not Annotation
  # SymbolManager. See map_style_overlays.feature for shared overlay contracts.
  # Out of scope: Phase C vehicle UI, create-route changes, global header/footer,
  # live OSM/NHVR pack ingest host, truck routing model routing.

  Scenario: Bridge markers are feature-flagged
    Given the feature flag ff_bridge_markers is off
    Then the home map does not show bridge markers
    When ff_bridge_markers is enabled
    And ff_map_home is enabled
    Then bridge markers are visible on the MapLibre home map

  Scenario: Sample Australian bridge points load from a local asset
    Given ff_bridge_markers is enabled
    When the home map loads bridge fixture data
    Then bridges are loaded from a local Australia-first JSON asset
    And each bridge has an id, name, latitude, and longitude
    And clearance height is optional
    And several sample bridges sit within 2 km of Brisbane CBD fallback

  Scenario: Demo samples sit on real Brisbane CBD crossings
    Given ff_bridge_markers is enabled
    And the Australia-first sample asset is loaded
    Then sample points snap onto Story Bridge, Victoria Bridge, Captain Cook / M3,
      Goodwill, William Jolly, Go Between, and Kurilpa
    And markers are not mid-river or mid-block away from those crossings
    And clearance labels are verified published road heights or omitted (ATNA-42)

  Scenario: Markers stay visible across map recompositions
    Given ff_bridge_markers is enabled
    And the MapLibre style has loaded once
    When location updates cause Compose to recompose the map
    Then the style is not reloaded
    And concurrent AndroidView updates coalesce into a single setStyle
    And GeoJSON overlay layers are rebound on the live style so bridge markers remain visible
    And if the live style identity drifts or an overlay layer is missing, overlays are recreated

  Scenario: Demo mode CBD sample cluster still shows W5-8 markers
    Given ff_bridge_markers and ff_demo_mode are enabled
    And the pack has seeded SEQ bridges near Brisbane CBD
    When the MapLibre camera is over the Brisbane CBD sample cluster
    Then W5-8 style-layer icons remain visible (not orphaned by style reload races)

  Scenario: Clearance labels use OpenFreeMap-compatible text fonts
    Given ff_bridge_markers is enabled
    And the basemap is OpenFreeMap Liberty
    When bridge markers include clearance text-field labels
    Then symbol-layer text-font is Noto Sans Regular
    And MapLibre’s default Open Sans stack is not requested
    And textOptional is true so icons still paint if glyphs fail
    And W5-8 icons remain visible over the Brisbane CBD sample cluster

  Scenario: Markers use style symbol layer with optional clearance labels
    Given ff_bridge_markers is enabled
    And sample bridges include a height clearance
    Then bridge markers use a GeoJSONSource symbol layer not deprecated MarkerOptions
    And the marker icon is the AU W5-8 low-bridge road sign only for qualifying low bridges
      (see low_bridge_w5_8.feature / ff_w5_8_low_only)
    And a short clearance label is shown when a verified height is present on a W5-8 marker
    And unknown / not_applicable / high spans do not get W5-8
    And clearance labels use an OpenFreeMap-hosted fontstack

  Scenario: Bridge markers stay Compose-only on map home
    Given ff_map_home and ff_bridge_markers are enabled
    Then bridge markers render on the map-first home MapLibre surface
    And no global header or footer is added for this feature
