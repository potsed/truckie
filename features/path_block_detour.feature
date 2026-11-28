Feature: Reroute around path blocks via live Valhalla PathRouter
  As a single-owner-operator driver
  I want Truckie to compute an alternate around a blocked path using PathRouter
  So that the map polyline and Navigate never go blank when a low bridge,
  road closure, or weight ban obstructs the first attempt

  # Product toggle: ff_path_block_detour (defaults on).
  # Preference (2026-11-28): when a path is blocked, compute an alternate around
  # the block — never drop the polyline. Detours may use residential or the
  # simplest path around the obstruction.
  # Production design (Daniel / ATNA-114): live Valhalla PathRouter (truck
  # costing / exclude polygons) with ff_valhalla_live + credentials — not a
  # hand-rolled MapLibre polyline rule. Stub PathRouter is tests / degraded
  # only when credentials are unset (see docs/valhalla-live.md).
  # Shared by MultiStopTourComposer → ActiveTourPath → map polyline / Navigate.
  # Cargo GVM / missing coords remain non-geographic blockers.

  Scenario: Path-block detour is feature-flagged and defaults on
    Given a fresh app start
    Then ff_path_block_detour is on
    And PathBlockDetour uses PathRouter (not a hand-rolled polyline rule)
    And PathBlockDetour prefers live Valhalla for production alternates

  Scenario: Select-user live Valhalla wiring
    Given debug dogfood enables ff_valhalla_live
    And VALHALLA_API_KEY or VALHALLA_BASE_URL may be configured at build time
    When credentials resolve and ff_async_routing_io is on
    Then ValhallaPathRouterFactory selects LiveValhallaPathRouter
    When credentials are unset
    Then the factory falls back to stub and ValhallaLiveCatalog.missingForLiveRouting documents the gap

  Scenario: Mid-path under-bridge yields a PathRouter alternate, not a blank line
    Given ff_path_block_detour and ff_bridge_passage are enabled
    And a PathRouter leg would conflict with an under-bridge mid-path
    When MultiStopTourComposer / PathBlockDetour resolve the leg
    Then PathRouter is retried with avoidUnderBridges / truck model
    And the result is Ready with at least two points for LineManager
    And the tour line is never erased solely because of the geographic block

  Scenario: Demo Airport run still draws when a stop is near Colina St
    Given ff_demo_mode, ff_map_route_polyline, and ff_path_block_detour are enabled
    And the Wynnum stop lies near the Colina St underbridge
    When the home polyline resolves
    Then PathRouter geometry is Ready (stop proximity and/or PathRouter detour)
    And LineManager is not left empty

  Scenario: Non-geographic blockers still report Blocked
    Given ff_path_block_detour is enabled
    When on-board mass exceeds GVM or stops lack coordinates
    Then OptimizeResult remains Blocked (no inventing a drawable tour)
