package com.truckie1.app.routing

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Async GraphHopper + Nominatim I/O (`ff_async_routing_io`, ATNA-88 / P1-3).
 *
 * Live PathRouter and Nominatim are **suspend** and hop to an IO dispatcher.
 * Compose never calls live HTTP from `remember` or a blocking main-thread click.
 * Stub PathRouter may stay instantaneous. Sync-on-main live path is deleted.
 *
 * Defaults **on**. Does **not** enable [FlagKeys.GRAPHHOPPER_LIVE].
 *
 * Gherkin: features/async_routing_io.feature
 */
object AsyncRoutingIoCatalog {
    const val FLAG_KEY: String = FlagKeys.ASYNC_ROUTING_IO

    /** Ship / dogfood default after ATNA-88 — always prefer off-main I/O. */
    const val DEFAULT_ENABLED: Boolean = true

    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true

    /** Live GraphHopper stays off until explicitly enabled + base URL. */
    const val enablesGraphHopperLiveByDefault: Boolean = false

    const val livePathRouterIsSuspend: Boolean = true
    const val nominatimSuggestIsSuspend: Boolean = true
    const val neverBlocksComposeRemember: Boolean = true
    const val neverBlocksMainThreadClickForLiveHttp: Boolean = true
    const val stubPathRouterMayStayInstantaneous: Boolean = true
    const val reusesMapRoutePolylineSessionCache: Boolean = true

    /** Debounce before Nominatim network for typed queries (ms). */
    const val SUGGEST_DEBOUNCE_MS: Long = 250L

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, enabled)
    }

    /**
     * Live HTTP GraphHopper is allowed only when async I/O is on **and**
     * [FlagKeys.GRAPHHOPPER_LIVE] is on (base URL still required by factory).
     */
    fun allowsLiveGraphHopper(flags: FeatureFlags): Boolean =
        isEnabled(flags) && flags.isEnabled(FlagKeys.GRAPHHOPPER_LIVE)

    /**
     * Live Nominatim is allowed only when async I/O is on **and**
     * address autocomplete is on — otherwise NoOp (never sync HTTP).
     */
    fun allowsLiveNominatim(flags: FeatureFlags): Boolean =
        isEnabled(flags) && flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE)
}
