Feature: Show next destination address while navigating
  As a single-owner-operator human driver
  I want the upcoming stop address always visible while navigating
  So that I can take a better route I know — Truckie is for drivers, not self-driving

  # Product toggle: ff_next_destination_display (requires ff_navigate + active Navigate).
  # Phone: compact / subtle Compose chip on map-home (optional via flag — avoid clutter).
  # Android Auto (ATNA-10): when Auto navigation lands, next destination MUST show
  # clearly on the car screen (first-class alongside turn-by-turn — not optional there).
  # Address source: upcoming RouteStop by order; when no intermediate stops, route end.
  # Destination address is first-class alongside turn-by-turn (not turn-only chrome).
  # Out of scope: MapLibre LineManager / MapRoutePathAnnotations (ATNA-82 polyline).
  # Out of scope: satellite peek, external handoff UI changes, Auto UI implementation.

  Scenario: Next destination display is feature-flagged
    Given the feature flag ff_next_destination_display is off
    And Navigate has an active in-app path
    Then the next destination address banner is not shown
    When ff_next_destination_display is enabled
    And Navigate has an active in-app path
    And the route has an upcoming stop or end address
    Then the next destination address banner shows that address

  Scenario: Upcoming stop address preferred over turn-only chrome
    Given ff_next_destination_display is enabled
    And Navigate is active on a route with ordered stops
    Then the banner shows the upcoming stop addressLabel
    And turn guidance does not replace the destination address
    And the address remains visible for the whole Navigate session

  Scenario: End address when there are no intermediate stops
    Given ff_next_destination_display is enabled
    And Navigate is active on a start/end route with no stops
    Then the banner shows the route end addressLabel

  Scenario: Hidden when not navigating
    Given ff_next_destination_display is enabled
    And there is no active Navigate path
    Then the next destination address banner is not shown

  Scenario: Phone stays compact; Android Auto must be clear
    Given ff_next_destination_display is enabled
    Then the phone map-home chip is compact and single-line
    And the phone surface is optional via the feature flag
    And Android Auto (ATNA-10) must show the next destination clearly when Auto nav lands
    And no LineManager or MapRoutePathAnnotations changes are required
