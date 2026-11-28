package com.truckie1.app.profiles

import com.truckie1.trips.ActiveTourFacade
import com.truckie1.trips.ActiveTourSession
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.Route
import com.truckie1.trips.RouteDateChoice
import com.truckie1.trips.RouteEndpoint
import com.truckie1.trips.RouteStop
import com.truckie1.trips.TripStop
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Room-backed [ActiveTourFacade] (ATNA-89 / P1-4).
 *
 * [ActiveTourSession] remains the in-process editable SoT; every mutation
 * write-throughs to [ActiveTourDao] on [io]. Hydrates once at construction
 * so process death restores the active route. Demo mode does **not** use this.
 *
 * Gherkin: features/room_route_persist.feature
 */
class RoomActiveTour(
    private val dao: ActiveTourDao,
    private val session: ActiveTourSession = ActiveTourSession(),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ActiveTourFacade {

    init {
        hydrate()
    }

    private fun hydrate() {
        val snapshot = db { dao.loadActiveTour() }
        session.replaceActive(snapshot.route)
        session.lastOptimizeMode = snapshot.lastOptimizeMode
    }

    private fun persist() {
        db {
            dao.replaceActiveTour(
                route = session.activeRoute(),
                lastOptimizeMode = session.lastOptimizeMode,
            )
        }
    }

    private fun <T> db(block: suspend () -> T): T = runBlocking(io) { block() }

    // --- RouteRepository ---

    override fun activeRoute(): Route? = session.activeRoute()

    override fun createRoute(
        name: String?,
        dateChoice: RouteDateChoice,
        today: LocalDate,
        customDate: LocalDate?,
        start: RouteEndpoint?,
        end: RouteEndpoint?,
    ): Route {
        val route = session.createRoute(
            name = name,
            dateChoice = dateChoice,
            today = today,
            customDate = customDate,
            start = start,
            end = end,
        )
        persist()
        return route
    }

    override fun addStop(stop: RouteStop) {
        session.addStop(stop)
        persist()
    }

    override fun removeStop(id: String) {
        session.removeStop(id)
        persist()
    }

    override fun clear() {
        session.clear()
        persist()
    }

    override fun replaceActive(route: Route?) {
        session.replaceActive(route)
        persist()
    }

    // --- TripRepository ---

    override var lastOptimizeMode: OptimizeMode?
        get() = session.lastOptimizeMode
        set(value) {
            session.lastOptimizeMode = value
            persist()
        }

    override fun stops(): List<TripStop> = session.stops()

    override fun addStop(stop: TripStop) {
        session.addStop(stop)
        persist()
    }

    override fun moveStop(fromIndex: Int, toIndex: Int) {
        session.moveStop(fromIndex, toIndex)
        persist()
    }

    override fun replaceStops(newStops: List<TripStop>) {
        session.replaceStops(newStops)
        persist()
    }

    override fun updateStop(stop: TripStop) {
        session.updateStop(stop)
        persist()
    }
}
