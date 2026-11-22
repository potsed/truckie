Feature: Polished map-home route stop drag sheet
  As a single-owner-operator driver
  I want the route/stop drag sheet to look production-ready
  So that create-route, add/find stops, and the stop list feel intentional on the map

  # Product toggle: ff_map_home (polish lands with map home; no separate phase flag)
  # Spoke-inspired layout shape only — Truckie light high-contrast Material3, not a clone.
  # Out of scope: Phase C vehicle depth, live routing, tolls, load optimisation.

  Scenario: Sheet chrome uses Material3 surfaces with a clear drag handle
    Given ff_map_home is enabled
    Then the route sheet uses elevated Material3 surface with large top corner radius
    And a high-contrast Material3 drag handle is centred above the summary
    And peek half and full still snap via AnchoredDraggable nested-scroll gestures

  Scenario: Peek summary is readable over the map
    Given the route sheet is in peek
    Then the summary strip uses title-small typography on high-contrast on-surface
    And horizontal content padding leaves clear map/sheet breathing room

  Scenario: Create route form has intentional hierarchy
    Given half or full sheet with no active route
    Then Create route uses a headline title and labelled optional name field
    And Today Tomorrow and Pick a date are outlined selectable cards
    And Confirm is a full-width high-contrast primary button

  Scenario: Add find stops and stop list look intentional
    Given an active route on half or full sheet
    Then Add or find stops shows a search field with leading icon
    And pickup/load and drop-off/unload are filter chips
    And address suggestions render as tappable surface rows when flagged
    And the stop list uses numbered timeline badges with kind labels
    And domain wording stays Route and Stops with pickup/load or drop-off/unload

  Scenario: Map home chrome stays sheet-local
    Given ff_map_home is enabled
    Then the polished sheet does not add a global TopAppBar or bottom NavigationBar
