package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/low_bridge_w5_8.feature — ATNA-45.
 *
 * Not every bridge is a low bridge: AU W5-8 only for verified restricted clearances.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LowBridgeW58Test {

    @Test
    fun w58LowOnlyFlagKeyIsFfW58LowOnly() {
        assertEquals("ff_w5_8_low_only", FlagKeys.W5_8_LOW_ONLY)
        val flags = LocalFeatureFlags()
        assertFalse(LowBridgeW58.isEnabled(flags))
        flags.setEnabled(FlagKeys.W5_8_LOW_ONLY, true)
        assertTrue(LowBridgeW58.isEnabled(flags))
    }

    @Test
    fun lowClearanceThresholdIs46MetresMatchingQrLowBand() {
        assertEquals(4.6, LowBridgeW58.LOW_CLEARANCE_MAX_M, 0.001)
        assertTrue(
            LowBridgeW58.THRESHOLD_RATIONALE.contains("4.6", ignoreCase = true),
        )
        assertTrue(
            LowBridgeW58.THRESHOLD_RATIONALE.contains("Queensland Rail", ignoreCase = true) ||
                LowBridgeW58.THRESHOLD_RATIONALE.contains("QR", ignoreCase = true),
        )
    }

    @Test
    fun storyAndVictoriaQualifyAsLowUnderThreshold() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val story = bridges.first { it.id.contains("story-bridge", ignoreCase = true) }
        val victoria = bridges.first { it.id.contains("victoria-bridge", ignoreCase = true) }
        assertTrue(LowBridgeW58.qualifiesForW58(story))
        assertTrue(LowBridgeW58.qualifiesForW58(victoria))
        assertTrue(story.clearanceHeightM!! <= LowBridgeW58.LOW_CLEARANCE_MAX_M)
        assertTrue(victoria.clearanceHeightM!! <= LowBridgeW58.LOW_CLEARANCE_MAX_M)
    }

    @Test
    fun unknownAndNotApplicableCbdSpansDoNotGetW58() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        listOf("captain-cook", "william-jolly", "go-between", "goodwill", "kurilpa").forEach { idPart ->
            val bridge = bridges.first { it.id.contains(idPart, ignoreCase = true) }
            assertFalse(
                "${bridge.id} must not get W5-8 (${bridge.clearanceStatus})",
                LowBridgeW58.qualifiesForW58(bridge),
            )
        }
    }

    @Test
    fun queenslandRailPublishedUnderbridgesQualifyEvenAbove46() {
        val qrHigh = BridgePoint(
            id = "qld-qr-woolloongabba-ipswich-road",
            name = "Ipswich Road (Woolloongabba)",
            latitude = -27.496266,
            longitude = 153.034905,
            clearanceHeightM = 5.4,
            clearanceStatus = BridgeClearanceStatus.VERIFIED,
            clearanceSource = "Queensland Rail — Low rail bridges",
            clearanceProvider = QueenslandRailClearances.PROVIDER_WIRE,
        )
        assertTrue(
            "QR published underbridges are height-restricted even when > 4.6 m",
            LowBridgeW58.qualifiesForW58(qrHigh),
        )
        val nonQrHigh = qrHigh.copy(
            id = "qld-other-high-span",
            clearanceProvider = null,
            clearanceSource = "Some other verified source",
        )
        assertFalse(
            "Non-QR verified height above 4.6 m is not a W5-8 low bridge",
            LowBridgeW58.qualifiesForW58(nonQrHigh),
        )
    }

    @Test
    fun unverifiedOrMissingHeightNeverQualifies() {
        assertFalse(
            LowBridgeW58.qualifiesForW58(
                BridgePoint(
                    id = "u",
                    name = "Unknown",
                    latitude = -27.47,
                    longitude = 153.02,
                    clearanceHeightM = 3.0,
                    clearanceStatus = BridgeClearanceStatus.UNKNOWN,
                ),
            ),
        )
        assertFalse(
            LowBridgeW58.qualifiesForW58(
                BridgePoint(
                    id = "v",
                    name = "Verified no height",
                    latitude = -27.47,
                    longitude = 153.02,
                    clearanceHeightM = null,
                    clearanceStatus = BridgeClearanceStatus.VERIFIED,
                ),
            ),
        )
    }

    @Test
    fun bridgesForW58FiltersWhenFlagOn() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
        val off = LocalFeatureFlags(mapOf(FlagKeys.BRIDGE_MARKERS to true))
        assertEquals(bridges.size, LowBridgeW58.bridgesForW58(bridges, off).size)

        val on = LocalFeatureFlags(
            mapOf(
                FlagKeys.BRIDGE_MARKERS to true,
                FlagKeys.W5_8_LOW_ONLY to true,
            ),
        )
        val lowOnly = LowBridgeW58.bridgesForW58(bridges, on)
        assertTrue(lowOnly.isNotEmpty())
        assertTrue(lowOnly.size < bridges.size)
        lowOnly.forEach { assertTrue(LowBridgeW58.qualifiesForW58(it)) }
        assertTrue(lowOnly.any { it.id.contains("story-bridge", ignoreCase = true) })
        assertTrue(lowOnly.none { it.id.contains("goodwill", ignoreCase = true) })
        assertTrue(lowOnly.none { it.id.contains("captain-cook", ignoreCase = true) })
    }

    @Test
    fun packQrEntriesWithPublishedClearanceAllQualify() {
        val bridges = BridgeMarkersCatalog.loadFromAssets(RuntimeEnvironment.getApplication())
            .filter { QueenslandRailClearances.isQueenslandRail(it) }
        assertTrue(bridges.size >= 100)
        bridges.forEach { bridge ->
            assertTrue("${bridge.id} should qualify for W5-8", LowBridgeW58.qualifiesForW58(bridge))
        }
    }
}
