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

/**
 * Gherkin: features/verified_cbd_clearances.feature — ATNA-42.
 *
 * Mock demo heights must not ship; only BCC/QR/TMR/NHVR-published road
 * clearances (or explicit unknown / not_applicable).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VerifiedCbdClearancesTest {

    @Test
    fun verifiedCbdClearancesFlagKeyIsFfVerifiedCbdClearances() {
        assertEquals("ff_verified_cbd_clearances", FlagKeys.VERIFIED_CBD_CLEARANCES)
        val flags = LocalFeatureFlags()
        assertFalse(VerifiedCbdClearances.isEnabled(flags))
        flags.setEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES, true)
        assertTrue(VerifiedCbdClearances.isEnabled(flags))
    }

    @Test
    fun packBootstrapVersionBumpsPastMockSampleForReseed() {
        assertTrue(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                "0.3.0-sample",
            ),
        )
        assertEquals("0.4.0-sample", CachedBridgePackRepository.BOOTSTRAP_VERSION)
    }

    @Test
    fun storyAndVictoriaUseBccSignedUnderpassClearances() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val story = bridges.first { it.id.contains("story", ignoreCase = true) }
        assertEquals(BridgeClearanceStatus.VERIFIED, story.clearanceStatus)
        assertEquals(3.8, story.clearanceHeightM!!, 0.001)
        assertTrue(story.clearanceSource!!.contains("Brisbane City Council", ignoreCase = true))
        assertTrue(story.clearanceSourceId!!.startsWith("B0001"))
        assertEquals(VerifiedCbdClearances.RETRIEVED_AT, story.clearanceRetrievedAt)

        val victoria = bridges.first { it.id.contains("victoria", ignoreCase = true) }
        assertEquals(BridgeClearanceStatus.VERIFIED, victoria.clearanceStatus)
        assertEquals(4.4, victoria.clearanceHeightM!!, 0.001)
        assertTrue(victoria.clearanceSource!!.contains("Brisbane City Council", ignoreCase = true))
        assertTrue(victoria.clearanceSourceId!!.startsWith("B0002"))
        assertEquals(VerifiedCbdClearances.RETRIEVED_AT, victoria.clearanceRetrievedAt)
    }

    @Test
    fun roadSpansWithoutPublishedClearanceAreUnknownWithoutHeight() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        listOf("captain-cook", "william-jolly", "go-between").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertNull("${bridge.id} must not invent a height", bridge.clearanceHeightM)
            assertEquals(BridgeClearanceStatus.UNKNOWN, bridge.clearanceStatus)
            assertNotNull(bridge.clearanceSource)
            assertFalse(
                "Must not treat MSQ vessel air draft as road clearance for ${bridge.id}",
                bridge.clearanceSource!!.contains("air draft", ignoreCase = true) &&
                    bridge.clearanceHeightM != null,
            )
            assertTrue(
                bridge.clearanceSource!!.contains("road", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("HAT", ignoreCase = true) ||
                    bridge.clearanceSource!!.contains("vessel", ignoreCase = true),
            )
        }
    }

    @Test
    fun pedestrianCycleBridgesAreNotApplicableWithoutHeight() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        listOf("goodwill", "kurilpa").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertNull(bridge.clearanceHeightM)
            assertEquals(BridgeClearanceStatus.NOT_APPLICABLE, bridge.clearanceStatus)
            assertNotNull(bridge.clearanceSource)
        }
    }

    @Test
    fun noSampleBridgeKeepsFormerMockDemoHeights() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val formerMocks = setOf(4.3, 4.5, 4.6, 4.8, 5.0)
        bridges.forEach { bridge ->
            val h = bridge.clearanceHeightM
            if (h != null) {
                assertFalse(
                    "${bridge.id} still has former mock height $h",
                    formerMocks.any { kotlin.math.abs(it - h) < 0.001 },
                )
            }
        }
    }

    @Test
    fun verifiedFlagGatesClearanceLabelsToVerifiedHeightsOnly() {
        val verified = BridgePoint(
            id = "v",
            name = "Verified",
            latitude = -27.47,
            longitude = 153.02,
            clearanceHeightM = 3.8,
            clearanceStatus = BridgeClearanceStatus.VERIFIED,
        )
        val unknownWithHeight = BridgePoint(
            id = "u",
            name = "Unknown",
            latitude = -27.47,
            longitude = 153.02,
            clearanceHeightM = 4.5,
            clearanceStatus = BridgeClearanceStatus.UNKNOWN,
        )
        val off = LocalFeatureFlags()
        assertEquals("3.8 m", BridgeMarkersCatalog.clearanceLabel(verified, off))
        assertEquals("4.5 m", BridgeMarkersCatalog.clearanceLabel(unknownWithHeight, off))

        val on = LocalFeatureFlags(mapOf(FlagKeys.VERIFIED_CBD_CLEARANCES to true))
        assertEquals("3.8 m", BridgeMarkersCatalog.clearanceLabel(verified, on))
        assertNull(BridgeMarkersCatalog.clearanceLabel(unknownWithHeight, on))
        assertNull(
            BridgeMarkersCatalog.clearanceLabel(
                verified.copy(clearanceHeightM = null),
                on,
            ),
        )
    }

    @Test
    fun packMetadataCitesSourcesAndRetrievalDate() {
        val context = RuntimeEnvironment.getApplication()
        val meta = VerifiedCbdClearances.loadPackMetadata(context)
        assertNotNull(meta)
        assertEquals(VerifiedCbdClearances.RETRIEVED_AT, meta!!.retrievedAt)
        assertTrue(meta.sources.isNotEmpty())
        assertTrue(meta.sources.any { it.contains("Brisbane City Council", ignoreCase = true) })
        assertTrue(
            meta.clearancePolicy.contains("not invent", ignoreCase = true) ||
                meta.clearancePolicy.contains("unknown", ignoreCase = true),
        )
        assertTrue(
            meta.clearancePolicy.contains("air draft", ignoreCase = true) ||
                meta.clearancePolicy.contains("vessel", ignoreCase = true),
        )
    }

    @Test
    fun localFirstLifecycleUnchangedForVerifiedPack() {
        assertTrue(BridgePackLifecycle.isLocalFirst)
        assertFalse(BridgePackLifecycle.loadActiveUsesNetwork)
    }
}
