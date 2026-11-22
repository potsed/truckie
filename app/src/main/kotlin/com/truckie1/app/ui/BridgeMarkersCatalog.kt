package com.truckie1.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import org.json.JSONArray
import org.maplibre.android.maps.Style
import java.util.Locale

/**
 * Low-bridge markers for MapLibre map home (`ff_bridge_markers`).
 * Australia-first sample/fixture points from a local JSON asset — not a live
 * GraphHopper / OSM / NHVR pack. Gherkin: features/bridge_markers.feature
 */
data class BridgePoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val clearanceHeightM: Double? = null,
)

object BridgeMarkersCatalog {
    const val ICON_ID: String = "truckie_bridge_marker"
    const val ASSET_PATH: String = "bridges/au_sample_bridges.json"
    const val REGION: String = "AU"

    /** Catalog: Studio must not see deprecated MarkerOptions / addMarker. */
    const val usesSymbolManager: Boolean = true
    const val usesDeprecatedMarkerApi: Boolean = false

    /** Warning amber — distinct from navy stop markers and blue location puck. */
    const val MARKER_COLOR_HEX: String = "#C47B16"

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.BRIDGE_MARKERS)

    fun shouldShow(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled && isEnabled(flags)

    fun clearanceLabel(bridge: BridgePoint): String? {
        val height = bridge.clearanceHeightM ?: return null
        return "%.1f m".format(Locale.US, height)
    }

    fun loadFromAssets(context: Context): List<BridgePoint> =
        runCatching {
            context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
                parseJson(reader.readText())
            }
        }.getOrDefault(emptyList())

    fun parseJson(raw: String): List<BridgePoint> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").trim()
                    val name = obj.optString("name").trim()
                    if (id.isEmpty() || name.isEmpty()) continue
                    if (!obj.has("lat") || !obj.has("lng")) continue
                    val lat = obj.getDouble("lat")
                    val lng = obj.getDouble("lng")
                    val clearance = if (obj.has("clearanceHeightM") && !obj.isNull("clearanceHeightM")) {
                        obj.getDouble("clearanceHeightM")
                    } else {
                        null
                    }
                    add(
                        BridgePoint(
                            id = id,
                            name = name,
                            latitude = lat,
                            longitude = lng,
                            clearanceHeightM = clearance,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun ensureBridgeMarkerImage(style: Style) {
        if (style.getImage(ICON_ID) != null) return
        style.addImage(ICON_ID, createBridgeMarkerBitmap())
    }

    fun createBridgeMarkerBitmap(sizePx: Int = 64): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val amber = Color.parseColor(MARKER_COLOR_HEX)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amber
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.07f
        }
        val path = Path().apply {
            moveTo(sizePx * 0.5f, sizePx * 0.12f)
            lineTo(sizePx * 0.88f, sizePx * 0.82f)
            lineTo(sizePx * 0.12f, sizePx * 0.82f)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
        // Small bridge-like bar inside the triangle for recognition at map scale.
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.06f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(sizePx * 0.34f, sizePx * 0.58f, sizePx * 0.66f, sizePx * 0.58f, bar)
        canvas.drawLine(sizePx * 0.38f, sizePx * 0.58f, sizePx * 0.38f, sizePx * 0.70f, bar)
        canvas.drawLine(sizePx * 0.62f, sizePx * 0.58f, sizePx * 0.62f, sizePx * 0.70f, bar)
        return bitmap
    }
}
