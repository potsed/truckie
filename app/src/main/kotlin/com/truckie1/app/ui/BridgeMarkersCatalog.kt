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
 * public-domain SVG rasterised for a style symbol layer (`au_road_sign_w5_8`).
 *
 * Long-term data source is a **downloadable, updatable** [BridgePackRepository]
 * (versioned cache → GeoJSONSource + symbol layer). The baked-in sample asset
 * is **bootstrap only** when the cache is empty. Gherkin:
 * features/bridge_markers.feature, features/bridge_pack.feature,
 * features/map_style_overlays.feature, features/verified_cbd_clearances.feature.
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
    /**
     * Vehicle passage relation (ATNA-48): under / over / both / unknown.
     * Height conflicts apply to under-paths only — OK to drive over the deck.
     */
    val passage: BridgePassage = BridgePassage.UNKNOWN,
    /** AU state/territory code when known (OSS / Truckie source pack). */
    val state: String? = null,
    /** Local government area when known — null over inventing. */
    val lga: String? = null,
    /** Road name going under the structure (height-restricted path). */
    val roadUnder: String? = null,
    /** Road / feature going over the structure when known. */
    val roadOver: String? = null,
    /** Required on OSS pack rows; optional on legacy bootstrap fixtures. */
    val source: BridgeSource? = null,
    /** Optional published weight/load restriction — null when unknown. */
    val weightRestriction: String? = null,
    /** Optional signage note; roadside signs still override the pack. */
    val signageNote: String? = null,
)

object BridgeMarkersCatalog {
    const val ICON_ID: String = "truckie_bridge_marker_w5_8"
    const val ASSET_PATH: String = "bridges/au_sample_bridges.json"
    /** Source SVG kept in assets for provenance (Wikimedia Commons, PD). */
    const val W5_8_SVG_ASSET: String = "bridges/AU_road_sign_W5-8.svg"
    const val REGION: String = "AU"

    /** Catalog: style symbol layer — not Annotation SymbolManager / MarkerOptions. */
    const val usesStyleSymbolLayer: Boolean = true
    const val usesSymbolManager: Boolean = false
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
                    val explicitPassage = obj.optString("passage").trim()
                        .takeIf { it.isNotEmpty() }
                        ?.let { BridgePassage.fromWire(it) }
                    val passage = BridgePassageInference.resolve(
                        id = id,
                        clearanceProvider = provider,
                        clearanceHeightM = clearance,
                        clearanceStatus = status,
                        explicit = explicitPassage,
                    )
                    val packSource = parseBridgeSource(obj)
                    val resolvedRetrieved = retrievedAt
                        ?: packSource?.retrievedAt
                    val resolvedCitation = source
                        ?: packSource?.citation()
                    add(
                        BridgePoint(
                            id = id,
                            name = name,
                            latitude = lat,
                            longitude = lng,
                            clearanceHeightM = clearance,
                            clearanceStatus = status,
                            clearanceSource = resolvedCitation,
                            clearanceSourceId = sourceId,
                            clearanceRetrievedAt = resolvedRetrieved,
                            clearanceProvider = provider,
                            passage = passage,
                            state = optNullableString(obj, "state"),
                            lga = optNullableString(obj, "lga"),
                            roadUnder = optNullableString(obj, "roadUnder"),
                            roadOver = optNullableString(obj, "roadOver"),
                            source = packSource,
                            weightRestriction = optNullableString(obj, "weightRestriction"),
                            signageNote = optNullableString(obj, "signageNote"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * org.json [JSONObject.optString] returns the literal `"null"` for JSON null —
     * treat missing / JSON null / blank as Kotlin null (prefer null over inventing).
     */
    fun optNullableString(obj: org.json.JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    /** Parse required OSS `source` object; null when absent (legacy bootstrap rows). */
    fun parseBridgeSource(obj: org.json.JSONObject): BridgeSource? {
        val nested = obj.optJSONObject("source") ?: return null
        val name = nested.optString("name").trim()
        val url = nested.optString("url").trim()
        val retrievedAt = nested.optString("retrievedAt").trim()
        if (name.isEmpty() || url.isEmpty() || retrievedAt.isEmpty()) return null
        return BridgeSource(name = name, url = url, retrievedAt = retrievedAt)
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
