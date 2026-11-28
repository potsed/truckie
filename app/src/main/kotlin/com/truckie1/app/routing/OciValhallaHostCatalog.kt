package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys

/**
 * Config catalog for self-hosted Valhalla on OCI Free Tier (ATNA-9).
 *
 * Deploy is app-independent under [STACK_PATH]; the Android client only
 * needs [FlagKeys.VALHALLA_LIVE] + [ValhallaLiveCatalog] base URL wiring.
 *
 * QLD-first / SEQ dogfood tiles fit Always Free Ampere; full AU is a
 * documented next step (same spirit as G-NAF QLD pack slice).
 *
 * Gherkin: features/oci_valhalla_host.feature
 * Docs: docs/oci-valhalla-runbook.md, infra/oci-valhalla/README.md
 */
object OciValhallaHostCatalog {
    const val FLAG_KEY: String = FlagKeys.VALHALLA_LIVE

    const val STACK_PATH: String = "infra/oci-valhalla"
    const val INSTALL_ROOT: String = "/opt/oci-valhalla"
    const val COMPOSE_PROJECT: String = "oci-valhalla"
    const val VALHALLA_LOOPBACK_PORT: Int = 8002

    /** Official scripted image pin — multi-arch (amd64/arm64 for Ampere). */
    const val VALHALLA_IMAGE: String =
        "ghcr.io/valhalla/valhalla-scripted:3.9.3"

    /**
     * South East Queensland dogfood bbox (lon_min,lat_min,lon_max,lat_max).
     * Gold Coast → Sunshine Coast coastal corridor — Brisbane legs.
     */
    const val SEQ_BBOX: String = "152.0,-28.55,153.65,-26.35"

    const val GEOFABRIK_AU_PBF_URL: String =
        "https://download.geofabrik.de/australia-oceania/australia-latest.osm.pbf"

    /** Always Free Ampere defaults used by terraform.tfvars.example. */
    const val FREE_TIER_SHAPE: String = "VM.Standard.A1.Flex"
    const val FREE_TIER_OCPUS: Int = 2
    const val FREE_TIER_MEMORY_GBS: Int = 12
    const val FREE_TIER_BOOT_GBS: Int = 100

    /**
     * Full Australia PBF + tiles typically exceed comfortable Always Free
     * memory during build; SEQ/QLD-first is the shipped dogfood path.
     */
    const val FULL_AU_FITS_FREE_TIER: Boolean = false
    const val QLD_FIRST_DEFAULT: Boolean = true

    /** No Bearer key for self-host — clear interim Simple Routing key. */
    const val SELF_HOST_REQUIRES_API_KEY: Boolean = false

    /** GraphHopper OCI scaffold must stay deleted (ATNA-114). */
    const val GRAPHHOPPER_STACK_REVIVED: Boolean = false

    const val SAMPLE_BRISBANE_FROM_LAT: Double = -27.4698
    const val SAMPLE_BRISBANE_FROM_LON: Double = 153.0251
    const val SAMPLE_BRISBANE_TO_LAT: Double = -27.4550
    const val SAMPLE_BRISBANE_TO_LON: Double = 153.0350

    fun baseUrlHint(domain: String): String {
        val host = domain.trim().trimEnd('/')
        require(host.isNotEmpty()) { "domain required" }
        val bare = host.removePrefix("https://").removePrefix("http://")
        return "https://$bare"
    }

    fun buildConfigWiringSteps(): List<String> = listOf(
        "Deploy ${STACK_PATH} (terraform apply + import-qld-seq.sh)",
        "Point DNS A → terraform output public_ip; wait for Caddy TLS",
        "Set truckie.valhalla.baseUrl / TRUCKIE_VALHALLA_BASE_URL / " +
            "VALHALLA_BASE_URL (secrets.properties or local.properties) " +
            "to https://<domain>",
        "Clear VALHALLA_API_KEY / TRUCKIE_VALHALLA_API_KEY",
        "Rebuild with ff_valhalla_live on — LiveValhallaPathRouter uses /route",
    )

    fun freeTierLimitsNotes(): List<String> = listOf(
        "SEQ clip (${SEQ_BBOX}) is the Free Tier dogfood default",
        "Always Free Ampere: ≤4 OCPU / ≤24 GB RAM shared; example " +
            "${FREE_TIER_OCPUS} OCPU / ${FREE_TIER_MEMORY_GBS} GB",
        "Full AU tile build usually needs more RAM/disk than Free Tier — " +
            "widen bbox or move to larger shape as a next step",
        "Valhalla listens on 127.0.0.1:${VALHALLA_LOOPBACK_PORT}; Caddy " +
            "exposes HTTPS /route and /status only",
    )

    fun brisbaneSampleCurl(baseUrl: String): String {
        val base = baseUrl.trimEnd('/')
        return """
            curl -fsS -X POST '$base/route' \
              -H 'Content-Type: application/json' \
              -d '{
                "locations": [
                  {"lat": $SAMPLE_BRISBANE_FROM_LAT, "lon": $SAMPLE_BRISBANE_FROM_LON},
                  {"lat": $SAMPLE_BRISBANE_TO_LAT, "lon": $SAMPLE_BRISBANE_TO_LON}
                ],
                "costing": "truck",
                "directions_options": {"units": "kilometers"},
                "costing_options": {
                  "truck": {"height": 3.5, "width": 2.5, "length": 12.0, "weight": 12.0}
                }
              }'
        """.trimIndent()
    }
}
