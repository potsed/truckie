package com.truckie1.app.ui

import com.truckie1.routing.RoutePath

/**
 * Caches the last MultiStopTourComposer-optimised [RoutePath] for map home
 * polyline reuse (`ff_map_route_polyline`). Cleared with Navigate/demo resets.
 */
class MapRoutePolylineSession {
    private var optimisedPath: RoutePath? = null

    fun lastOptimisedPath(): RoutePath? = optimisedPath

    fun rememberOptimised(path: RoutePath?) {
        optimisedPath = path
    }

    fun clear() {
        optimisedPath = null
    }
}
