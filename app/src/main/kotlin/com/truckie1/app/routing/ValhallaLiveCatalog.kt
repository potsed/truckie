package com.truckie1.app.routing

import com.truckie1.app.BuildConfig
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Live Valhalla PathRouter for select-user / dogfood (ATNA-113).
 *
 * **Interim:** hosted Valhalla (Simple Routing default base) via API key.
 * **Long-term / offline:** self-hosted or on-device Valhalla tiles — set
 * [configuredBaseUrl], clear API key. Same [LiveValhallaPathRouter] client.
 *
 * Stub PathRouter is tests / degraded only when credentials are missing.
 *
 * Gherkin: features/route_follow_roads.feature
 * Docs: docs/valhalla-live.md
 */
object ValhallaLiveCatalog {
    const val FLAG_KEY: String = FlagKeys.VALHALLA_LIVE

    /** Select-user / debug dogfood prefers live when credentials are configured. */
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    /** Production default stays off until credentials + AU tiles are confirmed. */
    const val DEFAULT_ENABLED: Boolean = false

    const val prefersLivePathRouterForDetours: Boolean = true
    const val stubIsDegradedFallbackOnly: Boolean = true

    /** Simple Routing hosted Valhalla — OSS-compatible /route + Bearer key. */
    const val SIMPLE_ROUTING_DEFAULT_BASE_URL: String =
        "https://api.simplerouting.io/valhalla"

    const val SETTINGS_ROUTING_STATUS_TAG: String = "settings_routing_geometry_status"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.VALHALLA_LIVE)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.VALHALLA_LIVE, enabled)
    }

    fun configuredBaseUrl(): String =
        BuildConfig.VALHALLA_BASE_URL.trim()

    fun configuredApiKey(): String =
        BuildConfig.VALHALLA_API_KEY.trim()

    fun hasConfiguredApiKey(): Boolean = configuredApiKey().isNotEmpty()

    /**
     * Effective Valhalla base (no trailing slash): explicit BuildConfig URL, else
     * Simple Routing default when an API key is present. Empty → stub.
     */
    fun effectiveBaseUrl(): String {
        val configured = configuredBaseUrl()
        if (configured.isNotEmpty()) return configured.trimEnd('/')
        if (hasConfiguredApiKey()) return SIMPLE_ROUTING_DEFAULT_BASE_URL
        return ""
    }

    fun hasLiveCredentials(): Boolean = effectiveBaseUrl().isNotEmpty()

    fun isHostedInterim(): Boolean {
        val url = effectiveBaseUrl()
        return url.isNotEmpty() && url.contains("simplerouting.io")
    }

    fun isLivePathRouterActive(flags: FeatureFlags): Boolean {
        if (!isEnabled(flags)) return false
        val url = effectiveBaseUrl()
        if (url.isEmpty()) return false
        // Hosted Simple Routing requires a Bearer key; self-hosted may omit it.
        if (isHostedInterim() && !hasConfiguredApiKey()) return false
        return true
    }

    fun missingForLiveRouting(flags: FeatureFlags): String {
        val missing = mutableListOf<String>()
        if (!isEnabled(flags)) {
            missing += "ff_valhalla_live is off"
        }
        if (!flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO)) {
            missing += "ff_async_routing_io is off (live HTTP requires async I/O)"
        }
        val url = effectiveBaseUrl()
        if (url.isEmpty()) {
            missing += "VALHALLA_API_KEY (hosted interim) or VALHALLA_BASE_URL " +
                "(self-hosted / offline tiles) is unset"
        } else if (url.contains("simplerouting.io") && !hasConfiguredApiKey()) {
            missing += "VALHALLA_API_KEY is unset (required for Simple Routing)"
        }
        return missing.joinToString("; ")
    }

    fun followsRoadNetwork(flags: FeatureFlags): Boolean =
        isLivePathRouterActive(flags) && flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO)

    fun isDegradedStubActive(flags: FeatureFlags): Boolean = !followsRoadNetwork(flags)

    fun degradedStubNotice(flags: FeatureFlags): String {
        if (followsRoadNetwork(flags)) return ""
        val gaps = missingForLiveRouting(flags).ifBlank { "live Valhalla is unavailable" }
        return "Degraded stub PathRouter (straight geometry, not road network). " +
            "Set VALHALLA_API_KEY for hosted Valhalla interim or VALHALLA_BASE_URL " +
            "for self-hosted/offline tiles. Missing: $gaps"
    }

    fun routingGeometryStatus(flags: FeatureFlags): String {
        if (followsRoadNetwork(flags)) {
            val host = if (isHostedInterim()) {
                "Valhalla hosted interim"
            } else {
                "Valhalla self-hosted"
            }
            return "Routing: $host (road network) · ${effectiveBaseUrl()}"
        }
        return "Routing: degraded stub (straight / not road network). " +
            degradedStubNotice(flags)
    }

    fun selfHostedSwapSteps(): List<String> = listOf(
        "Build or obtain AU Valhalla tiles (self-host on OCI Free Tier or on-device)",
        "Set truckie.valhalla.baseUrl / TRUCKIE_VALHALLA_BASE_URL / VALHALLA_BASE_URL " +
            "to https://<host> (path prefix that serves /route)",
        "Clear VALHALLA_API_KEY / TRUCKIE_VALHALLA_API_KEY (Bearer unused for self-host)",
        "Rebuild with ff_valhalla_live on — same LiveValhallaPathRouter client",
    )
}
