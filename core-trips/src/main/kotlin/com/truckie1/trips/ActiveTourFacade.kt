package com.truckie1.trips

/**
 * Single active-tour contract for map-home [Route] + Plan Trip / Optimise
 * [TripStop] projection (ATNA-86). Real mode may back this with Room
 * (`ff_room_route_persist`, ATNA-89); demo uses a read-only in-memory seed.
 *
 * [AppDataSource.routes] and [AppDataSource.trips] must be the **same**
 * [ActiveTourFacade] instance.
 */
interface ActiveTourFacade : RouteRepository, TripRepository
