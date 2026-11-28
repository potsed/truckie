package com.truckie1.app.ui

import android.graphics.Color
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * One shared GeoJSONSource + style-layer ensure/rebind path for home-map
 * overlays (bridges, stops, route). Replaces Annotation SymbolManager /
 * LineManager obtain (ATNA-117).
 *
 * Gherkin: features/map_style_overlays.feature
 */
object MapStyleOverlayBind {
    const val sharesPathWithBridgeStopAndRoute: Boolean = true
    const val usesMapLibreStyleLifecycle: Boolean = true

    fun setGeoJson(style: Style, sourceId: String, geoJson: String) {
        val existing = style.getSourceAs<GeoJsonSource>(sourceId)
        if (existing != null) {
            existing.setGeoJson(geoJson)
            return
        }
        style.addSource(GeoJsonSource(sourceId, geoJson))
    }

    fun ensureBridgeSymbolLayer(style: Style) {
        if (style.getLayer(MapStyleOverlayIds.BRIDGE_LAYER) != null) return
        val layer = SymbolLayer(
            MapStyleOverlayIds.BRIDGE_LAYER,
            MapStyleOverlayIds.BRIDGE_SOURCE,
        ).withProperties(
            PropertyFactory.iconImage(Expression.get(MapOverlayGeoJson.PROP_ICON)),
            PropertyFactory.iconSize(MapStyleOverlayCatalog.IconSize.BRIDGE_W58),
            PropertyFactory.iconAnchor(MapStyleOverlayCatalog.BridgeLabel.ICON_ANCHOR),
            PropertyFactory.iconAllowOverlap(MapStyleOverlayCatalog.iconAllowOverlap),
            PropertyFactory.iconIgnorePlacement(MapStyleOverlayCatalog.iconIgnorePlacement),
            PropertyFactory.textField(Expression.get(MapOverlayGeoJson.PROP_LABEL)),
            PropertyFactory.textFont(MapLibreSymbolText.openFreeMapTextFont),
            PropertyFactory.textSize(MapStyleOverlayCatalog.BridgeLabel.TEXT_SIZE),
            PropertyFactory.textColor(
                Color.parseColor(MapStyleOverlayCatalog.BridgeLabel.TEXT_COLOR_HEX),
            ),
            PropertyFactory.textHaloColor(
                Color.parseColor(MapStyleOverlayCatalog.BridgeLabel.TEXT_HALO_COLOR_HEX),
            ),
            PropertyFactory.textHaloWidth(MapStyleOverlayCatalog.BridgeLabel.TEXT_HALO_WIDTH),
            PropertyFactory.textOffset(
                MapStyleOverlayCatalog.BridgeLabel.TEXT_OFFSET
                    .map { it }.toTypedArray(),
            ),
            PropertyFactory.textAnchor(MapStyleOverlayCatalog.BridgeLabel.TEXT_ANCHOR),
            PropertyFactory.textAllowOverlap(MapStyleOverlayCatalog.textAllowOverlap),
            PropertyFactory.textOptional(MapStyleOverlayCatalog.textOptional),
        )
        style.addLayer(layer)
    }

    fun ensureStopSymbolLayer(style: Style) {
        if (style.getLayer(MapStyleOverlayIds.STOP_LAYER) != null) return
        val layer = SymbolLayer(
            MapStyleOverlayIds.STOP_LAYER,
            MapStyleOverlayIds.STOP_SOURCE,
        ).withProperties(
            PropertyFactory.iconImage(Expression.get(MapOverlayGeoJson.PROP_ICON)),
            PropertyFactory.iconSize(MapStyleOverlayCatalog.IconSize.STOP),
            PropertyFactory.iconAnchor(MapStyleOverlayCatalog.StopLabel.ICON_ANCHOR),
            PropertyFactory.iconAllowOverlap(MapStyleOverlayCatalog.iconAllowOverlap),
            PropertyFactory.iconIgnorePlacement(MapStyleOverlayCatalog.iconIgnorePlacement),
            PropertyFactory.textField(Expression.get(MapOverlayGeoJson.PROP_LABEL)),
            PropertyFactory.textFont(MapLibreSymbolText.openFreeMapTextFont),
            PropertyFactory.textSize(MapStyleOverlayCatalog.StopLabel.TEXT_SIZE),
            PropertyFactory.textColor(
                Color.parseColor(MapStyleOverlayCatalog.StopLabel.TEXT_COLOR_HEX),
            ),
            PropertyFactory.textHaloColor(
                Color.parseColor(MapStyleOverlayCatalog.StopLabel.TEXT_HALO_COLOR_HEX),
            ),
            PropertyFactory.textHaloWidth(MapStyleOverlayCatalog.StopLabel.TEXT_HALO_WIDTH),
            PropertyFactory.textOffset(
                MapStyleOverlayCatalog.StopLabel.TEXT_OFFSET
                    .map { it }.toTypedArray(),
            ),
            PropertyFactory.textAllowOverlap(MapStyleOverlayCatalog.textAllowOverlap),
            PropertyFactory.textOptional(MapStyleOverlayCatalog.textOptional),
        )
        style.addLayer(layer)
    }

    fun ensureRouteLineLayer(style: Style) {
        if (style.getLayer(MapStyleOverlayIds.ROUTE_LAYER) != null) return
        val layer = LineLayer(
            MapStyleOverlayIds.ROUTE_LAYER,
            MapStyleOverlayIds.ROUTE_SOURCE,
        ).withProperties(
            PropertyFactory.lineColor(Color.parseColor(MapRoutePathStyle.LINE_COLOR_HEX)),
            PropertyFactory.lineWidth(MapRoutePathStyle.LINE_WIDTH),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        )
        // Route under stop/bridge symbols so W5-8 and stop indices stay readable.
        style.addLayer(layer)
    }

    fun bindBridges(style: Style, bridges: List<BridgeSymbolSpec>) {
        setGeoJson(style, MapStyleOverlayIds.BRIDGE_SOURCE, MapOverlayGeoJson.bridges(bridges))
        ensureBridgeSymbolLayer(style)
    }

    fun bindStops(style: Style, stops: List<com.truckie1.trips.RouteStop>) {
        setGeoJson(style, MapStyleOverlayIds.STOP_SOURCE, MapOverlayGeoJson.stops(stops))
        ensureStopSymbolLayer(style)
    }

    fun bindRoute(style: Style, points: List<com.truckie1.routing.LatLngPoint>) {
        setGeoJson(style, MapStyleOverlayIds.ROUTE_SOURCE, MapOverlayGeoJson.routeLine(points))
        ensureRouteLineLayer(style)
    }

    /**
     * When an overlay layer id is absent from the live style, recreate on next bind.
     * Same orphan rule Annotation managers used via [MapLibreStyleLifecycle].
     */
    fun mustRecreateOverlay(
        hasBoundOverlay: Boolean,
        styleContainsLayer: Boolean,
    ): Boolean = MapLibreStyleLifecycle.mustRecreateOverlay(
        hasBoundOverlay = hasBoundOverlay,
        styleContainsLayer = styleContainsLayer,
    )
}
