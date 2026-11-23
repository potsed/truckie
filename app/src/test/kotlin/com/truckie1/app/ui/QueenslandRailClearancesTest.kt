package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Gherkin: features/queensland_rail_clearances.feature — ATNA-44.
 *
 * Authoritative source:
 * https://www.queenslandrail.com.au/forcustomers/safetysecurity/railbridges
 * retrieved 2026-11-23. Do not invent heights for bridges absent from that page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class QueenslandRailClearancesTest {

    @Test
    fun queenslandRailClearancesFlagKeyIsFfQueenslandRailClearances() {
        assertEquals("ff_queensland_rail_clearances", FlagKeys.QUEENSLAND_RAIL_CLEARANCES)
        val flags = LocalFeatureFlags()
        assertFalse(QueenslandRailClearances.isEnabled(flags))
        flags.setEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES, true)
        assertTrue(QueenslandRailClearances.isEnabled(flags))
    }

    @Test
    fun packBootstrapVersionBumpsPastVerifiedCbdSampleForReseed() {
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.4.0-sample",
            ),
        )
        assertEquals("0.5.0-sample", CachedBridgePackRepository.BOOTSTRAP_VERSION)
    }

    @Test
    fun qrFlagOffHidesQueenslandRailEntriesButKeepsCbd() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val off = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.QUEENSLAND_RAIL_CLEARANCES to false,
            ),
        )
        val visible = QueenslandRailClearances.visibleBridges(bridges, off)
        assertTrue(visible.any { it.id.contains("story-bridge", ignoreCase = true) })
        assertTrue(visible.none { QueenslandRailClearances.isQueenslandRail(it) })
    }

    @Test
    fun qrFlagOnShowsQueenslandRailVerifiedHeights() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.QUEENSLAND_RAIL_CLEARANCES to true,
            ),
        )
        val visible = QueenslandRailClearances.visibleBridges(bridges, on)
        val qr = visible.filter { QueenslandRailClearances.isQueenslandRail(it) }
        assertTrue("Expected QR SEQ entries when flag on", qr.size >= 100)
        qr.forEach { bridge ->
            assertEquals(BridgeClearanceStatus.VERIFIED, bridge.clearanceStatus)
            assertNotNull(bridge.clearanceHeightM)
            assertTrue(
                bridge.clearanceSource!!.contains("Queensland Rail", ignoreCase = true),
            )
            assertTrue(
                bridge.clearanceSource!!.contains(QueenslandRailClearances.SOURCE_URL),
            )
            assertEquals(QueenslandRailClearances.RETRIEVED_AT, bridge.clearanceRetrievedAt)
        }
    }

    @Test
    fun publishedQrSpotChecksMatchPageFiguresAndMapCoords() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val qr = bridges.filter { QueenslandRailClearances.isQueenslandRail(it) }
        // Spot-check published QR page rows + Google Maps place coords from the same page.
        val spots = listOf(
            Spot("ascot-subway", 3.4, -27.430716, 153.064722),
            Spot("wynnum-colina-street-berrima-street", 1.9, -27.443741, 153.168832),
            Spot("brisbane-city-countess-street", 4.4, -27.465055, 153.015782),
            Spot("eudlo-rosebed-street", 2.4, -26.724676, 152.956788),
            Spot("woolloongabba-ipswich-road", 5.4, -27.496266, 153.034905),
            Spot("beenleigh-market-place-drive", 2.9, -27.717974, 153.205171),
        )
        spots.forEach { spot ->
            val match = qr.firstOrNull { it.id.contains(spot.idPart, ignoreCase = true) }
            assertNotNull("Missing QR entry for ${spot.idPart}", match)
            assertEquals(spot.heightM, match!!.clearanceHeightM!!, 0.001)
            assertTrue(
                "${match.id} lat ${match.latitude} != ${spot.lat}",
                abs(match.latitude - spot.lat) < 0.0002,
            )
            assertTrue(
                "${match.id} lng ${match.longitude} != ${spot.lng}",
                abs(match.longitude - spot.lng) < 0.0002,
            )
        }
    }

    @Test
    fun doesNotInventHeightsOutsidePublishedQrPage() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val qr = bridges.filter { QueenslandRailClearances.isQueenslandRail(it) }
        assertEquals(
            "Pack must ship exactly the published QR SEQ rows (no extras)",
            QueenslandRailClearances.PUBLISHED_ENTRY_COUNT,
            qr.size,
        )
        qr.forEach { bridge ->
            val h = bridge.clearanceHeightM!!
            assertTrue(
                "${bridge.id} height $h outside published QR bands",
                h in 1.9..5.4,
            )
        }
    }

    @Test
    fun bccCbdRiverCrossingsRemainFromVerifiedPack() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val story = bridges.first { it.id.contains("story-bridge", ignoreCase = true) }
        assertEquals(3.8, story.clearanceHeightM!!, 0.001)
        assertTrue(story.clearanceSource!!.contains("Brisbane City Council", ignoreCase = true))

        val victoria = bridges.first { it.id.contains("victoria-bridge", ignoreCase = true) }
        assertEquals(4.4, victoria.clearanceHeightM!!, 0.001)
        assertTrue(victoria.clearanceSource!!.contains("Brisbane City Council", ignoreCase = true))

        listOf("captain-cook", "william-jolly", "go-between").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertNull(bridge.clearanceHeightM)
            assertEquals(BridgeClearanceStatus.UNKNOWN, bridge.clearanceStatus)
        }
        listOf("goodwill", "kurilpa").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertNull(bridge.clearanceHeightM)
            assertEquals(BridgeClearanceStatus.NOT_APPLICABLE, bridge.clearanceStatus)
        }
    }

    @Test
    fun packMetadataCitesQueenslandRailUrlAndRetrievalDate() {
        val meta = VerifiedCbdClearances.loadPackMetadata(RuntimeEnvironment.getApplication())
        assertNotNull(meta)
        assertEquals(QueenslandRailClearances.RETRIEVED_AT, meta!!.retrievedAt)
        assertTrue(
            meta.sources.any {
                it.contains(QueenslandRailClearances.SOURCE_URL) &&
                    it.contains(QueenslandRailClearances.RETRIEVED_AT)
            },
        )
    }

    @Test
    fun localFirstLifecycleUnchangedForQueenslandRailPack() {
        assertTrue(BridgePackLifecycle.isLocalFirst)
        assertFalse(BridgePackLifecycle.loadActiveUsesNetwork)
        assertTrue(BridgePackLifecycle.refreshOnlyWhenVersionNewer)
    }

    private data class Spot(
        val idPart: String,
        val heightM: Double,
        val lat: Double,
        val lng: Double,
    )
}
