package com.truckie1.trips

import java.time.LocalDate

/**
 * Active [Route] store for the map-first home drag sheet.
 * Real and demo modes share this contract — only the backing dataset differs.
 * Implemented by [ActiveTourFacade] / [ActiveTourSession] (ATNA-86 / ATNA-89).
 */
interface RouteRepository {
    fun activeRoute(): Route?

    fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate = LocalDate.now(),
        customDate: LocalDate? = null,
        start: RouteEndpoint? = null,
        end: RouteEndpoint? = null,
    ): Route

    fun addStop(stop: RouteStop)
    fun removeStop(id: String)
    fun clear()

    /** Replaces the active route in one shot. Pass null to clear. */
    fun replaceActive(route: Route?)
}
