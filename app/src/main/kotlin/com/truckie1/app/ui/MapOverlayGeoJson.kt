package com.truckie1.app.ui

import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.RouteStop

/**
 * Pure GeoJSON FeatureCollection builders for MapLibre style overlays (ATNA-117).
 *
 * Output is raw GeoJSON so unit tests assert Annotation-era parity without a
 * live [org.maplibre.android.maps.Style] or Android [org.json] mocks.
 * [MapLibreHost] feeds these strings into
 * [org.maplibre.android.style.sources.GeoJsonSource].
 *
 * Gherkin: features/map_style_overlays.feature
 */
object MapOverlayGeoJson {
    const val PROP_ICON: String = "icon"
    const val PROP_LABEL: String = "label"

    fun bridges(specs: List<BridgeSymbolSpec>): String {
        val features = specs.joinToString(",") { spec ->
            val props = buildString {
                append("\"id\":").append(jsonString(spec.bridge.id))
                append(",\"name\":").append(jsonString(spec.bridge.name))
                append(",\"").append(PROP_ICON).append("\":")
                    .append(jsonString(BridgeMarkersCatalog.ICON_ID))
                spec.clearanceLabel?.let { label ->
                    append(",\"").append(PROP_LABEL).append("\":")
                        .append(jsonString(label))
                }
            }
            pointFeatureJson(
                longitude = spec.bridge.longitude,
                latitude = spec.bridge.latitude,
                propertiesJson = props,
            )
        }
        return featureCollectionJson(features)
    }

    fun stops(stops: List<RouteStop>): String {
        val mappable = MapStopMarkersCatalog.mappableStops(stops)
        val features = mappable.mapIndexed { index, stop ->
            val props = buildString {
                append("\"id\":").append(jsonString(stop.id))
                append(",\"").append(PROP_ICON).append("\":")
                    .append(jsonString(MapStopMarkersCatalog.iconIdFor(stop.kind)))
                append(",\"").append(PROP_LABEL).append("\":")
                    .append(jsonString("${index + 1}"))
            }
            pointFeatureJson(
                longitude = stop.longitude!!,
                latitude = stop.latitude!!,
                propertiesJson = props,
            )
        }.joinToString(",")
        return featureCollectionJson(features)
    }

    fun routeLine(points: List<LatLngPoint>): String {
        if (points.size < 2) return featureCollectionJson("")
        val coords = points.joinToString(",") { point ->
            "[${point.longitude},${point.latitude}]"
        }
        val feature = """{"type":"Feature","geometry":{"type":"LineString","coordinates":[$coords]},"properties":{}}"""
        return featureCollectionJson(feature)
    }

    private fun pointFeatureJson(
        longitude: Double,
        latitude: Double,
        propertiesJson: String,
    ): String =
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[$longitude,$latitude]},"properties":{$propertiesJson}}"""

    private fun featureCollectionJson(featuresCsv: String): String =
        """{"type":"FeatureCollection","features":[$featuresCsv]}"""

    private fun jsonString(value: String): String =
        buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
}
