package com.truckie1.app.ui

import com.truckie1.profiles.Vehicle
import com.truckie1.routing.LatLngPoint
import com.truckie1.routing.RoutePath
import com.truckie1.trips.Route

/**
 * Caches MultiStopTourComposer geometry for map home polyline reuse
 * (`ff_map_route_polyline`, ATNA-88). Holds last Optimise path and a keyed
 * preview compose so Compose never re-enters PathRouter from `remember`.
 */
class MapRoutePolylineSession {
    private var optimisedPath: RoutePath? = null
    private var previewKey: String? = null
    private var previewPath: RoutePath? = null

    fun lastOptimisedPath(): RoutePath? = optimisedPath

    fun rememberOptimised(path: RoutePath?) {
        optimisedPath = path
    }

    fun previewCacheKey(
        route: Route?,
        vehicle: Vehicle?,
        bufferM: Double,
        bridgePassage: Boolean,
        graphHopperLive: Boolean,
        currentLocation: LatLngPoint? = null,
        routePathFromLocation: Boolean = false,
    ): String {
        if (route == null) return "none"
        val stopPart = route.stops.joinToString("|") {
            "${it.id}:${it.latitude},${it.longitude}"
        }
        val start = route.start?.let { "${it.latitude},${it.longitude}" }.orEmpty()
        val end = route.end?.let { "${it.latitude},${it.longitude}" }.orEmpty()
        val approach = currentLocation?.let {
            "${"%.5f".format(it.latitude)},${"%.5f".format(it.longitude)}"
        }.orEmpty()
        return listOf(
            route.id,
            start,
            end,
            stopPart,
            vehicle?.rego.orEmpty(),
            bufferM.toString(),
            bridgePassage.toString(),
            graphHopperLive.toString(),
            routePathFromLocation.toString(),
            approach,
        ).joinToString("#")
    }

    fun cachedPreview(key: String): RoutePath? =
        if (previewKey == key) previewPath else null

    fun rememberPreview(key: String, path: RoutePath?) {
        previewKey = key
        previewPath = path
    }

    fun clear() {
        optimisedPath = null
        previewKey = null
        previewPath = null
    }
}
