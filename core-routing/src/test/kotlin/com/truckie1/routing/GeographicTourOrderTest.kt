package com.truckie1.routing

import com.truckie1.profiles.Vehicle
import com.truckie1.trips.Cargo
import com.truckie1.trips.CargoAction
import com.truckie1.trips.OptimizeMode
import com.truckie1.trips.TripStop
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/geographic_tour_order.feature —
 * SEQ-like zig-zag must not pick farthest first (ATNA-120).
 */
class GeographicTourOrderTest {

    private val vehicle = Vehicle(
        rego = "104YQ3",
        nickname = "Rigid 3.5",
        vehicleType = "Rigid",
        make = "Hino",
        model = "500",
        year = 2024,
        heightM = 3.5,
        widthM = 2.5,
        lengthM = 8.0,
        massT = 8.0,
        tareT = 5.0,
        gvmT = 12.0,
        gcmT = 12.0,
        axleCount = 2,
    )

    /** DHL Boronia Rd — north SEQ airport depot. */
    private val start = TripStop(
        id = "start",
        addressLabel = "DHL Express, 1A Boronia Road, Brisbane Airport QLD 4008",
        latitude = -27.419949,
        longitude = 153.099544,
    )
    private val end = start.copy(id = "end")

    private val lytton = TripStop(
        id = "lytton",
        addressLabel = "42 South Street, Lytton QLD 4178",
        latitude = -27.4168,
        longitude = 153.1542,
        cargo = listOf(Cargo("Crates", 3.8, 2.2, 3.0, 2.0, CargoAction.PICK_UP)),
    )
    /** Far south — must not be first intermediate after Optimise. */
    private val yatala = TripStop(
        id = "yatala",
        addressLabel = "18 Christensen Road, Yatala QLD 4207",
        latitude = -27.7665,
        longitude = 153.2368,
        cargo = listOf(Cargo("Pallets", 3.2, 2.4, 4.0, 2.5, CargoAction.PICK_UP)),
    )
    private val eagle = TripStop(
        id = "eagle",
        addressLabel = "55 Schneider Road, Eagle Farm QLD 4009",
        latitude = -27.4305,
        longitude = 153.0912,
    )
    private val hemmant = TripStop(
        id = "hemmant",
        addressLabel = "120 Tingalpa Road, Hemmant QLD 4174",
        latitude = -27.4502,
        longitude = 153.1295,
        cargo = listOf(Cargo("Crates", 3.8, 2.2, 3.0, 2.0, CargoAction.DROP_OFF)),
    )
    private val acacia = TripStop(
        id = "acacia",
        addressLabel = "25 Beatty Road, Acacia Ridge QLD 4110",
        latitude = -27.5861,
        longitude = 153.0264,
        cargo = listOf(Cargo("Pallets", 3.2, 2.4, 4.0, 2.5, CargoAction.DROP_OFF)),
    )
    private val wynnum = TripStop(
        id = "wynnum",
        addressLabel = "8 Colina Street, Wynnum QLD 4178",
        latitude = -27.44395,
        longitude = 153.16905,
    )

    /** Raw seed order: start north → far south → back north → south again. */
    private val zigZagTour: List<TripStop> = listOf(
        start, lytton, yatala, eagle, hemmant, acacia, wynnum, end,
    )

    @Test
    fun seqLikeZigZagDoesNotPickFarthestFirstWhenGeographicOrderOn() = runBlocking {
        val composer = MultiStopTourComposer(
            pathRouter = StubPathRouter(),
            geographicTourOrder = true,
        )
        val zigZag = composer.composeOrdered(
            stops = zigZagTour,
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        ) as OptimizeResult.Ok

        val result = composer.optimize(
            stops = zigZagTour,
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        )
        assertTrue(result is OptimizeResult.Ok)
        val ok = result as OptimizeResult.Ok

        assertEquals("start", ok.orderedStops.first().id)
        assertEquals("end", ok.orderedStops.last().id)
        val firstIntermediate = ok.orderedStops[1].id
        assertNotEquals(
            "Geographic Optimise must not leave farthest Yatala first after start; got ${ok.orderedStops.map { it.id }}",
            "yatala",
            firstIntermediate,
        )
        assertTrue(
            "Optimised tour must beat raw zig-zag duration (${zigZag.route.durationSeconds} vs ${ok.route.durationSeconds})",
            ok.route.durationSeconds < zigZag.route.durationSeconds,
        )
    }

    @Test
    fun withoutGeographicFlagEndMayLeaveLastSlot() = runBlocking {
        val composer = MultiStopTourComposer(
            pathRouter = StubPathRouter(),
            geographicTourOrder = false,
        )
        val result = composer.optimize(
            stops = zigZagTour,
            vehicle = vehicle,
            bufferM = 0.15,
            mode = OptimizeMode.FASTEST,
        ) as OptimizeResult.Ok
        // Legacy origin-only permutes end into the middle — fixed by the flag.
        assertNotEquals("end", result.orderedStops.last().id)
        assertTrue(result.orderedStops.any { it.id == "end" })
    }
}
