package com.truckie1.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.RouteStop
import com.truckie1.trips.StopKind
import org.maplibre.android.maps.Style

/**
 * Route stop markers for MapLibre map home (`ff_stop_markers`).
 *
 * Pickup/load vs drop-off/unload use distinct style-layer icons. Stops come
 * from the active [AppDataSource] route (demo or real — same [RouteStop] schema).
 * Rendering reuses [MapStyleOverlayBind] + [MapLibreStyleLifecycle] with bridges.
 *
 * Out of scope: satellite peek. Gherkin: features/map_stop_markers.feature,
 * features/map_style_overlays.feature.
 */
object MapStopMarkersCatalog {
    const val FLAG_KEY: String = FlagKeys.STOP_MARKERS

    const val ICON_PICKUP: String = "truckie_stop_pickup"
    const val ICON_DROPOFF: String = "truckie_stop_dropoff"

    /** @deprecated Prefer [ICON_PICKUP] / [ICON_DROPOFF]; kept for legacy tests. */
    const val ICON_ID: String = ICON_PICKUP

    /** Catalog: style symbol layer — not Annotation SymbolManager / MarkerOptions. */
    const val usesStyleSymbolLayer: Boolean = true
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
     * Stop index labels need the same OpenFreeMap Noto stack as bridge clearance
     * text — otherwise symbol-layer icons vanish after text-field is set.
     */
    const val requiresOpenFreeMapCompatibleTextFont: Boolean =
        MapLibreSymbolText.requiresOpenFreeMapCompatibleTextFont

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

    fun iconIdFor(kind: StopKind): String = when (kind) {
        StopKind.PICKUP_LOAD -> ICON_PICKUP
        StopKind.DROP_OFF_UNLOAD -> ICON_DROPOFF
    }

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

    fun ensureStopMarkerImages(style: Style) {
        if (style.getImage(ICON_PICKUP) == null) {
            style.addImage(
                ICON_PICKUP,
                createStopMarkerBitmap(kind = StopKind.PICKUP_LOAD),
                /* sdf = */ false,
            )
        }
        if (style.getImage(ICON_DROPOFF) == null) {
            style.addImage(
                ICON_DROPOFF,
                createStopMarkerBitmap(kind = StopKind.DROP_OFF_UNLOAD),
                /* sdf = */ false,
            )
        }
    }

    /** @deprecated Use [ensureStopMarkerImages]. */
    fun ensureStopMarkerImage(style: Style) = ensureStopMarkerImages(style)

    fun createStopMarkerBitmap(
        sizePx: Int = DEFAULT_ICON_PX,
        kind: StopKind = StopKind.PICKUP_LOAD,
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
        val radius = sizePx * 0.32f
        canvas.drawCircle(cx, cy, radius, fill)
        canvas.drawCircle(cx, cy, radius, stroke)

        val arrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val path = Path()
        val tip = sizePx * 0.14f
        when (kind) {
            StopKind.PICKUP_LOAD -> {
                // Upward chevron — load onto vehicle.
                path.moveTo(cx, cy - tip)
                path.lineTo(cx + tip * 0.85f, cy + tip * 0.35f)
                path.lineTo(cx - tip * 0.85f, cy + tip * 0.35f)
                path.close()
            }
            StopKind.DROP_OFF_UNLOAD -> {
                // Downward chevron — unload.
                path.moveTo(cx, cy + tip)
                path.lineTo(cx + tip * 0.85f, cy - tip * 0.35f)
                path.lineTo(cx - tip * 0.85f, cy - tip * 0.35f)
                path.close()
            }
        }
        canvas.drawPath(path, arrow)
        return bitmap
    }
}
