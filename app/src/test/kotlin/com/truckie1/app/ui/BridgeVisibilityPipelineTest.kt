package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.routing.BridgePassagePolicy
import com.truckie1.routing.BridgePassageRelation
import com.truckie1.trips.ClearanceBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/bridge_passage_pipeline.feature — visibility pipeline (ATNA-73).
 *
 * One ordered BridgeVisibilityPipeline for Compose home-map and style-layer bind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BridgeVisibilityPipelineTest {

    private val storyBoth = BridgePoint(
        id = "qld-story-bridge-sample",
        name = "Story Bridge",
        latitude = -27.468,
        longitude = 153.036,
        clearanceHeightM = 3.8,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        passage = BridgePassage.BOTH,
    )

    private val captainCookOver = BridgePoint(
        id = "qld-captain-cook-bridge-sample",
        name = "Captain Cook Bridge / M3",
        latitude = -27.48,
        longitude = 153.029,
        clearanceHeightM = null,
        clearanceStatus = BridgeClearanceStatus.UNKNOWN,
        passage = BridgePassage.OVER,
    )

    private val highSpan = BridgePoint(
        id = "qld-high-open-span",
        name = "High open span",
        latitude = -27.47,
        longitude = 153.03,
        clearanceHeightM = 6.0,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        passage = BridgePassage.UNDER,
    )

    @Test
    fun stageOrderIsQldOpenThenW58ThenPassageThenVehicleHeight() {
        assertEquals(
            listOf("qld_open", "w5_8_low", "passage_under", "vehicle_height"),
            BridgeVisibilityPipeline.STAGE_ORDER,
        )
        assertTrue(BridgeVisibilityPipeline.singlePipelineOwnsMapFilters)
        assertTrue(BridgeVisibilityPipeline.bindReceivesPreFilteredList)
    }

    @Test
    fun filterAppliesOrderedStagesWithFlagsOn() {
        val flags = LocalFeatureFlags().apply {
            setEnabled(FlagKeys.BRIDGE_MARKERS, true)
            setEnabled(FlagKeys.W5_8_LOW_ONLY, true)
            setEnabled(FlagKeys.BRIDGE_PASSAGE, true)
            setEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, true)
        }
        val result = BridgeVisibilityPipeline.filter(
            BridgeVisibilityPipeline.Input(
                bridges = listOf(storyBoth, captainCookOver, highSpan),
                flags = flags,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        // Over-only dropped by passage; high span cleared by vehicle height; story conflicts.
        assertEquals(listOf("qld-story-bridge-sample"), result.map { it.id })
    }

    @Test
    fun passageStagePassthroughWhenFlagOff() {
        val flags = LocalFeatureFlags().apply {
            setEnabled(FlagKeys.W5_8_LOW_ONLY, false)
            setEnabled(FlagKeys.BRIDGE_PASSAGE, false)
            setEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, false)
        }
        val input = listOf(storyBoth, captainCookOver)
        val result = BridgeVisibilityPipeline.filter(
            BridgeVisibilityPipeline.Input(
                bridges = input,
                flags = flags,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertEquals(input.map { it.id }, result.map { it.id })
    }

    @Test
    fun forStyleBindDoesNotReFilter() {
        val preFiltered = listOf(storyBoth)
        assertSame(preFiltered, BridgeVisibilityPipeline.forStyleBind(preFiltered))
    }

    @Test
    fun mapFilterDelegatesConflictSemanticsToSharedPolicy() {
        assertEquals(BridgePassagePolicy.ROUTING_RULE, BridgePassageFilter.ROUTING_RULE)
        val blocksUnder = BridgePassageFilter.heightConflictBlocksPath(
            bridge = storyBoth,
            vehicleHeightM = 4.0,
            bufferM = ClearanceBuffer.DEFAULT_M,
            pathRelation = BridgePassage.UNDER,
        )
        assertEquals(
            blocksUnder,
            BridgePassagePolicy.heightConflictBlocksPath(
                clearanceHeightM = storyBoth.clearanceHeightM,
                structurePassage = BridgePassageRelation.BOTH,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassageRelation.UNDER,
            ),
        )
        assertFalse(
            BridgePassageFilter.heightConflictBlocksPath(
                bridge = storyBoth,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
                pathRelation = BridgePassage.OVER,
            ),
        )
    }

    @Test
    fun appPassageIsSharedRelationType() {
        assertTrue(BridgePassage.UNDER === BridgePassageRelation.UNDER)
        assertTrue(BridgePassage.OVER === BridgePassageRelation.OVER)
        assertTrue(BridgePassage.BOTH === BridgePassageRelation.BOTH)
        assertTrue(BridgePassage.UNKNOWN === BridgePassageRelation.UNKNOWN)
    }
}
