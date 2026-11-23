package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Gherkin: features/qld_tmr_bcc_clearances.feature — ATNA-46.
 *
 * Sources retrieved 2026-11-23:
 * - https://www.data.qld.gov.au/dataset/structure-clearances-queensland
 * - https://data.brisbane.qld.gov.au/explore/dataset/road-restrictions/
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QldTmrBccClearancesTest {

    @Test
    fun tmrAndBccFlagKeysAreDefined() {
        assertEquals("ff_tmr_structure_clearances", FlagKeys.TMR_STRUCTURE_CLEARANCES)
        assertEquals("ff_bcc_road_height_restrictions", FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS)
        val flags = LocalFeatureFlags()
        assertFalse(TmrStructureClearances.isEnabled(flags))
        assertFalse(BccRoadHeightRestrictions.isEnabled(flags))
        flags.setEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES, true)
        flags.setEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS, true)
        assertTrue(TmrStructureClearances.isEnabled(flags))
        assertTrue(BccRoadHeightRestrictions.isEnabled(flags))
    }

    @Test
    fun packBootstrapVersionBumpsPastQueenslandRailSampleForReseed() {
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.5.0-sample",
            ),
        )
        assertEquals("0.7.0-sample", CachedBridgePackRepository.BOOTSTRAP_VERSION)
    }

    @Test
    fun tmrFlagOffHidesTmrEntriesButKeepsCbdAndQr() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val off = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.QUEENSLAND_RAIL_CLEARANCES to true,
                FlagKeys.TMR_STRUCTURE_CLEARANCES to false,
                FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS to true,
            ),
        )
        val visible = QldOpenClearances.visibleBridges(bridges, off)
        assertTrue(visible.any { it.id.contains("story-bridge", ignoreCase = true) })
        assertTrue(visible.any { QueenslandRailClearances.isQueenslandRail(it) })
        assertTrue(visible.none { TmrStructureClearances.isTmr(it) })
    }

    @Test
    fun bccFlagOffHidesBccGisEntriesButKeepsCbdSamples() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val off = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.QUEENSLAND_RAIL_CLEARANCES to true,
                FlagKeys.TMR_STRUCTURE_CLEARANCES to true,
                FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS to false,
            ),
        )
        val visible = QldOpenClearances.visibleBridges(bridges, off)
        assertTrue(visible.any { it.id.contains("story-bridge", ignoreCase = true) })
        assertTrue(visible.none { BccRoadHeightRestrictions.isBccGis(it) })
    }

    @Test
    fun tmrFlagOnShowsVerifiedPublishedHeights() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.TMR_STRUCTURE_CLEARANCES to true,
            ),
        )
        val visible = QldOpenClearances.visibleBridges(bridges, on)
        val tmr = visible.filter { TmrStructureClearances.isTmr(it) }
        assertTrue("Expected TMR Structure Clearance entries when flag on", tmr.size >= 40)
        tmr.forEach { bridge ->
            assertEquals(BridgeClearanceStatus.VERIFIED, bridge.clearanceStatus)
            assertNotNull(bridge.clearanceHeightM)
            assertTrue(
                bridge.clearanceSource!!.contains("Structure Clearances", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("TMR", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("data.qld.gov.au", ignoreCase = true),
            )
            assertEquals(TmrStructureClearances.RETRIEVED_AT, bridge.clearanceRetrievedAt)
            assertEquals(TmrStructureClearances.PROVIDER_WIRE, bridge.clearanceProvider)
        }
    }

    @Test
    fun bccFlagOnShowsLocalRestrictionsUnder46() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS to true,
            ),
        )
        val visible = QldOpenClearances.visibleBridges(bridges, on)
        val bcc = visible.filter { BccRoadHeightRestrictions.isBccGis(it) }
        assertTrue("Expected BCC GIS road-restriction entries when flag on", bcc.size >= 10)
        bcc.forEach { bridge ->
            assertEquals(BridgeClearanceStatus.VERIFIED, bridge.clearanceStatus)
            assertNotNull(bridge.clearanceHeightM)
            assertTrue(bridge.clearanceHeightM!! <= 4.6 + 0.001)
            assertTrue(
                bridge.clearanceSource!!.contains("brisbane", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("road-restrictions", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("BCC", ignoreCase = true),
            )
            assertEquals(BccRoadHeightRestrictions.RETRIEVED_AT, bridge.clearanceRetrievedAt)
        }
    }

    @Test
    fun mergeKeepsCbdVerifiedAndQueenslandRailRows() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val story = bridges.first { it.id.contains("story-bridge", ignoreCase = true) }
        val victoria = bridges.first { it.id.contains("victoria-bridge", ignoreCase = true) }
        assertEquals(3.8, story.clearanceHeightM!!, 0.001)
        assertEquals(4.4, victoria.clearanceHeightM!!, 0.001)
        assertEquals(BridgeClearanceStatus.VERIFIED, story.clearanceStatus)
        assertEquals(BridgeClearanceStatus.VERIFIED, victoria.clearanceStatus)
        val qr = bridges.filter { QueenslandRailClearances.isQueenslandRail(it) }
        assertTrue(qr.size >= 100)
    }

    @Test
    fun packHasUniqueIdsAndNoSourceIdDuplicatesAgainstCbd() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val ids = bridges.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        val cbdSourceIds = bridges
            .filter { it.id.endsWith("-sample") }
            .mapNotNull { it.clearanceSourceId }
            .toSet()
        val bccDupes = bridges.filter {
            BccRoadHeightRestrictions.isBccGis(it) &&
                it.clearanceSourceId != null &&
                it.clearanceSourceId in cbdSourceIds
        }
        assertTrue(
            "BCC GIS must not re-add CBD clearanceSourceIds: $bccDupes",
            bccDupes.isEmpty(),
        )
    }

    @Test
    fun tmrAndBccGisAreDeDuplicatedWithin35MetresOfPack() {
        // Merge contract (ATNA-46): newly ingested TMR/BCC GIS rows must not sit
        // within ~35 m of another pack point. Existing QR page pairs that are
        // intentionally adjacent underbridges stay as published.
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val ingested = bridges.filter {
            TmrStructureClearances.isTmr(it) || BccRoadHeightRestrictions.isBccGis(it)
        }
        for (row in ingested) {
            for (other in bridges) {
                if (other.id == row.id) continue
                val metres = haversineMetres(
                    row.latitude,
                    row.longitude,
                    other.latitude,
                    other.longitude,
                )
                assertTrue(
                    "Ingested ${row.id} within 35 m of ${other.id} (${metres}m)",
                    metres >= 35.0 - 0.5,
                )
            }
        }
    }

    @Test
    fun w58OnlyForTmrBccAtOrBelow46Metres() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val tmrHigh = bridges.filter {
            TmrStructureClearances.isTmr(it) &&
                (it.clearanceHeightM ?: 0.0) > LowBridgeW58.LOW_CLEARANCE_MAX_M
        }
        tmrHigh.forEach { bridge ->
            assertFalse(
                "TMR mid-band ${bridge.id} (${bridge.clearanceHeightM} m) must not get W5-8",
                LowBridgeW58.qualifiesForW58(bridge),
            )
        }
        val tmrLow = bridges.filter {
            TmrStructureClearances.isTmr(it) &&
                it.clearanceStatus == BridgeClearanceStatus.VERIFIED &&
                (it.clearanceHeightM ?: Double.MAX_VALUE) <= LowBridgeW58.LOW_CLEARANCE_MAX_M
        }
        assertTrue(tmrLow.isNotEmpty())
        tmrLow.forEach { assertTrue(LowBridgeW58.qualifiesForW58(it)) }
        val bcc = bridges.filter { BccRoadHeightRestrictions.isBccGis(it) }
        bcc.forEach { bridge ->
            if (bridge.clearanceHeightM != null &&
                bridge.clearanceHeightM <= LowBridgeW58.LOW_CLEARANCE_MAX_M
            ) {
                assertTrue(LowBridgeW58.qualifiesForW58(bridge))
            }
        }
    }

    @Test
    fun packMetaCitesTmrAndBccSourcesWithRetrievalDate() {
        val meta = VerifiedCbdClearances.loadPackMetadata(RuntimeEnvironment.getApplication())
        assertNotNull(meta)
        val joined = meta!!.sources.joinToString("\n")
        assertTrue(joined.contains("data.qld.gov.au/dataset/structure-clearances-queensland"))
        assertTrue(
            joined.contains("data.brisbane.qld.gov.au/explore/dataset/road-restrictions"),
        )
        assertEquals("2026-11-23", meta.retrievedAt)
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.5.0-sample",
            ),
        )
    }

    @Test
    fun tmrSpotCheckPacificHwyUnderpassMatchesPublishedFigure() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val row = bridges.first {
            it.id.contains("24215") ||
                it.name.contains("Pacific Hwy/Gold Coast Hwy", ignoreCase = true)
        }
        assertEquals(4.3, row.clearanceHeightM!!, 0.001)
        assertEquals(BridgeClearanceStatus.VERIFIED, row.clearanceStatus)
        assertTrue(abs(row.latitude - (-27.925744)) < 0.001)
        assertTrue(abs(row.longitude - 153.332819) < 0.001)
        assertEquals(TmrStructureClearances.PROVIDER_WIRE, row.clearanceProvider)
    }

    @Test
    fun bccSpotCheckWalterTaylorMatchesOpenData() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val row = bridges.first {
            it.clearanceSourceId == "B0004 - 18" ||
                it.name.contains("Walter Taylor", ignoreCase = true)
        }
        assertEquals(4.1, row.clearanceHeightM!!, 0.001)
        assertEquals(BridgeClearanceStatus.VERIFIED, row.clearanceStatus)
        assertTrue(BccRoadHeightRestrictions.isBccGis(row) || row.id.contains("walter", true))
    }

    @Test
    fun clearancePolicyDocumentsSignageOverrideAndBufferForDataLag() {
        val meta = VerifiedCbdClearances.loadPackMetadata(RuntimeEnvironment.getApplication())
        assertNotNull(meta)
        val policy = meta!!.clearancePolicy
        assertTrue(
            policy.contains("sign", ignoreCase = true) ||
                policy.contains("roadside", ignoreCase = true),
        )
        assertTrue(
            policy.contains("buffer", ignoreCase = true) ||
                policy.contains("lag", ignoreCase = true),
        )
    }

    private fun haversineMetres(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2 * r * kotlin.math.asin(sqrt(a))
    }
}
