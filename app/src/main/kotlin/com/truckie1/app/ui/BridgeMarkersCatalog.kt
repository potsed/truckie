package com.truckie1.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import com.truckie1.app.R
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.json.JSONArray
import org.maplibre.android.maps.Style
import java.util.Locale

/**
 * Low-bridge markers for MapLibre map home (`ff_bridge_markers`).
 *
 * Icon: Australian road sign **W5-8** (low level bridge) — Wikimedia Commons
 * public-domain SVG rasterised for SymbolManager (`au_road_sign_w5_8`).
 *
 * Long-term data source is a **downloadable, updatable** [BridgePackRepository]
 * (versioned cache → SymbolManager). The baked-in sample asset is **bootstrap
 * only** when the cache is empty. Gherkin: features/bridge_markers.feature,
 * features/bridge_pack.feature, features/verified_cbd_clearances.feature.
 */
data class BridgePoint(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val clearanceHeightM: Double? = null,
    val clearanceStatus: BridgeClearanceStatus = BridgeClearanceStatus.UNKNOWN,
    val clearanceSource: String? = null,
    val clearanceSourceId: String? = null,
    val clearanceRetrievedAt: String? = null,
    /** Optional provenance provider wire name (e.g. queensland_rail). */
    val clearanceProvider: String? = null,
)

object BridgeMarkersCatalog {
    const val ICON_ID: String = "truckie_bridge_marker_w5_8"
    const val ASSET_PATH: String = "bridges/au_sample_bridges.json"
    /** Source SVG kept in assets for provenance (Wikimedia Commons, PD). */
    const val W5_8_SVG_ASSET: String = "bridges/AU_road_sign_W5-8.svg"
    const val REGION: String = "AU"

    /** Catalog: Studio must not see deprecated MarkerOptions / addMarker. */
    const val usesSymbolManager: Boolean = true
    const val usesDeprecatedMarkerApi: Boolean = false
    const val usesAuRoadSignW58: Boolean = true

    /** Intrinsic raster size registered with MapLibre (dp-ish at xxhdpi). */
    const val DEFAULT_ICON_PX: Int = 96

    /** Warning amber — fallback triangle only if W5-8 drawable is missing. */
    const val MARKER_COLOR_HEX: String = "#C47B16"

    fun isEnabled(flags: FeatureFlags): Boolean = flags.isEnabled(FlagKeys.BRIDGE_MARKERS)

    fun shouldShow(flags: FeatureFlags, mapHomeEnabled: Boolean): Boolean =
        mapHomeEnabled && isEnabled(flags)

    fun clearanceLabel(
        bridge: BridgePoint,
        flags: FeatureFlags = LocalFeatureFlags(),
    ): String? {
        if (!VerifiedCbdClearances.allowsHeightLabel(bridge, flags)) return null
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
                    val status = BridgeClearanceStatus.fromWire(
                        obj.optString("clearanceStatus").takeIf { it.isNotBlank() },
                    )
                    val source = obj.optString("clearanceSource").trim().takeIf { it.isNotEmpty() }
                    val sourceId = obj.optString("clearanceSourceId").trim().takeIf { it.isNotEmpty() }
                    val retrievedAt = obj.optString("clearanceRetrievedAt").trim()
                        .takeIf { it.isNotEmpty() }
                    val provider = obj.optString("clearanceProvider").trim()
                        .takeIf { it.isNotEmpty() }
                    add(
                        BridgePoint(
                            id = id,
                            name = name,
                            latitude = lat,
                            longitude = lng,
                            clearanceHeightM = clearance,
                            clearanceStatus = status,
                            clearanceSource = source,
                            clearanceSourceId = sourceId,
                            clearanceRetrievedAt = retrievedAt,
                            clearanceProvider = provider,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun ensureBridgeMarkerImage(style: Style, context: Context) {
        if (style.getImage(ICON_ID) != null) return
        // Explicit non-SDF — coloured W5-8 must not be treated as a template icon.
        style.addImage(ICON_ID, createBridgeMarkerBitmap(context = context), /* sdf = */ false)
    }

    /**
     * Prefer bundled AU W5-8 road-sign PNG (from Wikimedia SVG); fall back to a
     * procedural diamond if the drawable is unavailable (tests / mispackaged APK).
     */
    fun createBridgeMarkerBitmap(
        sizePx: Int = DEFAULT_ICON_PX,
        context: Context? = null,
    ): Bitmap {
        val fromDrawable = context?.let { loadW58FromDrawable(it, sizePx) }
        if (fromDrawable != null) return fromDrawable
        return createFallbackDiamondBitmap(sizePx)
    }

    private fun loadW58FromDrawable(context: Context, sizePx: Int): Bitmap? =
        runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.au_road_sign_w5_8)
                ?: return@runCatching null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        }.getOrNull() ?: runCatching {
            // Decode the raw PNG resource if drawable inflation fails in unit tests.
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val raw = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.au_road_sign_w5_8,
                opts,
            ) ?: return@runCatching null
            if (raw.width == sizePx && raw.height == sizePx) {
                raw
            } else {
                Bitmap.createScaledBitmap(raw, sizePx, sizePx, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        }.getOrNull()

    /** Procedural yellow diamond — last-resort stand-in for W5-8. */
    fun createFallbackDiamondBitmap(sizePx: Int = DEFAULT_ICON_PX): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val amber = Color.parseColor(MARKER_COLOR_HEX)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amber
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.06f
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val r = sizePx * 0.42f
        val path = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
        return bitmap
    }

    @Deprecated(
        "Use createBridgeMarkerBitmap(sizePx, context) with W5-8 drawable",
        ReplaceWith("createBridgeMarkerBitmap(sizePx, context)"),
    )
    fun createBridgeMarkerBitmapLegacyTriangle(sizePx: Int = 64): Bitmap =
        createFallbackDiamondBitmap(sizePx)
}
