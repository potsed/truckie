package com.truckie1.flags

/**
 * Feature-flag facade. Every user-facing capability must be gated.
 */
interface FeatureFlags {
    fun isEnabled(key: String): Boolean
}

object FlagKeys {
    const val VEHICLE_PROFILES = "ff_vehicle_profiles"
    const val MULTI_STOP = "ff_multi_stop"
    const val STOP_CARGO = "ff_stop_cargo"
    const val BRIDGE_SAFE_ROUTING = "ff_bridge_safe_routing"
    /** Fastest / Cheapest among height- and weight-safe tours. */
    const val ROUTE_OPTIMIZE = "ff_route_optimize"

    @Deprecated("Use ROUTE_OPTIMIZE (ff_route_optimize)", ReplaceWith("ROUTE_OPTIMIZE"))
    const val OPTIMIZE_FOR_VEHICLE = ROUTE_OPTIMIZE
    const val TOLLS = "ff_tolls"
    const val OFFLINE_MAPS = "ff_offline_maps"
    const val ANDROID_AUTO = "ff_android_auto"
    const val NHVR_OVERLAY = "ff_nhvr_overlay"
    /** Material3 TopAppBar + bottom NavigationBar across primary screens. */
    const val NAV_CHROME = "ff_nav_chrome"
}

/**
 * Local in-memory flags. Production defaults are off.
 */
class LocalFeatureFlags(
    initial: Map<String, Boolean> = emptyMap(),
) : FeatureFlags {
    private val overrides = initial.toMutableMap()

    override fun isEnabled(key: String): Boolean = overrides[key] == true

    fun setEnabled(key: String, enabled: Boolean) {
        overrides[key] = enabled
    }
}
