package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Brisbane City Council Road Height Restrictions GIS/CSV (ATNA-46).
 *
 * Gherkin: features/qld_tmr_bcc_clearances.feature
 *
 * Source (retrieved [RETRIEVED_AT]): [SOURCE_URL] — local bridges, tunnels,
 * and pedestrian overpasses under 4.6 m. CBD river verified samples (ATNA-42)
 * stay as curated `-sample` rows; this flag gates additional BCC GIS pack rows.
 * Physical roadside signs override pack data.
 */
object BccRoadHeightRestrictions {
    const val FLAG_KEY: String = FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS
    const val SOURCE_URL: String =
        "https://data.brisbane.qld.gov.au/explore/dataset/road-restrictions/"
    /** Retrieval date for BCC road-restrictions ingest (Australia/Brisbane). */
    const val RETRIEVED_AT: String = "2026-11-23"
    const val PROVIDER_WIRE: String = "bcc_road_restrictions"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS)

    /** BCC GIS ingest rows (not the curated CBD `-sample` river crossings). */
    fun isBccGis(bridge: BridgePoint): Boolean {
        if (bridge.clearanceProvider == PROVIDER_WIRE) return true
        return bridge.id.startsWith("qld-bcc-", ignoreCase = true)
    }

    /**
     * When [ff_bcc_road_height_restrictions][FlagKeys.BCC_ROAD_HEIGHT_RESTRICTIONS]
     * is off, drop BCC GIS pack rows; curated CBD samples remain.
     */
    fun visibleBridges(bridges: List<BridgePoint>, flags: FeatureFlags): List<BridgePoint> {
        if (isEnabled(flags)) return bridges
        return bridges.filterNot { isBccGis(it) }
    }
}
