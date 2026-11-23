package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Gherkin: features/bridge_markers.feature, features/jump_to_sample_bridges.feature
 *
 * Demo sample points must sit on real Brisbane CBD crossings (not mid-river /
 * mid-block). Clearances are verified published road heights or omitted (ATNA-42).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BrisbaneCbdSampleBridgesTest {

    /** OSM Nominatim man_made=bridge centroids (ATNA-41 snap targets). */
    private data class Anchor(
        val idContains: String,
        val nameContains: String,
        val lat: Double,
        val lng: Double,
    )

    private val cbdAnchors = listOf(
        Anchor("story-bridge", "Story Bridge", -27.4682221, 153.0358746),
        Anchor("victoria-bridge", "Victoria Bridge", -27.4724167, 153.0210044),
        Anchor("captain-cook", "Captain Cook", -27.4804702, 153.0289367),
        Anchor("goodwill", "Goodwill", -27.4806730, 153.0269819),
        Anchor("william-jolly", "William Jolly", -27.4691880, 153.0156208),
        Anchor("go-between", "Go Between", -27.4698444, 153.0122765),
        Anchor("kurilpa", "Kurilpa", -27.4691815, 153.0188130),
    )

    /** ~80 m — enough for deck centre vs approach, not mid-block / wrong span. */
    private val maxSnapMetres: Double = 80.0

    @Test
    fun sampleBridgesSnapOntoNamedBrisbaneCbdCrossings() {
        val context = RuntimeEnvironment.getApplication()
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
        assertTrue(bridges.size >= cbdAnchors.size)

        cbdAnchors.forEach { anchor ->
            val match = bridges.firstOrNull { bridge ->
                bridge.id.contains(anchor.idContains, ignoreCase = true) ||
                    bridge.name.contains(anchor.nameContains, ignoreCase = true)
            }
            assertTrue(
                "Missing sample for ${anchor.nameContains} (ids=${bridges.map { it.id }})",
                match != null,
            )
            val bridge = match!!
            val metres = BridgeMarkersVisibility.distanceKm(
                bridge.latitude,
                bridge.longitude,
                anchor.lat,
                anchor.lng,
            ) * 1000.0
            assertTrue(
                "${bridge.id} is ${metres.toInt()}m from ${anchor.nameContains} " +
                    "(${anchor.lat},${anchor.lng}); expected ≤${maxSnapMetres.toInt()}m " +
                    "(was ${bridge.latitude},${bridge.longitude})",
                metres <= maxSnapMetres,
            )
        }
    }

    @Test
    fun cbdSampleClusterStaysWithinJumpRadiusOfBrisbaneFallback() {
        val context = RuntimeEnvironment.getApplication()
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
        val nearby = BridgeMarkersVisibility.nearBrisbaneFallback(bridges)
        assertTrue(
            "Need ≥${BridgeMarkersVisibility.MIN_SEQ_BRIDGES} CBD crossings within " +
                "${BridgeMarkersVisibility.SEQ_RADIUS_KM} km for Jump, was ${nearby.size}",
            nearby.size >= BridgeMarkersVisibility.MIN_SEQ_BRIDGES,
        )
        // Every named CBD anchor must be in the Jump cluster (not Indooroopilly-far).
        cbdAnchors.forEach { anchor ->
            assertTrue(
                "${anchor.nameContains} must be inside Jump SEQ radius",
                nearby.any {
                    it.id.contains(anchor.idContains, ignoreCase = true) ||
                        it.name.contains(anchor.nameContains, ignoreCase = true)
                },
            )
        }
    }

    @Test
    fun clearanceLabelsAreVerifiedOrOmittedNotMocked() {
        val context = RuntimeEnvironment.getApplication()
        // CBD river samples only — QR SEQ covers a wider published height band.
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
            .filterNot { QueenslandRailClearances.isQueenslandRail(it) }
        val withHeight = bridges.filter { it.clearanceHeightM != null }
        assertTrue(withHeight.isNotEmpty())
        withHeight.forEach { bridge ->
            assertEquals(
                "${bridge.id} must be verified when height is present",
                BridgeClearanceStatus.VERIFIED,
                bridge.clearanceStatus,
            )
            val h = bridge.clearanceHeightM!!
            assertTrue("${bridge.id} verified clearance $h out of signed range", h in 2.0..5.3)
            assertEquals(
                "%.1f m".format(java.util.Locale.US, h),
                BridgeMarkersCatalog.clearanceLabel(bridge),
            )
        }
        assertTrue(
            "At least one marker-only sample (no clearance) keeps optional contract",
            bridges.any { it.clearanceHeightM == null },
        )
    }
}
