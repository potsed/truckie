package com.truckie1.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.maplibre.android.maps.Style

/**
 * Route stop markers for MapLibre map home (`ff_stop_markers`).
 *
 * Pickup/load vs drop-off/unload use distinct **numbered style-image sprites**
 * registered via [Style.addImage] (MapLibre native sprite atlas) and drawn with
 * one GeoJSONSource + symbol layer ([MapStyleOverlayBind]). Order index is baked
 * into the sprite — the stop SymbolLayer does **not** set textField (avoids the
 * doubled/scribbled look from overlapping text + chevron).
 *
 * Out of scope: satellite peek. Gherkin: features/map_stop_markers.feature,
 * features/map_style_overlays.feature
 */
object MapStopMarkersCatalog {
    const val FLAG_KEY: String = FlagKeys.STOP_MARKERS

    const val ICON_PICKUP: String = "truckie_stop_pickup"
    const val ICON_DROPOFF: String = "truckie_stop_dropoff"

    /** @deprecated Prefer numbered [iconIdFor]; kept for legacy tests. */
    const val ICON_ID: String = ICON_PICKUP

    /** Catalog: MapLibre style image sprites + one symbol layer. */
    const val usesStyleSymbolLayer: Boolean = true
    /** Numbered pins via style.addImage (runtime sprite atlas) — not textField. */
    const val usesNumberedSpriteIcons: Boolean = true
    /** Stop layer is icon-only; index is in the sprite (ATNA-121). */
    const val stopSymbolLayerUsesTextField: Boolean = false
    const val usesSymbolManager: Boolean = false
    const val usesDeprecatedMarkerApi: Boolean = false

    /** One GeoJSON / style-layer rebind path with bridge markers — not a parallel stack. */
    const val usesSharedStyleOverlayBindPath: Boolean = true
    /** @deprecated Use [usesSharedStyleOverlayBindPath]. */
    const val usesSharedSymbolBindPath: Boolean = true
    const val sharesStyleLifecycleWithBridges: Boolean = true

    /** This feature does not implement satellite peek (separate preference). */
    const val implementsSatellitePeek: Boolean = false

    /**
     * Bridge clearance labels still need OpenFreeMap Noto; stop pins bake the
     * index into sprites so the stop layer does not depend on glyph stacks.
     */
    const val requiresOpenFreeMapCompatibleTextFont: Boolean = false

    val symbolTextFont: Array<String> = MapLibreSymbolText.openFreeMapTextFont

    /** Pickup/load — teal fill. */
    const val PICKUP_COLOR_HEX: String = "#0B6E4F"

    /** Drop-off/unload — deep blue (distinct from pickup, not W5-8 amber). */
    const val DROPOFF_COLOR_HEX: String = "#1A4B7A"

    const val DEFAULT_ICON_PX: Int = 64

    /** Shared home-map scale — see [MapStyleOverlayCatalog.IconSize.STOP]. */
    val ICON_SIZE: Float = MapStyleOverlayCatalog.IconSize.STOP

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.STOP_MARKERS)

    fun shouldShow(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled && isEnabled(flags)

    /** Kind-only id (legacy); prefer [iconIdFor] with order index for sprites. */
    fun iconIdFor(kind: StopKind): String = when (kind) {
        StopKind.PICKUP_LOAD -> ICON_PICKUP
        StopKind.DROP_OFF_UNLOAD -> ICON_DROPOFF
    }

    /** Runtime sprite id: kind prefix + 1-based stop order (baked into bitmap). */
    fun iconIdFor(kind: StopKind, orderIndex: Int): String =
        "${iconIdFor(kind)}_$orderIndex"

    fun colorHexFor(kind: StopKind): String = when (kind) {
        StopKind.PICKUP_LOAD -> PICKUP_COLOR_HEX
        StopKind.DROP_OFF_UNLOAD -> DROPOFF_COLOR_HEX
    }

    /** Stops with verified coordinates eligible for style-layer bind. */
    fun mappableStops(stops: List<RouteStop>): List<RouteStop> =
        stops.filter { it.latitude != null && it.longitude != null }

    /**
     * Active-route stops from [AppDataSource] (demo or real). Same bind path —
     * only the backing store differs.
     */
    fun stopsFrom(source: AppDataSource): List<RouteStop> =
        mappableStops(source.routes.activeRoute()?.stops.orEmpty())

    /**
     * Register numbered style images for each mappable stop (MapLibre sprite
     * atlas via [Style.addImage]). Idempotent per icon id.
     */
    fun ensureStopMarkerImages(style: Style, stops: List<RouteStop> = emptyList()) {
        val mappable = mappableStops(stops)
        if (mappable.isEmpty()) {
            // Keep base kind sprites available for legacy callers / empty bind.
            ensureBaseKindImages(style)
            return
        }
        mappable.forEachIndexed { index, stop ->
            val order = index + 1
            val id = iconIdFor(stop.kind, order)
            if (style.getImage(id) == null) {
                style.addImage(
                    id,
                    createStopMarkerBitmap(kind = stop.kind, orderIndex = order),
                    /* sdf = */ false,
                )
            }
        }
    }

    private fun ensureBaseKindImages(style: Style) {
        if (style.getImage(ICON_PICKUP) == null) {
            style.addImage(
                ICON_PICKUP,
                createStopMarkerBitmap(kind = StopKind.PICKUP_LOAD, orderIndex = 1),
                /* sdf = */ false,
            )
        }
        if (style.getImage(ICON_DROPOFF) == null) {
            style.addImage(
                ICON_DROPOFF,
                createStopMarkerBitmap(kind = StopKind.DROP_OFF_UNLOAD, orderIndex = 1),
                /* sdf = */ false,
            )
        }
    }

    /** @deprecated Use [ensureStopMarkerImages]. */
    fun ensureStopMarkerImage(style: Style) = ensureStopMarkerImages(style)

    /**
     * Clean numbered pin sprite: filled circle + white order index.
     * No chevron — index is the sole interior glyph (ATNA-121).
     */
    fun createStopMarkerBitmap(
        sizePx: Int = DEFAULT_ICON_PX,
        kind: StopKind = StopKind.PICKUP_LOAD,
        orderIndex: Int = 1,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHexFor(kind))
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.08f
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = sizePx * 0.36f
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)

        val label = orderIndex.toString()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * if (label.length >= 2) 0.34f else 0.42f
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, textPaint)
        return bitmap
    }
}
