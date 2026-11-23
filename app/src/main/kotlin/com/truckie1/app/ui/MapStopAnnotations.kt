package com.truckie1.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.maplibre.android.maps.Style

/**
 * MapLibre Annotation plugin helpers for stop markers.
 * Replaces deprecated [org.maplibre.android.annotations.MarkerOptions] / [org.maplibre.android.maps.MapLibreMap.addMarker].
 */
object MapStopAnnotations {
    const val ICON_ID: String = "truckie_stop_marker"

    /** Catalog: Studio must not see deprecated MarkerOptions / addMarker / clear. */
    const val usesSymbolManager: Boolean = true
    const val usesDeprecatedMarkerApi: Boolean = false

    /**
     * Stop index labels need the same OpenFreeMap Noto stack as bridge clearance
     * text — otherwise SymbolManager icons vanish after textField is set.
     */
    const val requiresOpenFreeMapCompatibleTextFont: Boolean =
        MapLibreSymbolText.requiresOpenFreeMapCompatibleTextFont

    val symbolTextFont: Array<String> = MapLibreSymbolText.openFreeMapTextFont

    fun ensureStopMarkerImage(style: Style) {
        if (style.getImage(ICON_ID) != null) return
        style.addImage(ICON_ID, createStopMarkerBitmap())
    }

    fun createStopMarkerBitmap(sizePx: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1F33") // Truckie navy
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
        return bitmap
    }
}
