package com.truckie1.app.routing

import com.truckie1.app.ui.BridgePassage
import com.truckie1.app.ui.BridgePoint
import com.truckie1.routing.RoutingBridgePassage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation.feature —
 * pack bridges → RoutingBridgeConstraint for MultiStopTourComposer (ATNA-71).
 * Does not edit BridgePassage UI twins (sibling worker).
 */
class RoutingBridgePackMapperTest {

    @Test
    fun mapsPackRowsToRoutingConstraints() {
        val pack = listOf(
            BridgePoint(
                id = "under-1",
                name = "Low under",
                latitude = -27.42,
                longitude = 153.10,
                clearanceHeightM = 3.2,
                passage = BridgePassage.UNDER,
            ),
            BridgePoint(
                id = "over-1",
                name = "Deck",
                latitude = -27.43,
                longitude = 153.11,
                clearanceHeightM = 3.0,
                passage = BridgePassage.OVER,
            ),
        )
        val mapped = RoutingBridgePackMapper.toRoutingConstraints(pack)
        assertEquals(2, mapped.size)
        assertEquals("under-1", mapped[0].id)
        assertEquals(RoutingBridgePassage.UNDER, mapped[0].passage)
        assertEquals(3.2, mapped[0].clearanceHeightM!!, 0.0)
        assertEquals(RoutingBridgePassage.OVER, mapped[1].passage)
    }

    @Test
    fun whenPassageFlagOffComposerBridgesAreEmpty() {
        val pack = listOf(
            BridgePoint(
                id = "under-1",
                name = "Low under",
                latitude = -27.42,
                longitude = 153.10,
                clearanceHeightM = 3.2,
                passage = BridgePassage.UNDER,
            ),
        )
        assertTrue(
            RoutingBridgePackMapper.bridgesForComposer(
                packBridges = pack,
                bridgePassageEnabled = false,
            ).isEmpty(),
        )
        assertEquals(
            1,
            RoutingBridgePackMapper.bridgesForComposer(
                packBridges = pack,
                bridgePassageEnabled = true,
            ).size,
        )
    }
}
