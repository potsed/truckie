package com.truckie1.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/bridge_passage_pipeline.feature — shared policy (ATNA-73).
 *
 * One BridgePassagePolicy object owns under/over conflict semantics for map
 * filters and MultiStopTourComposer / BridgePassageConstraint.
 */
class BridgePassagePolicyTest {

    private val underLow = RoutingBridgeConstraint(
        id = "qr-colina",
        latitude = -27.443741,
        longitude = 153.168832,
        clearanceHeightM = 1.9,
        passage = BridgePassageRelation.UNDER,
    )

    private val overDeck = RoutingBridgeConstraint(
        id = "captain-cook",
        latitude = -27.476,
        longitude = 153.029,
        clearanceHeightM = 1.9,
        passage = BridgePassageRelation.OVER,
    )

    private val bothStory = RoutingBridgeConstraint(
        id = "story",
        latitude = -27.468,
        longitude = 153.036,
        clearanceHeightM = 3.8,
        passage = BridgePassageRelation.BOTH,
    )

    @Test
    fun routingRuleDocumentsOverOkUnderBlocked() {
        assertTrue(BridgePassagePolicy.ROUTING_RULE.contains("over", ignoreCase = true))
        assertTrue(BridgePassagePolicy.ROUTING_RULE.contains("under", ignoreCase = true))
    }

    @Test
    fun passageWireNamesRoundTrip() {
        assertEquals("under", BridgePassageRelation.UNDER.wireName())
        assertEquals("over", BridgePassageRelation.OVER.wireName())
        assertEquals("both", BridgePassageRelation.BOTH.wireName())
        assertEquals("unknown", BridgePassageRelation.UNKNOWN.wireName())
        assertEquals(BridgePassageRelation.UNDER, BridgePassageRelation.fromWire("under"))
        assertEquals(BridgePassageRelation.OVER, BridgePassageRelation.fromWire("OVER"))
        assertEquals(BridgePassageRelation.BOTH, BridgePassageRelation.fromWire("both"))
        assertEquals(BridgePassageRelation.UNKNOWN, BridgePassageRelation.fromWire(null))
        assertEquals(BridgePassageRelation.UNKNOWN, BridgePassageRelation.fromWire("nope"))
    }

    @Test
    fun restrictsUnderForUnderAndBothOnly() {
        assertTrue(BridgePassagePolicy.restrictsUnder(BridgePassageRelation.UNDER))
        assertTrue(BridgePassagePolicy.restrictsUnder(BridgePassageRelation.BOTH))
        assertFalse(BridgePassagePolicy.restrictsUnder(BridgePassageRelation.OVER))
        assertFalse(BridgePassagePolicy.restrictsUnder(BridgePassageRelation.UNKNOWN))
    }

    @Test
    fun heightConflictBlocksUnderNotOver() {
        assertTrue(
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = bothStory.clearanceHeightM,
                structurePassage = bothStory.passage,
                vehicleHeightM = 4.0,
                bufferM = 0.15,
                pathRelation = BridgePassageRelation.UNDER,
            ),
        )
        assertFalse(
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = bothStory.clearanceHeightM,
                structurePassage = bothStory.passage,
                vehicleHeightM = 4.0,
                bufferM = 0.15,
                pathRelation = BridgePassageRelation.OVER,
            ),
        )
        assertFalse(
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = overDeck.clearanceHeightM,
                structurePassage = overDeck.passage,
                vehicleHeightM = 4.0,
                bufferM = 0.15,
                pathRelation = BridgePassageRelation.UNDER,
            ),
        )
    }

    @Test
    fun missingClearanceNeverInventedAsConflict() {
        assertFalse(
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = null,
                structurePassage = BridgePassageRelation.UNDER,
                vehicleHeightM = 4.0,
                bufferM = 0.15,
                pathRelation = BridgePassageRelation.UNDER,
            ),
        )
    }

    @Test
    fun constraintFacadeDelegatesToSharedPolicy() {
        assertEquals(BridgePassagePolicy.ROUTING_RULE, BridgePassageConstraint.ROUTING_RULE)
        assertEquals(BridgePassagePolicy.NEAR_PATH_M, BridgePassageConstraint.NEAR_PATH_M, 0.0)

        val underBlocks = BridgePassagePolicy.heightConflictBlocksPath(
            clearanceHeightM = underLow.clearanceHeightM,
            structurePassage = underLow.passage,
            vehicleHeightM = 3.5,
            bufferM = 0.15,
            pathRelation = BridgePassageRelation.UNDER,
        )
        assertEquals(
            underBlocks,
            BridgePassageConstraint.heightConflictBlocksPath(
                bridge = underLow,
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                pathRelation = BridgePassageRelation.UNDER,
            ),
        )
        assertEquals(
            BridgePassagePolicy.bridgesRestrictingUnder(listOf(underLow, overDeck)).map { it.id },
            BridgePassageConstraint.bridgesRestrictingUnder(listOf(underLow, overDeck)).map { it.id },
        )
    }

    @Test
    fun pathNearUnderLowBridgeIsUnsafe() {
        val points = listOf(
            LatLngPoint(-27.44395, 153.16905),
            LatLngPoint(-27.443741, 153.168832),
            LatLngPoint(-27.4435, 153.1686),
        )
        assertTrue(
            BridgePassagePolicy.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun intentionalStopNearUnderBridgeDoesNotConflict() {
        // Demo Wynnum delivery sits ~32 m from Colina St — must remain deliverable.
        val stop = LatLngPoint(-27.44395, 153.16905)
        val points = listOf(
            LatLngPoint(-27.4502, 153.1295),
            stop,
        )
        assertFalse(
            BridgePassagePolicy.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                allowedNearPoints = listOf(stop),
            ),
        )
        // Without the stop allow-list, the same points still conflict.
        assertTrue(
            BridgePassagePolicy.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(underLow),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun midPathUnderBridgeStillConflictsWhenStopsAreAllowed() {
        val depot = LatLngPoint(-27.42, 153.10)
        val near = LatLngPoint(-27.43, 153.11)
        val midUnder = RoutingBridgeConstraint(
            id = "mid-under",
            latitude = -27.425,
            longitude = 153.105,
            clearanceHeightM = 1.9,
            passage = BridgePassageRelation.UNDER,
        )
        val points = listOf(
            depot,
            LatLngPoint(-27.425, 153.105),
            near,
        )
        assertTrue(
            BridgePassagePolicy.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(midUnder),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
                allowedNearPoints = listOf(depot, near),
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
            BridgePassagePolicy.pathConflictsWithUnderBridges(
                points = points,
                bridges = listOf(overDeck),
                vehicleHeightM = 3.5,
                bufferM = 0.15,
            ),
        )
    }

    @Test
    fun routingBridgePassageIsAliasOfSharedRelation() {
        // Twin enum deleted: RoutingBridgePassage is the shared relation type.
        assertTrue(RoutingBridgePassage.UNDER === BridgePassageRelation.UNDER)
        assertTrue(RoutingBridgePassage.OVER === BridgePassageRelation.OVER)
    }
}
