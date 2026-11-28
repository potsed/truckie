package com.truckie1.app.nhvr

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * NHVR / state restriction overlays (`ff_nhvr_overlay`, ATNA-11 / ATNA-43 /
 * ATNA-95 contracts).
 *
 * Defaults **off** in production and debug until the overlay ingest pipeline
 * ships. Authoritative reference is the NHVR National Network Map; Truckie
 * still routes in-app via PathRouter. Overlays are guidance — not a permit —
 * and must cite source metadata; never invent restriction geometries.
 * Roadside signs and official permits override digital layers. Distinct from
 * QLD bridge packs / AU low-bridges OSS (`ff_bridge_markers`).
 *
 * Gherkin: features/nhvr_overlay.feature
 */
object NhvrOverlayCatalog {
    const val FLAG_KEY: String = FlagKeys.NHVR_OVERLAY

    const val DEFAULT_ENABLED: Boolean = false
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = false

    const val authorityMapUrl: String = "https://maps.nhvr.gov.au/"
    const val routingRemainsInAppPathRouter: Boolean = true
    const val requiresCitedSourceMetadata: Boolean = true
    const val neverInventsRestrictionGeometries: Boolean = true
    const val roadsideSignsAndPermitsOverrideDigital: Boolean = true
    const val bridgePacksAreSeparateConcern: Boolean = true
    const val substitutesForBridgeMarkers: Boolean = false
    const val liveIngestTrackedBy: String = "ATNA-11"
    const val otherStateIngestTrackedBy: String = "ATNA-43"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NHVR_OVERLAY)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.NHVR_OVERLAY, enabled)
    }

    /** Draw only when the flag is on — no layer when off. */
    fun shouldDrawOverlay(flags: FeatureFlags): Boolean = isEnabled(flags)
}
