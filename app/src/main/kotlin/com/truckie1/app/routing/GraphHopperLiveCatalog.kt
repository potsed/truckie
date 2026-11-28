package com.truckie1.app.routing

import com.truckie1.app.BuildConfig
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Live GraphHopper PathRouter for select-user / dogfood (ATNA-9 / ATNA-102).
 *
 * Intended production path for constrained legs + alternates around blocks:
 * `ff_graphhopper_live` + non-blank base URL + AU truck graph data.
 * Stub PathRouter is tests / degraded only when URL is unset.
 *
 * Gherkin: features/path_block_detour.feature,
 * features/route_optimisation_graphhopper.feature
 * Docs: docs/graphhopper-live.md
 */
object GraphHopperLiveCatalog {
    const val FLAG_KEY: String = FlagKeys.GRAPHHOPPER_LIVE

    /** Select-user / debug dogfood prefers live when a base URL is configured. */
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    /** Production default stays off until URL + AU data are confirmed. */
    const val DEFAULT_ENABLED: Boolean = false

    const val prefersLivePathRouterForDetours: Boolean = true
    const val stubIsDegradedFallbackOnly: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.GRAPHHOPPER_LIVE)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.GRAPHHOPPER_LIVE, enabled)
    }

    /** BuildConfig / gradle property / env — empty means live cannot start. */
    fun configuredBaseUrl(): String =
        BuildConfig.GRAPHHOPPER_BASE_URL.trim()

    fun hasConfiguredBaseUrl(): Boolean = configuredBaseUrl().isNotEmpty()

    /**
     * Live PathRouter is usable when the flag is on **and** a base URL is set
     * (factory still requires both; async I/O gate is separate).
     */
    fun isLivePathRouterActive(flags: FeatureFlags): Boolean =
        isEnabled(flags) && hasConfiguredBaseUrl()

    /**
     * Human-readable gap when select-user expects live but cannot use it.
     * Empty string when live is active.
     */
    fun missingForLiveRouting(flags: FeatureFlags): String {
        val missing = mutableListOf<String>()
        if (!isEnabled(flags)) {
            missing += "ff_graphhopper_live is off"
        }
        if (!hasConfiguredBaseUrl()) {
            missing += "GRAPHHOPPER_BASE_URL / truckie.graphhopper.baseUrl / " +
                "TRUCKIE_GRAPHHOPPER_BASE_URL is unset"
        }
        if (!flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO)) {
            missing += "ff_async_routing_io is off (live HTTP requires async I/O)"
        }
        return missing.joinToString("; ")
    }
}
