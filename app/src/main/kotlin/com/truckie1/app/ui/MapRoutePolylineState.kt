package com.truckie1.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.MultiStopTourComposer
import com.truckie1.routing.PathRouter
import com.truckie1.trips.Route

/**
 * Async home polyline geometry for Compose (ATNA-88).
 *
 * Resolves PathRouter legs via [LaunchedEffect] — never from `remember` —
 * and reuses [MapRoutePolylineSession] preview / Optimise cache. Passes
 * [currentLocation] for ATNA-99 approach (current → start → stops → end).
 */
object MapRoutePolylineState {
    @Composable
    fun rememberDisplayPoints(
        flags: FeatureFlags,
        mapHomeEnabled: Boolean,
        route: Route?,
        vehicle: Vehicle?,
        bufferM: Double,
        pathRouter: PathRouter,
        composer: MultiStopTourComposer,
        session: MapRoutePolylineSession,
        navigatePathPoints: List<LatLngPoint>,
        showRoutePolyline: Boolean,
        revision: Int,
        currentLocation: LatLngPoint? = null,
    ): List<LatLngPoint> {
        var points by remember { mutableStateOf<List<LatLngPoint>>(emptyList()) }
        val bridgePassage = flags.isEnabled(FlagKeys.BRIDGE_PASSAGE)
        val valhallaLive = flags.isEnabled(FlagKeys.VALHALLA_LIVE)
        val fromLocation = flags.isEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION)
        val approach = RoutePathFromLocationCatalog.approachFix(
            flags = flags,
            currentLocation = currentLocation,
        )
        val cacheKey = session.previewCacheKey(
            route = route,
            vehicle = vehicle,
            bufferM = bufferM,
            bridgePassage = bridgePassage,
            valhallaLive = valhallaLive,
            currentLocation = approach,
            routePathFromLocation = fromLocation,
        )
        LaunchedEffect(
            route,
            vehicle,
            bufferM,
            navigatePathPoints,
            showRoutePolyline,
            revision,
            cacheKey,
            bridgePassage,
            valhallaLive,
            fromLocation,
            approach?.latitude,
            approach?.longitude,
            flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE),
            flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO),
        ) {
            when {
                navigatePathPoints.size >= 2 -> {
                    points = navigatePathPoints
                }
                !showRoutePolyline -> {
                    points = emptyList()
                }
                else -> {
                    session.cachedPreview(cacheKey)?.let { cached ->
                        points = cached.points
                        return@LaunchedEffect
                    }
                    val result = MapRoutePolylineGeometry.resolve(
                        flags = flags,
                        mapHomeEnabled = mapHomeEnabled,
                        route = route,
                        vehicle = vehicle,
                        bufferM = bufferM,
                        pathRouter = pathRouter,
                        composer = composer,
                        optimisedPath = session.lastOptimisedPath(),
                        navigatePath = null,
                        currentLocation = currentLocation,
                    )
                    val ready = (result as? MapRoutePolylineGeometry.Result.Ready)?.path
                    if (ready != null) {
                        session.rememberPreview(cacheKey, ready)
                    }
                    points = MapRoutePolylineGeometry.pointsOrEmpty(result)
                }
            }
        }
        return points
    }
}
