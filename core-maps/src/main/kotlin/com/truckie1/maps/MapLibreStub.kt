package com.truckie1.maps

/**
 * MapLibre Native Android will render vector tiles / offline packs.
 * Phase 0 exposes a stub so :app can assemble without the native SDK
 * (MapLibre's current minSdk may still exceed Truckie's phone minSdk 24).
 */
interface MapEngine {
    val engineName: String
    fun isOfflinePackManagerAvailable(): Boolean
    fun listOfflinePackIds(): List<String>
}

class MapLibreStubEngine : MapEngine {
    override val engineName: String = "MapLibre-stub"
    private val packs = mutableListOf<String>()

    override fun isOfflinePackManagerAvailable(): Boolean = true

    override fun listOfflinePackIds(): List<String> = packs.toList()

    fun registerPackForTests(id: String) {
        packs += id
    }
}
