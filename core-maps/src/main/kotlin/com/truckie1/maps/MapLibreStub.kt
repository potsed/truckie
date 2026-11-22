package com.truckie1.maps

/**
 * Map rendering facade. Phase A ships the real MapLibre Native Android SDK
 * in `:app` ([MapLibreNativeEngine]); this module keeps camera defaults and
 * the offline-pack contract for later packs.
 */
interface MapEngine {
    val engineName: String
    fun isOfflinePackManagerAvailable(): Boolean
    fun listOfflinePackIds(): List<String>
}

/**
 * Test / offline-pack stub. Prefer [MapLibreNativeEngine] for the map-first home.
 */
class MapLibreStubEngine : MapEngine {
    override val engineName: String = "MapLibre-stub"
    private val packs = mutableListOf<String>()

    override fun isOfflinePackManagerAvailable(): Boolean = true

    override fun listOfflinePackIds(): List<String> = packs.toList()

    fun registerPackForTests(id: String) {
        packs += id
    }
}

/**
 * Identity for the real MapLibre Native Android SDK wired in the app module.
 * Offline pack manager hooks land with [com.truckie1.flags.FlagKeys.OFFLINE_MAPS].
 */
class MapLibreNativeEngine : MapEngine {
    override val engineName: String = "MapLibre"

    override fun isOfflinePackManagerAvailable(): Boolean = false

    override fun listOfflinePackIds(): List<String> = emptyList()
}
