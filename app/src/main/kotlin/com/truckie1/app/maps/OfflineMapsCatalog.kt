package com.truckie1.app.maps

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.maps.MapEngine

/**
 * Offline MapLibre pack manager (`ff_offline_maps`, ATNA-95 contracts).
 *
 * Defaults **off** in production and debug until MapLibre OfflineManager +
 * pack download UX + an offline-capable style ship. Does not enable product
 * surfaces by itself — [shouldExposePackManager] also requires the engine to
 * report packs available.
 *
 * Gherkin: features/offline_maps.feature
 */
object OfflineMapsCatalog {
    const val FLAG_KEY: String = FlagKeys.OFFLINE_MAPS

    const val DEFAULT_ENABLED: Boolean = false
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = false

    const val targetsMapLibreOfflineManager: Boolean = true
    const val requiresPackDownloadUxBeforeProduction: Boolean = true
    const val requiresOfflineCapableStyleBeforeProduction: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.OFFLINE_MAPS)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.OFFLINE_MAPS, enabled)
    }

    /**
     * Product surface for offline packs: flag on **and** engine OfflineManager ready.
     * Native [com.truckie1.maps.MapLibreNativeEngine] stays unavailable until
     * OfflineManager lands; [com.truckie1.maps.MapLibreStubEngine] is for JVM tests.
     */
    fun shouldExposePackManager(flags: FeatureFlags, engine: MapEngine): Boolean =
        isEnabled(flags) && engine.isOfflinePackManagerAvailable()

    fun listedPackIds(flags: FeatureFlags, engine: MapEngine): List<String> =
        if (shouldExposePackManager(flags, engine)) {
            engine.listOfflinePackIds()
        } else {
            emptyList()
        }
}
