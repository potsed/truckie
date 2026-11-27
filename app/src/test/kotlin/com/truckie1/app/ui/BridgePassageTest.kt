package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.trips.ClearanceBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/bridge_passage.feature — ATNA-48.
 *
 * Passage under/over/both/unknown; height conflicts block under-paths only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BridgePassageTest {

    private val storyUnderClearance = BridgePoint(
        id = "qld-story-bridge-sample",
        name = "Story Bridge",
        latitude = -27.468,
        longitude = 153.036,
        clearanceHeightM = 3.8,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        clearanceSource = "Kerbside Lane under Bradfield Highway/Story Bridge",
        passage = BridgePassage.BOTH,
    )

    private val qrUnder = BridgePoint(
        id = "qld-qr-woolloongabba-ipswich-road",
        name = "Ipswich Road (Woolloongabba)",
        latitude = -27.496,
        longitude = 153.035,
        clearanceHeightM = 5.4,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        clearanceProvider = QueenslandRailClearances.PROVIDER_WIRE,
        passage = BridgePassage.UNDER,
    )

    private val riverOverOnly = BridgePoint(
        id = "qld-captain-cook-bridge-sample",
        name = "Captain Cook Bridge / M3",
        latitude = -27.48,
        longitude = 153.029,
        clearanceHeightM = null,
        clearanceStatus = BridgeClearanceStatus.UNKNOWN,
        passage = BridgePassage.OVER,
    )

    @Test
    fun bridgePassageFlagKeyIsFfBridgePassage() {
        assertEquals("ff_bridge_passage", FlagKeys.BRIDGE_PASSAGE)
        assertEquals(FlagKeys.BRIDGE_PASSAGE, BridgePassageFilter.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(BridgePassageFilter.isEnabled(flags))
        flags.setEnabled(FlagKeys.BRIDGE_PASSAGE, true)
        assertTrue(BridgePassageFilter.isEnabled(flags))
    }

    @Test
    fun passageWireNamesRoundTrip() {
        assertEquals("under", BridgePassage.UNDER.wireName())
        assertEquals("over", BridgePassage.OVER.wireName())
        assertEquals("both", BridgePassage.BOTH.wireName())
        assertEquals("unknown", BridgePassage.UNKNOWN.wireName())
        assertEquals(BridgePassage.UNDER, BridgePassage.fromWire("under"))
        assertEquals(BridgePassage.OVER, BridgePassage.fromWire("OVER"))
        assertEquals(BridgePassage.BOTH, BridgePassage.fromWire("both"))
        assertEquals(BridgePassage.UNKNOWN, BridgePassage.fromWire(null))
        assertEquals(BridgePassage.UNKNOWN, BridgePassage.fromWire("nope"))
    }

    @Test
    fun routingRuleAllowsOverBlocksUnderOnConflict() {
        assertTrue(
            BridgePassageFilter.ROUTING_RULE.contains("over", ignoreCase = true),
        )
        assertTrue(
            BridgePassageFilter.ROUTING_RULE.contains("under", ignoreCase = true),
        )
        // Deck path is never blocked by under clearance.
        assertFalse(
            BridgePassageFilter.heightConflictBlocksPath(
                bridge = storyUnderClearance,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassage.OVER,
            ),
        )
        // Under path conflicts when clearance < height + buffer.
        assertTrue(
            BridgePassageFilter.heightConflictBlocksPath(
                bridge = storyUnderClearance,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassage.UNDER,
            ),
        )
        // Pure over-crossing with no under clearance never conflicts.
        assertFalse(
            BridgePassageFilter.heightConflictBlocksPath(
                bridge = riverOverOnly,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassage.UNDER,
            ),
        )
        assertFalse(
            BridgePassageFilter.heightConflictBlocksPath(
                bridge = riverOverOnly,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassage.OVER,
            ),
        )
    }

    @Test
    fun restrictsUnderPassageForUnderAndBothOnly() {
        assertTrue(BridgePassageFilter.restrictsUnderPassage(qrUnder))
        assertTrue(BridgePassageFilter.restrictsUnderPassage(storyUnderClearance))
        assertFalse(BridgePassageFilter.restrictsUnderPassage(riverOverOnly))
        assertFalse(
            BridgePassageFilter.restrictsUnderPassage(
                riverOverOnly.copy(passage = BridgePassage.UNKNOWN),
            ),
        )
    }

    @Test
    fun flagOffLeavesListUnchanged() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.BRIDGE_MARKERS to true))
        val input = listOf(storyUnderClearance, qrUnder, riverOverOnly)
        assertEquals(
            input,
            BridgePassageFilter.bridgesRestrictingUnder(input, flags),
        )
    }

    @Test
    fun flagOnKeepsOnlyUnderRestrictingStructures() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.BRIDGE_PASSAGE to true,
            ),
        )
        val filtered = BridgePassageFilter.bridgesRestrictingUnder(
            listOf(storyUnderClearance, qrUnder, riverOverOnly),
            flags,
        )
        assertEquals(listOf(storyUnderClearance, qrUnder), filtered)
        assertTrue(filtered.none { it.passage == BridgePassage.OVER })
    }

    @Test
    fun heightFilterUsesPassageWhenFlagOn() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER to true,
                FlagKeys.BRIDGE_PASSAGE to true,
            ),
        )
        // Over-only with a fake height must not become a conflict via invention —
        // passage over means deck path; under clearance absent.
        val fakeOverWithHeight = riverOverOnly.copy(
            clearanceHeightM = 3.0,
            clearanceStatus = BridgeClearanceStatus.VERIFIED,
            passage = BridgePassage.OVER,
        )
        val conflicts = VehicleHeightBridgeFilter.bridgesAffectingVehicle(
            bridges = listOf(storyUnderClearance, fakeOverWithHeight, qrUnder),
            flags = flags,
            vehicleHeightM = 4.0,
            bufferM = ClearanceBuffer.DEFAULT_M,
        )
        assertTrue(conflicts.any { it.id == storyUnderClearance.id })
        assertTrue(conflicts.none { it.id == fakeOverWithHeight.id })
        // QR 5.4 m clears 4.0 + 0.15 — not a conflict.
        assertTrue(conflicts.none { it.id == qrUnder.id })
    }

    @Test
    fun packInfersPassageForKnownProvidersAndCbdSamples() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        assertTrue(bridges.isNotEmpty())

        val qr = bridges.filter { QueenslandRailClearances.isQueenslandRail(it) }
        assertTrue(qr.size >= 100)
        qr.forEach {
            assertEquals("${it.id} should be under", BridgePassage.UNDER, it.passage)
        }

        val story = bridges.first { it.id.contains("story-bridge", ignoreCase = true) }
        val victoria = bridges.first { it.id.contains("victoria-bridge", ignoreCase = true) }
        assertEquals(BridgePassage.BOTH, story.passage)
        assertEquals(BridgePassage.BOTH, victoria.passage)

        listOf("captain-cook", "william-jolly", "go-between").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertEquals(BridgePassage.OVER, bridge.passage)
            assertTrue(bridge.clearanceHeightM == null)
        }

        val tmr = bridges.filter { TmrStructureClearances.isTmr(it) }
        assertTrue(tmr.isNotEmpty())
        tmr.forEach {
            assertEquals("${it.id} TMR under", BridgePassage.UNDER, it.passage)
        }

        val bccGis = bridges.filter { BccRoadHeightRestrictions.isBccGis(it) }
        assertTrue(bccGis.isNotEmpty())
        bccGis.forEach {
            assertEquals("${it.id} BCC GIS under", BridgePassage.UNDER, it.passage)
        }
    }

    @Test
    fun parseJsonReadsExplicitPassageAndDoesNotInventHeights() {
        val raw = """
            [
              {
                "id": "test-over",
                "name": "Over Only",
                "lat": -27.47,
                "lng": 153.02,
                "clearanceStatus": "unknown",
                "passage": "over"
              },
              {
                "id": "test-under",
                "name": "Under Only",
                "lat": -27.48,
                "lng": 153.03,
                "clearanceHeightM": 3.5,
                "clearanceStatus": "verified",
                "passage": "under"
              }
            ]
        """.trimIndent()
        val parsed = BridgeMarkersCatalog.parseJson(raw)
        assertEquals(2, parsed.size)
        assertEquals(BridgePassage.OVER, parsed[0].passage)
        assertEquals(null, parsed[0].clearanceHeightM)
        assertEquals(BridgePassage.UNDER, parsed[1].passage)
        assertEquals(3.5, parsed[1].clearanceHeightM!!, 1e-9)
    }

    @Test
    fun resolveInfersWithoutInventingClearanceHeights() {
        assertEquals(
            BridgePassage.UNDER,
            BridgePassageInference.resolve(
                id = "qld-qr-x",
                clearanceProvider = QueenslandRailClearances.PROVIDER_WIRE,
                clearanceHeightM = 4.0,
                clearanceStatus = BridgeClearanceStatus.VERIFIED,
                explicit = null,
            ),
        )
        assertEquals(
            BridgePassage.BOTH,
            BridgePassageInference.resolve(
                id = "qld-story-bridge-sample",
                clearanceProvider = null,
                clearanceHeightM = 3.8,
                clearanceStatus = BridgeClearanceStatus.VERIFIED,
                explicit = null,
            ),
        )
        assertEquals(
            BridgePassage.OVER,
            BridgePassageInference.resolve(
                id = "qld-captain-cook-bridge-sample",
                clearanceProvider = null,
                clearanceHeightM = null,
                clearanceStatus = BridgeClearanceStatus.UNKNOWN,
                explicit = null,
            ),
        )
        // Explicit pack value wins.
        assertEquals(
            BridgePassage.BOTH,
            BridgePassageInference.resolve(
                id = "qld-qr-x",
                clearanceProvider = QueenslandRailClearances.PROVIDER_WIRE,
                clearanceHeightM = 4.0,
                clearanceStatus = BridgeClearanceStatus.VERIFIED,
                explicit = BridgePassage.BOTH,
            ),
        )
    }
}
