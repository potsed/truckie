package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.maps.MapCameraTarget
import com.truckie1.routing.LatLngPoint
import com.truckie1.trips.RouteStop

/**
 * One immutable bind payload for [MapLibreHost] (ATNA-90 / P1-5).
 *
 * Compose / [MapHomeController] precomputes bridge clearance labels so the host
 * never looks up [TruckieApp] feature flags during style-layer bind.
 *
 * Gherkin: features/map_home_orchestration.feature, features/map_style_overlays.feature
 */
data class BridgeSymbolSpec(
    val bridge: BridgePoint,
    val clearanceLabel: String?,
)

data class MapLibreBindSnapshot(
    val stops: List<RouteStop>,
    val routePathPoints: List<LatLngPoint>,
    val bridges: List<BridgeSymbolSpec>,
    val prioritiseStreetLabels: Boolean,
    val showLocationPuck: Boolean,
    val locationFollowEnabled: Boolean,
    val headingUpEnabled: Boolean,
    val locationFix: LocationFix?,
    val cameraTarget: MapCameraTarget,
    val isFollowing: Boolean,
) {
    companion object {
        fun bridgeSpecs(
            bridges: List<BridgePoint>,
            flags: FeatureFlags,
        ): List<BridgeSymbolSpec> =
            BridgeVisibilityPipeline.forStyleBind(bridges).map { bridge ->
                BridgeSymbolSpec(
                    bridge = bridge,
                    clearanceLabel = BridgeMarkersCatalog.clearanceLabel(bridge, flags),
                )
            }
    }
}
