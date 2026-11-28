Feature: NHVR and state restriction overlays gated by ff_nhvr_overlay
  As a single-owner-operator driver on restricted heavy-vehicle networks
  I want NHVR / state restriction overlays behind a feature flag
  So that network and authority layers can land without enabling unfinished ingest

  # Product toggle: ff_nhvr_overlay (ATNA-11 / ATNA-43)
  # Defaults OFF in production and debug — do not enable until overlay pipeline ships.
  # Authoritative reference: NHVR National Network Map (maps.nhvr.gov.au).
  # Truckie still routes in-app (Valhalla PathRouter); overlay is guidance, not a permit.
  # Physical roadside signage and official permits override digital overlays.
  # Distinct from QLD bridge packs / AU low-bridges OSS pack (ff_bridge_markers etc.).
  # Out of scope: enabling the flag, inventing restriction geometries, GraphHopper.

  Scenario: NHVR overlay flag defaults off
    Given a fresh LocalFeatureFlags instance
    Then ff_nhvr_overlay is off
    And NHVR overlay stays disabled even in debug dogfood defaults

  Scenario: Overlay is not drawn when the flag is off
    Given the feature flag ff_nhvr_overlay is off
    And map home is active
    Then no NHVR or state restriction overlay is drawn
    And bridge pack markers remain a separate concern

  Scenario: Enabling the flag exposes the overlay contract only
    Given the feature flag ff_nhvr_overlay is off
    When ff_nhvr_overlay is enabled
    Then NHVR overlay is enabled for the product gate
    And the catalog targets the NHVR National Network Map as authority
    And Truckie routing remains in-app PathRouter
    And overlay data must cite source name URL and retrieval date
    And missing geometries stay absent — never invent restriction polygons
    And roadside signs and permits override digital overlays

  Scenario: Overlay does not replace QLD bridge packs
    Given ff_nhvr_overlay is enabled
    Then NHVR overlay is not a substitute for ff_bridge_markers packs
    And other-state / national ingest remains tracked under ATNA-43
