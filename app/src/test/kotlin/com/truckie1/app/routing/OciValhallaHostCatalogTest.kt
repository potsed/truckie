package com.truckie1.app.routing

import com.truckie1.flags.FlagKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/oci_valhalla_host.feature — OCI Valhalla host catalog. */
class OciValhallaHostCatalogTest {

    @Test
    fun flagAndStackPathsMatchRunbook() {
        assertEquals(FlagKeys.VALHALLA_LIVE, OciValhallaHostCatalog.FLAG_KEY)
        assertEquals("ff_valhalla_live", OciValhallaHostCatalog.FLAG_KEY)
        assertEquals("infra/oci-valhalla", OciValhallaHostCatalog.STACK_PATH)
        assertEquals("/opt/oci-valhalla", OciValhallaHostCatalog.INSTALL_ROOT)
        assertEquals("oci-valhalla", OciValhallaHostCatalog.COMPOSE_PROJECT)
        assertEquals(8002, OciValhallaHostCatalog.VALHALLA_LOOPBACK_PORT)
        assertTrue(
            OciValhallaHostCatalog.VALHALLA_IMAGE.contains("valhalla-scripted"),
        )
        assertFalse(OciValhallaHostCatalog.GRAPHHOPPER_STACK_REVIVED)
        assertFalse(OciValhallaHostCatalog.SELF_HOST_REQUIRES_API_KEY)
    }

    @Test
    fun qldFirstSeqIsFreeTierDogfoodSlice() {
        assertTrue(OciValhallaHostCatalog.QLD_FIRST_DEFAULT)
        assertFalse(OciValhallaHostCatalog.FULL_AU_FITS_FREE_TIER)
        assertEquals("152.0,-28.55,153.65,-26.35", OciValhallaHostCatalog.SEQ_BBOX)
        assertTrue(
            OciValhallaHostCatalog.GEOFABRIK_AU_PBF_URL.contains("australia-latest.osm.pbf"),
        )
        assertEquals("VM.Standard.A1.Flex", OciValhallaHostCatalog.FREE_TIER_SHAPE)
        assertTrue(OciValhallaHostCatalog.FREE_TIER_MEMORY_GBS >= 12)
        val limits = OciValhallaHostCatalog.freeTierLimitsNotes()
        assertTrue(limits.any { it.contains("SEQ", ignoreCase = true) })
        assertTrue(limits.any { it.contains("AU", ignoreCase = true) })
    }

    @Test
    fun baseUrlHintAndBuildConfigWiringAreSelfHostOnly() {
        assertEquals(
            "https://vh.example.com",
            OciValhallaHostCatalog.baseUrlHint("vh.example.com"),
        )
        assertEquals(
            "https://vh.example.com",
            OciValhallaHostCatalog.baseUrlHint("https://vh.example.com/"),
        )
        val steps = OciValhallaHostCatalog.buildConfigWiringSteps()
        assertTrue(steps.size >= 4)
        assertTrue(steps.any { it.contains("VALHALLA_BASE_URL") })
        assertTrue(steps.any { it.contains("local.properties") || it.contains("secrets.properties") })
        assertTrue(steps.any { it.contains("VALHALLA_API_KEY") })
        assertTrue(steps.any { it.contains("ff_valhalla_live") })
    }

    @Test
    fun brisbaneSampleCurlUsesTruckCosting() {
        val curl = OciValhallaHostCatalog.brisbaneSampleCurl("https://vh.example.com")
        assertTrue(curl.contains("/route"))
        assertTrue(curl.contains("\"costing\": \"truck\""))
        assertTrue(curl.contains("${OciValhallaHostCatalog.SAMPLE_BRISBANE_FROM_LAT}"))
        assertTrue(curl.contains("${OciValhallaHostCatalog.SAMPLE_BRISBANE_FROM_LON}"))
        assertFalse(curl.contains("Authorization"))
        assertFalse(curl.contains("graphhopper", ignoreCase = true))
    }
}
