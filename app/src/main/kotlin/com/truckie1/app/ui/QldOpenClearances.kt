package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags

/**
 * Combined visibility filter for QLD open-data clearance packs (ATNA-46).
 *
 * Applies QR → TMR → BCC GIS gates so each source can be toggled independently
 * while CBD verified samples remain available under their own flags.
 */
object QldOpenClearances {
    fun visibleBridges(bridges: List<BridgePoint>, flags: FeatureFlags): List<BridgePoint> {
        val afterQr = QueenslandRailClearances.visibleBridges(bridges, flags)
        val afterTmr = TmrStructureClearances.visibleBridges(afterQr, flags)
        return BccRoadHeightRestrictions.visibleBridges(afterTmr, flags)
    }
}
