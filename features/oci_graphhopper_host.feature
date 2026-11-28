Feature: Self-hosted GraphHopper on OCI Free Tier
  As a select-user dogfood operator
  I want a reusable OCI Free Tier GraphHopper host with AU/SEQ truck data
  So that Truckie live PathRouter can call HTTPS /route without GraphHopper Cloud or Firebase

  # ATNA-105. Infra under infra/oci-graphhopper/. Client remains gated by
  # ff_graphhopper_live + GRAPHHOPPER_BASE_URL (docs/graphhopper-live.md).

  @ff_graphhopper_live
  Scenario: Operator can redeploy GraphHopper behind HTTPS without the Android app
    Given OCI Free Tier credentials and DNS are supplied by the operator (not in git)
    And only the infra/oci-graphhopper stack is used (no Gradle, no app modules on the VM)
    When Terraform bootstraps the host and syncs compose/scripts via SSH
    Then GraphHopper serves /route only via HTTPS on the configured domain
    And SSH password authentication is disabled
    And the GraphHopper HTTP port is not published on the public interface
    And re-running install.sh or sync-to-host.sh is idempotent

  @ff_graphhopper_live
  Scenario: AU SEQ graph and Custom Model are available on the host
    Given the SEQ OSM extract has been imported with the truck profile
    When a client POSTs /route with profile truck and a Custom Model snippet
    Then GraphHopper returns a path response over HTTPS
    And the Android app may later set GRAPHHOPPER_BASE_URL to that host (optional)

  @ff_graphhopper_live
  Scenario: OSM refresh rebuilds the graph without committing secrets
    Given an existing OCI GraphHopper deployment
    When the operator runs the OSM refresh script
    Then the SEQ PBF and graph-cache are rebuilt
    And terraform.tfvars and compose .env remain outside version control

  @ff_graphhopper_live
  Scenario: Stack sync never clones the Truckie Android monorepo
    Given cloud-init has finished host bootstrap
    When the stack is installed via terraform provisioner or sync-to-host.sh
    Then /opt/oci-graphhopper contains compose and scripts only
    And the VM has no requirement on :app or core-* Android modules
