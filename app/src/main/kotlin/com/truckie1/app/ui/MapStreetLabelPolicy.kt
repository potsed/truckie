package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.maps.MapCameraDefaults
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory

/**
 * Prefer road / street name labels over POI clutter on OpenFreeMap Liberty
 * (`ff_map_street_labels`, ATNA-78).
 *
 * Keeps [MapCameraDefaults.STYLE_URI] (OSS, free for commercial). Applies
 * runtime layer visibility + minzoom tweaks after style load — no custom
 * hosted style override and no paid tile vendor.
 */
enum class LayerVisibilityIntent {
    VISIBLE,
    NONE,
}

/**
 * One declarative tweak against a MapLibre style layer id.
 */
data class StyleLayerAdjustment(
    val layerId: String,
    val visibility: LayerVisibilityIntent? = null,
    val minZoom: Double? = null,
)

/**
 * Test seam for MapLibre [Style] mutations (unit tests record; production wraps Style).
 */
interface StyleLayerMutator {
    fun hasLayer(layerId: String): Boolean
    fun setVisibility(layerId: String, visible: Boolean)
    fun setMinZoom(layerId: String, minZoom: Double)
}

/**
 * MapLibre [Style] adapter for [StyleLayerMutator].
 */
class MapLibreStyleLayerMutator(
    private val style: Style,
) : StyleLayerMutator {
    override fun hasLayer(layerId: String): Boolean = style.getLayer(layerId) != null

    override fun setVisibility(layerId: String, visible: Boolean) {
        val layer = style.getLayer(layerId) ?: return
        layer.setProperties(
            PropertyFactory.visibility(if (visible) Property.VISIBLE else Property.NONE),
        )
    }

    override fun setMinZoom(layerId: String, minZoom: Double) {
        val layer = style.getLayer(layerId) ?: return
        layer.setMinZoom(minZoom.toFloat())
    }
}

object MapStreetLabelPolicy {
    const val FLAG_KEY: String = FlagKeys.MAP_STREET_LABELS

    /** Same OpenFreeMap Liberty URI as [MapCameraDefaults] — not a rehosted style. */
    const val keepsOpenFreeMapLibertyStyleUri: Boolean = true

    const val usesCustomHostedStyleOverride: Boolean = false

    const val usesRuntimeLayerTweaks: Boolean = true

    const val rehostsBasemapTiles: Boolean = false

    val styleUri: String = MapCameraDefaults.STYLE_URI

    const val HIGHWAY_NAME_MAJOR: String = "highway-name-major"
    const val HIGHWAY_NAME_MINOR: String = "highway-name-minor"

    /**
     * Liberty defaults minor highway names at minzoom 15; lower so street names
     * appear reliably within Truckie's street-neighbourhood driving zoom (~15.5).
     */
    const val HIGHWAY_MINOR_MIN_ZOOM: Double = 14.0

    /**
     * OpenFreeMap Liberty POI / transit label layers that crowd street names
     * (bus stops via `poi_transit`, plus ranked amenity labels).
     */
    val HIDDEN_POI_LAYERS: List<String> = listOf(
        "poi_transit",
        "poi_r1",
        "poi_r7",
        "poi_r20",
    )

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.MAP_STREET_LABELS)

    fun shouldApply(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled && isEnabled(flags)

    /**
     * Declarative tweaks applied once the Liberty style is fully loaded.
     * Missing layer ids are skipped at apply time (style may evolve).
     */
    fun adjustments(): List<StyleLayerAdjustment> = buildList {
        add(
            StyleLayerAdjustment(
                layerId = HIGHWAY_NAME_MAJOR,
                visibility = LayerVisibilityIntent.VISIBLE,
            ),
        )
        add(
            StyleLayerAdjustment(
                layerId = HIGHWAY_NAME_MINOR,
                visibility = LayerVisibilityIntent.VISIBLE,
                minZoom = HIGHWAY_MINOR_MIN_ZOOM,
            ),
        )
        HIDDEN_POI_LAYERS.forEach { id ->
            add(
                StyleLayerAdjustment(
                    layerId = id,
                    visibility = LayerVisibilityIntent.NONE,
                ),
            )
        }
    }

    fun applyToStyle(mutator: StyleLayerMutator, enabled: Boolean) {
        if (!enabled) return
        adjustments().forEach { tweak ->
            if (!mutator.hasLayer(tweak.layerId)) return@forEach
            when (tweak.visibility) {
                LayerVisibilityIntent.VISIBLE -> mutator.setVisibility(tweak.layerId, true)
                LayerVisibilityIntent.NONE -> mutator.setVisibility(tweak.layerId, false)
                null -> Unit
            }
            tweak.minZoom?.let { mutator.setMinZoom(tweak.layerId, it) }
        }
    }

    fun applyToStyle(style: Style, enabled: Boolean) {
        applyToStyle(MapLibreStyleLayerMutator(style), enabled)
    }
}
