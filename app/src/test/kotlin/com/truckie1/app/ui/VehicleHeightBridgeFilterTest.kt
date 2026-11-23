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
 * Gherkin: features/vehicle_height_bridge_filter.feature — ATNA-47.
 *
 * With an active vehicle height + system buffer, home-map W5-8 symbols
 * show only structures the vehicle cannot clear.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VehicleHeightBridgeFilterTest {

    private val story = BridgePoint(
        id = "qld-bcc-story-bridge",
        name = "Story Bridge",
        latitude = -27.467,
        longitude = 153.036,
        clearanceHeightM = 3.8,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        clearanceSource = "BCC",
    )

    private val clearable = BridgePoint(
        id = "qld-test-clearable-43",
        name = "Clearable 4.3 m",
        latitude = -27.47,
        longitude = 153.02,
        clearanceHeightM = 4.3,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        clearanceSource = "test",
    )

    private val marginal = BridgePoint(
        id = "qld-test-marginal-42",
        name = "Marginal 4.2 m",
        latitude = -27.471,
        longitude = 153.021,
        clearanceHeightM = 4.2,
        clearanceStatus = BridgeClearanceStatus.VERIFIED,
        clearanceSource = "test",
    )

    private val unknown = BridgePoint(
        id = "qld-bcc-captain-cook",
        name = "Captain Cook",
        latitude = -27.48,
        longitude = 153.03,
        clearanceHeightM = null,
        clearanceStatus = BridgeClearanceStatus.UNKNOWN,
    )

    @Test
    fun vehicleHeightBridgeFilterFlagKeyIsFfVehicleHeightBridgeFilter() {
        assertEquals("ff_vehicle_height_bridge_filter", FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER)
        assertEquals(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, VehicleHeightBridgeFilter.FLAG_KEY)
        val flags = LocalFeatureFlags()
        assertFalse(VehicleHeightBridgeFilter.isEnabled(flags))
        flags.setEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, true)
        assertTrue(VehicleHeightBridgeFilter.isEnabled(flags))
    }

    @Test
    fun usesExistingClearanceBufferDefault() {
        assertEquals(0.15, ClearanceBuffer.DEFAULT_M, 1e-9)
        assertEquals(ClearanceBuffer.DEFAULT_M, VehicleHeightBridgeFilter.DEFAULT_BUFFER_M, 1e-9)
        assertTrue(
            VehicleHeightBridgeFilter.BUFFER_RATIONALE.contains("ClearanceBuffer", ignoreCase = true) ||
                VehicleHeightBridgeFilter.BUFFER_RATIONALE.contains("0.15", ignoreCase = true),
        )
    }

    @Test
    fun conflictThresholdIsVehicleHeightPlusBuffer() {
        assertEquals(
            4.15,
            VehicleHeightBridgeFilter.conflictThresholdM(
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
            1e-9,
        )
        assertEquals(
            4.3,
            VehicleHeightBridgeFilter.conflictThresholdM(vehicleHeightM = 4.0, bufferM = 0.3),
            1e-9,
        )
    }

    @Test
    fun conflictsWhenClearanceBelowThreshold() {
        assertTrue(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = story,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertFalse(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = clearable,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertTrue(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = marginal,
                vehicleHeightM = 4.0,
                bufferM = 0.3,
            ),
        )
        assertFalse(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = clearable,
                vehicleHeightM = 4.0,
                bufferM = 0.3,
            ),
        )
    }

    @Test
    fun unknownOrMissingHeightNeverConflictsOrInventHeights() {
        assertFalse(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = unknown,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertFalse(
            VehicleHeightBridgeFilter.conflictsWithVehicle(
                bridge = story.copy(
                    clearanceHeightM = null,
                    clearanceStatus = BridgeClearanceStatus.VERIFIED,
                ),
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertFalse(LowBridgeW58.qualifiesForW58(unknown))
    }

    @Test
    fun flagOffReturnsInputUnchanged() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.W5_8_LOW_ONLY to true,
            ),
        )
        val input = listOf(story, clearable, unknown)
        assertEquals(
            input,
            VehicleHeightBridgeFilter.bridgesAffectingVehicle(
                bridges = input,
                flags = flags,
                vehicleHeightM = 4.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
    }

    @Test
    fun flagOnWithUsableHeightReturnsConflictsOnly() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.W5_8_LOW_ONLY to true,
                FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER to true,
            ),
        )
        val input = listOf(story, clearable, marginal, unknown)
        val filtered = VehicleHeightBridgeFilter.bridgesAffectingVehicle(
            bridges = input,
            flags = flags,
            vehicleHeightM = 4.0,
            bufferM = ClearanceBuffer.DEFAULT_M,
        )
        assertEquals(listOf(story), filtered)
        assertTrue(filtered.none { it.id == clearable.id })
        assertTrue(filtered.none { it.clearanceStatus == BridgeClearanceStatus.UNKNOWN })
    }

    @Test
    fun noActiveVehicleOrUnusableHeightFallsBackToAllLowMarkers() {
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.W5_8_LOW_ONLY to true,
                FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER to true,
            ),
        )
        val lowOnly = listOf(story, clearable)
        assertEquals(
            lowOnly,
            VehicleHeightBridgeFilter.bridgesAffectingVehicle(
                bridges = lowOnly,
                flags = flags,
                vehicleHeightM = null,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertEquals(
            lowOnly,
            VehicleHeightBridgeFilter.bridgesAffectingVehicle(
                bridges = lowOnly,
                flags = flags,
                vehicleHeightM = 0.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertEquals(
            lowOnly,
            VehicleHeightBridgeFilter.bridgesAffectingVehicle(
                bridges = lowOnly,
                flags = flags,
                vehicleHeightM = -1.0,
                bufferM = ClearanceBuffer.DEFAULT_M,
            ),
        )
        assertEquals(
            VehicleHeightBridgeFilter.NO_HEIGHT_FALLBACK,
            "show_all_low",
        )
    }

    @Test
    fun packPipelineKeepsW58ThenFiltersConflictsForActiveVehicle() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val flags = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.W5_8_LOW_ONLY to true,
                FlagKeys.QUEENSLAND_RAIL_CLEARANCES to true,
                FlagKeys.TMR_STRUCTURE_CLEARANCES to true,
                FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS to true,
                FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER to true,
            ),
        )
        val visible = QldOpenClearances.visibleBridges(bridges, flags)
        val low = LowBridgeW58.bridgesForW58(visible, flags)
        assertTrue(low.isNotEmpty())

        val vehicleHeightM = 4.0
        val bufferM = ClearanceBuffer.DEFAULT_M
        val threshold = vehicleHeightM + bufferM
        val conflicts = VehicleHeightBridgeFilter.bridgesAffectingVehicle(
            bridges = low,
            flags = flags,
            vehicleHeightM = vehicleHeightM,
            bufferM = bufferM,
        )
        assertTrue(conflicts.isNotEmpty())
        assertTrue(conflicts.size <= low.size)
        conflicts.forEach { bridge ->
            assertTrue(LowBridgeW58.qualifiesForW58(bridge))
            val height = bridge.clearanceHeightM
            assertTrue(height != null && height < threshold)
        }
        assertTrue(conflicts.any { it.id.contains("story-bridge", ignoreCase = true) })
        // Victoria 4.4 m clears a 4.0 + 0.15 vehicle — must not appear as a conflict.
        assertTrue(conflicts.none { it.id.contains("victoria-bridge", ignoreCase = true) })
    }

    @Test
    fun usableHeightRequiresPositiveMetres() {
        assertFalse(VehicleHeightBridgeFilter.hasUsableHeight(null))
        assertFalse(VehicleHeightBridgeFilter.hasUsableHeight(0.0))
        assertFalse(VehicleHeightBridgeFilter.hasUsableHeight(-0.1))
        assertTrue(VehicleHeightBridgeFilter.hasUsableHeight(3.5))
    }
}
