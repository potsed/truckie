Feature: Self-hosted GraphHopper on OCI Free Tier
  As a select-user dogfood operator
  I want a reusable OCI Free Tier GraphHopper host with AU/SEQ truck data
  So that Truckie live PathRouter can call HTTPS /route without GraphHopper Cloud or Firebase

  # ATNA-105. Infra under infra/oci-graphhopper/. Client remains gated by
  # ff_graphhopper_live + GRAPHHOPPER_BASE_URL (docs/graphhopper-live.md).

  @ff_graphhopper_live
  Scenario: Operator can redeploy GraphHopper behind HTTPS
    Given feature flag "ff_graphhopper_live" is enabled
    And OCI Free Tier credentials and DNS are supplied by the operator (not in git)
    When Terraform and cloud-init bootstrap the GraphHopper host with Caddy TLS
    Then GraphHopper serves /route only via HTTPS on the configured domain
    And SSH password authentication is disabled
    And the GraphHopper HTTP port is not published on the public interface

  @ff_graphhopper_live
  Scenario: AU SEQ graph and Custom Model are wired for the app
    Given feature flag "ff_graphhopper_live" is enabled
    And the SEQ OSM extract has been imported with the truck profile
    When the Android app is built with GRAPHHOPPER_BASE_URL pointing at the host
    Then LiveGraphHopperPathRouter can POST profile truck with a Custom Model snippet
    And BuildConfig.GRAPHHOPPER_BASE_URL is non-blank for that assemble

  @ff_graphhopper_live
  Scenario: OSM refresh rebuilds the graph without committing secrets
    Given an existing OCI GraphHopper deployment
    When the operator runs the OSM refresh script
    Then the SEQ PBF and graph-cache are rebuilt
    And terraform.tfvars and compose .env remain outside version control
