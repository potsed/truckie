Feature: Self-hosted Valhalla on OCI Free Tier
  As a select-user dogfood operator
  I want a reusable OCI Free Tier Valhalla host with QLD/SEQ truck tiles
  So that Truckie live PathRouter can call HTTPS /route without hosted interim or GraphHopper

  # ATNA-9. Infra under infra/oci-valhalla/. Client remains gated by
  # ff_valhalla_live + VALHALLA_BASE_URL (docs/valhalla-live.md,
  # docs/oci-valhalla-runbook.md). GraphHopper OCI scaffold stays deleted.

  @ff_valhalla_live
  Scenario: Operator can redeploy Valhalla behind HTTPS without the Android app
    Given OCI Free Tier credentials and DNS are supplied by the operator (not in git)
    And only the infra/oci-valhalla stack is used (no Gradle, no app modules on the VM)
    When Terraform bootstraps the host and syncs compose/scripts via SSH
    Then Valhalla serves /route only via HTTPS on the configured domain
    And SSH password authentication is disabled
    And the Valhalla HTTP port is not published on the public interface
    And re-running install.sh or sync-to-host.sh is idempotent

  @ff_valhalla_live
  Scenario: QLD-first SEQ tiles are the Free Tier dogfood slice
    Given the SEQ OSM extract has been imported into Valhalla tiles
    When a client POSTs /route with costing truck between Brisbane sample points
    Then Valhalla returns a trip response over HTTPS
    And OciValhallaHostCatalog documents SEQ bbox and Free Tier memory limits
    And full AU tiles are a documented next step (not the Free Tier default)

  @ff_valhalla_live
  Scenario: App points at self-host via VALHALLA_BASE_URL without secrets in git
    Given Valhalla is healthy at https://vh.example.com
    When the operator sets truckie.valhalla.baseUrl or VALHALLA_BASE_URL
    And clears VALHALLA_API_KEY
    And enables ff_valhalla_live
    Then ValhallaLiveCatalog.effectiveBaseUrl is the self-host URL
    And LiveValhallaPathRouter calls {base}/route without Authorization
    And no API key or OCI credential is committed

  @ff_valhalla_live
  Scenario: Stack sync never clones the Truckie Android monorepo
    Given cloud-init has finished host bootstrap
    When the stack is installed via terraform provisioner or sync-to-host.sh
    Then /opt/oci-valhalla contains compose and scripts only
    And the VM has no requirement on :app or core-* Android modules
    And GraphHopper paths are absent from the Valhalla stack
