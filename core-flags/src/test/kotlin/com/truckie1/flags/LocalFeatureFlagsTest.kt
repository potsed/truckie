package com.truckie1.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFeatureFlagsTest {
    @Test
    fun defaultsOff() {
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VEHICLE_PROFILES))
        assertFalse(flags.isEnabled(FlagKeys.MULTI_STOP))
        assertFalse(flags.isEnabled(FlagKeys.STOP_CARGO))
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
    }

    @Test
    fun canEnableFlag() {
        val flags = LocalFeatureFlags()
        flags.setEnabled(FlagKeys.TOLLS, true)
        assertTrue(flags.isEnabled(FlagKeys.TOLLS))
    }

    @Test
    fun routeOptimizeFlagKey() {
        assertTrue(FlagKeys.ROUTE_OPTIMIZE == "ff_route_optimize")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ROUTE_OPTIMIZE))
        flags.setEnabled(FlagKeys.ROUTE_OPTIMIZE, true)
        assertTrue(flags.isEnabled(FlagKeys.ROUTE_OPTIMIZE))
    }

    @Test
    fun navChromeFlagKey() {
        assertTrue(FlagKeys.NAV_CHROME == "ff_nav_chrome")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NAV_CHROME))
        flags.setEnabled(FlagKeys.NAV_CHROME, true)
        assertTrue(flags.isEnabled(FlagKeys.NAV_CHROME))
    }

    @Test
    fun mapHomeFlagKey() {
        assertTrue(FlagKeys.MAP_HOME == "ff_map_home")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_HOME))
        flags.setEnabled(FlagKeys.MAP_HOME, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_HOME))
    }

    @Test
    fun addressAutocompleteFlagKey() {
        assertTrue(FlagKeys.ADDRESS_AUTOCOMPLETE == "ff_address_autocomplete")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE))
        flags.setEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE, true)
        assertTrue(flags.isEnabled(FlagKeys.ADDRESS_AUTOCOMPLETE))
    }

    @Test
    fun mapLocationPuckFlagKey() {
        assertTrue(FlagKeys.MAP_LOCATION_PUCK == "ff_map_location_puck")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
        flags.setEnabled(FlagKeys.MAP_LOCATION_PUCK, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_LOCATION_PUCK))
    }

    @Test
    fun routeStartEndFlagKey() {
        assertTrue(FlagKeys.ROUTE_START_END == "ff_route_start_end")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ROUTE_START_END))
        flags.setEnabled(FlagKeys.ROUTE_START_END, true)
        assertTrue(flags.isEnabled(FlagKeys.ROUTE_START_END))
    }

    @Test
    fun navigateFlagKey() {
        assertTrue(FlagKeys.NAVIGATE == "ff_navigate")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NAVIGATE))
        flags.setEnabled(FlagKeys.NAVIGATE, true)
        assertTrue(flags.isEnabled(FlagKeys.NAVIGATE))
    }

    @Test
    fun bridgeMarkersFlagKey() {
        assertTrue(FlagKeys.BRIDGE_MARKERS == "ff_bridge_markers")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.BRIDGE_MARKERS))
        flags.setEnabled(FlagKeys.BRIDGE_MARKERS, true)
        assertTrue(flags.isEnabled(FlagKeys.BRIDGE_MARKERS))
    }

    @Test
    fun demoModeFlagKey() {
        assertTrue(FlagKeys.DEMO_MODE == "ff_demo_mode")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.DEMO_MODE))
        flags.setEnabled(FlagKeys.DEMO_MODE, true)
        assertTrue(flags.isEnabled(FlagKeys.DEMO_MODE))
    }

    @Test
    fun verifiedCbdClearancesFlagKey() {
        assertTrue(FlagKeys.VERIFIED_CBD_CLEARANCES == "ff_verified_cbd_clearances")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES))
        flags.setEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES, true)
        assertTrue(flags.isEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES))
    }

    @Test
    fun queenslandRailClearancesFlagKey() {
        assertTrue(FlagKeys.QUEENSLAND_RAIL_CLEARANCES == "ff_queensland_rail_clearances")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES))
        flags.setEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES, true)
        assertTrue(flags.isEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES))
    }

    @Test
    fun w58LowOnlyFlagKey() {
        assertTrue(FlagKeys.W5_8_LOW_ONLY == "ff_w5_8_low_only")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.W5_8_LOW_ONLY))
        flags.setEnabled(FlagKeys.W5_8_LOW_ONLY, true)
        assertTrue(flags.isEnabled(FlagKeys.W5_8_LOW_ONLY))
    }

    @Test
    fun tmrStructureClearancesFlagKey() {
        assertTrue(FlagKeys.TMR_STRUCTURE_CLEARANCES == "ff_tmr_structure_clearances")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES))
        flags.setEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES, true)
        assertTrue(flags.isEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES))
    }

    @Test
    fun bccRoadHeightRestrictionsFlagKey() {
        assertTrue(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS == "ff_bcc_road_height_restrictions")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS))
        flags.setEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS, true)
        assertTrue(flags.isEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS))
    }

    @Test
    fun vehicleHeightBridgeFilterFlagKey() {
        assertTrue(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER == "ff_vehicle_height_bridge_filter")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER))
        flags.setEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER, true)
        assertTrue(flags.isEnabled(FlagKeys.VEHICLE_HEIGHT_BRIDGE_FILTER))
    }

    @Test
    fun bridgePassageFlagKey() {
        assertTrue(FlagKeys.BRIDGE_PASSAGE == "ff_bridge_passage")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.BRIDGE_PASSAGE))
        flags.setEnabled(FlagKeys.BRIDGE_PASSAGE, true)
        assertTrue(flags.isEnabled(FlagKeys.BRIDGE_PASSAGE))
    }

    @Test
    fun auLowBridgesPackFlagKey() {
        assertTrue(FlagKeys.AU_LOW_BRIDGES_PACK == "ff_au_low_bridges_pack")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.AU_LOW_BRIDGES_PACK))
        flags.setEnabled(FlagKeys.AU_LOW_BRIDGES_PACK, true)
        assertTrue(flags.isEnabled(FlagKeys.AU_LOW_BRIDGES_PACK))
    }

    @Test
    fun mapHeadingUpFlagKey() {
        assertTrue(FlagKeys.MAP_HEADING_UP == "ff_map_heading_up")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_HEADING_UP))
        flags.setEnabled(FlagKeys.MAP_HEADING_UP, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_HEADING_UP))
    }

    @Test
    fun graphhopperLiveFlagKey() {
        assertTrue(FlagKeys.GRAPHHOPPER_LIVE == "ff_graphhopper_live")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.GRAPHHOPPER_LIVE))
        flags.setEnabled(FlagKeys.GRAPHHOPPER_LIVE, true)
        assertTrue(flags.isEnabled(FlagKeys.GRAPHHOPPER_LIVE))
    }
}
