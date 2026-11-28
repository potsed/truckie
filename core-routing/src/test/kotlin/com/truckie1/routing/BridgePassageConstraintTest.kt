package com.truckie1.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation.feature —
 * under-passage hard-block; over deck OK.
 */
class BridgePassageConstraintTest {

    private val underLow = RoutingBridgeConstraint(
        id = "qr-colina",
        latitude = -27.443741,
        longitude = 153.168832,
        clearanceHeightM = 1.9,
        passage = RoutingBridgePassage.UNDER,
    )

    private val overDeck = RoutingBridgeConstraint(
        id = "captain-cook",
        latitude = -27.476,
        longitude = 153.029,
        clearanceHeightM = 1.9,
        passage = RoutingBridgePassage.OVER,
    )

    @Test
    fun underPassageBelowClearanceHardBlocks() {
        assertTrue(
            BridgePassageConstraint.heightConflictBlocksPath(
                bridge = underLow,
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                pathRelation = RoutingBridgePassage.UNDER,
            ),
        )
    }

    @Test
    fun overDeckNeverBlockedByUnderClearance() {
        assertFalse(
            BridgePassageConstraint.heightConflictBlocksPath(
                bridge = underLow,
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                pathRelation = RoutingBridgePassage.OVER,
            ),
        )
        assertFalse(
            BridgePassageConstraint.heightConflictBlocksPath(
                bridge = overDeck,
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                pathRelation = RoutingBridgePassage.UNDER,
            ),
        )
    }

    @Test
    fun filtersUnderRestrictingBridgesOnly() {
        val filtered = BridgePassageConstraint.bridgesRestrictingUnder(
            listOf(underLow, overDeck),
        )
        assertEquals(listOf("qr-colina"), filtered.map { it.id })
    }

    @Test
    fun pathNearUnderLowBridgeIsUnsafe() {
        val points = listOf(
            LatLngPoint(-27.44395, 153.16905),
            LatLngPoint(-27.443741, 153.168832),
            LatLngPoint(-27.4435, 153.1686),
        )
        assertTrue(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun pathNearOverOnlyBridgeRemainsSafe() {
        val points = listOf(
            LatLngPoint(-27.476, 153.029),
            LatLngPoint(-27.4761, 153.0291),
        )
        assertFalse(
            BridgePassageConstraint.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(overDeck),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
    }
}
