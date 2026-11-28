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
        assertFalse(flags.isEnabled(FlagKeys.OFFLINE_MAPS))
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
        assertFalse(flags.isEnabled(FlagKeys.NHVR_OVERLAY))
    }

    @Test
    fun offlineMapsFlagKey() {
        assertTrue(FlagKeys.OFFLINE_MAPS == "ff_offline_maps")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.OFFLINE_MAPS))
        flags.setEnabled(FlagKeys.OFFLINE_MAPS, true)
        assertTrue(flags.isEnabled(FlagKeys.OFFLINE_MAPS))
    }

    @Test
    fun androidAutoFlagKey() {
        assertTrue(FlagKeys.ANDROID_AUTO == "ff_android_auto")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ANDROID_AUTO))
        flags.setEnabled(FlagKeys.ANDROID_AUTO, true)
        assertTrue(flags.isEnabled(FlagKeys.ANDROID_AUTO))
    }

    @Test
    fun nhvrOverlayFlagKey() {
        assertTrue(FlagKeys.NHVR_OVERLAY == "ff_nhvr_overlay")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NHVR_OVERLAY))
        flags.setEnabled(FlagKeys.NHVR_OVERLAY, true)
        assertTrue(flags.isEnabled(FlagKeys.NHVR_OVERLAY))
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
    fun gnafAddressGeocodeFlagKey() {
        assertTrue(FlagKeys.GNAF_ADDRESS_GEOCODE == "ff_gnaf_address_geocode")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.GNAF_ADDRESS_GEOCODE))
        flags.setEnabled(FlagKeys.GNAF_ADDRESS_GEOCODE, true)
        assertTrue(flags.isEnabled(FlagKeys.GNAF_ADDRESS_GEOCODE))
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
    fun stopMarkersFlagKey() {
        assertTrue(FlagKeys.STOP_MARKERS == "ff_stop_markers")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.STOP_MARKERS))
        flags.setEnabled(FlagKeys.STOP_MARKERS, true)
        assertTrue(flags.isEnabled(FlagKeys.STOP_MARKERS))
    }

    @Test
    fun mapRoutePolylineFlagKey() {
        assertTrue(FlagKeys.MAP_ROUTE_POLYLINE == "ff_map_route_polyline")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE))
        flags.setEnabled(FlagKeys.MAP_ROUTE_POLYLINE, true)
        assertTrue(flags.isEnabled(FlagKeys.MAP_ROUTE_POLYLINE))
    }

    @Test
    fun nextDestinationDisplayFlagKey() {
        assertTrue(FlagKeys.NEXT_DESTINATION_DISPLAY == "ff_next_destination_display")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY))
        flags.setEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY, true)
        assertTrue(flags.isEnabled(FlagKeys.NEXT_DESTINATION_DISPLAY))
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
    fun valhallaLiveFlagKey() {
        assertTrue(FlagKeys.VALHALLA_LIVE == "ff_valhalla_live")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.VALHALLA_LIVE))
        flags.setEnabled(FlagKeys.VALHALLA_LIVE, true)
        assertTrue(flags.isEnabled(FlagKeys.VALHALLA_LIVE))
    }

    @Test
    fun activeTourFlagKey() {
        assertTrue(FlagKeys.ACTIVE_TOUR == "ff_active_tour")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ACTIVE_TOUR))
        flags.setEnabled(FlagKeys.ACTIVE_TOUR, true)
        assertTrue(flags.isEnabled(FlagKeys.ACTIVE_TOUR))
    }

    @Test
    fun navigateMultiStopFlagKey() {
        assertTrue(FlagKeys.NAVIGATE_MULTI_STOP == "ff_navigate_multi_stop")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.NAVIGATE_MULTI_STOP))
        flags.setEnabled(FlagKeys.NAVIGATE_MULTI_STOP, true)
        assertTrue(flags.isEnabled(FlagKeys.NAVIGATE_MULTI_STOP))
    }

    @Test
    fun routePathFromLocationFlagKey() {
        assertTrue(FlagKeys.ROUTE_PATH_FROM_LOCATION == "ff_route_path_from_location")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION))
        flags.setEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION, true)
        assertTrue(flags.isEnabled(FlagKeys.ROUTE_PATH_FROM_LOCATION))
    }

    @Test
    fun asyncRoutingIoFlagKey() {
        assertTrue(FlagKeys.ASYNC_ROUTING_IO == "ff_async_routing_io")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO))
        flags.setEnabled(FlagKeys.ASYNC_ROUTING_IO, true)
        assertTrue(flags.isEnabled(FlagKeys.ASYNC_ROUTING_IO))
    }

    @Test
    fun roomRoutePersistFlagKey() {
        assertTrue(FlagKeys.ROOM_ROUTE_PERSIST == "ff_room_route_persist")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.ROOM_ROUTE_PERSIST))
        flags.setEnabled(FlagKeys.ROOM_ROUTE_PERSIST, true)
        assertTrue(flags.isEnabled(FlagKeys.ROOM_ROUTE_PERSIST))
    }

    @Test
    fun pathBlockDetourFlagKey() {
        assertTrue(FlagKeys.PATH_BLOCK_DETOUR == "ff_path_block_detour")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.PATH_BLOCK_DETOUR))
        flags.setEnabled(FlagKeys.PATH_BLOCK_DETOUR, true)
        assertTrue(flags.isEnabled(FlagKeys.PATH_BLOCK_DETOUR))
    }

    @Test
    fun ownedAddressPackFlagKey() {
        assertTrue(FlagKeys.OWNED_ADDRESS_PACK == "ff_owned_address_pack")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.OWNED_ADDRESS_PACK))
        flags.setEnabled(FlagKeys.OWNED_ADDRESS_PACK, true)
        assertTrue(flags.isEnabled(FlagKeys.OWNED_ADDRESS_PACK))
    }

    @Test
    fun settingsDisclaimerFlagKey() {
        assertTrue(FlagKeys.SETTINGS_DISCLAIMER == "ff_settings_disclaimer")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.SETTINGS_DISCLAIMER))
        flags.setEnabled(FlagKeys.SETTINGS_DISCLAIMER, true)
        assertTrue(flags.isEnabled(FlagKeys.SETTINGS_DISCLAIMER))
    }

    @Test
    fun licensesPageFlagKey() {
        assertTrue(FlagKeys.LICENSES_PAGE == "ff_licenses_page")
        val flags = LocalFeatureFlags()
        assertFalse(flags.isEnabled(FlagKeys.LICENSES_PAGE))
        flags.setEnabled(FlagKeys.LICENSES_PAGE, true)
        assertTrue(flags.isEnabled(FlagKeys.LICENSES_PAGE))
    }
}
