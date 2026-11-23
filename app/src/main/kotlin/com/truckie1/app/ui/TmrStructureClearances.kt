package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * TMR Structure Clearances in Queensland (ATNA-46).
 *
 * Gherkin: features/qld_tmr_bcc_clearances.feature
 *
 * Authoritative machine-readable spatial source (retrieved [RETRIEVED_AT]):
 * [SOURCE_URL] via StateRoadInformation MapServer layers 21 (&lt;4.6 m) and
 * 22 (≥4.6–&lt;5.4 m). Prefer published TMR heights over invented figures.
 * Physical roadside signs override pack data.
 */
object TmrStructureClearances {
    const val FLAG_KEY: String = FlagKeys.TMR_STRUCTURE_CLEARANCES
    const val SOURCE_URL: String =
        "https://www.data.qld.gov.au/dataset/structure-clearances-queensland"
    const val REST_MAP_SERVER: String =
        "https://spatial-gis.information.qld.gov.au/arcgis/rest/services/" +
            "Transportation/StateRoadInformation/MapServer"
    /** Retrieval date for TMR Structure Clearances ingest (Australia/Brisbane). */
    const val RETRIEVED_AT: String = "2026-11-23"
    const val PROVIDER_WIRE: String = "tmr_structure_clearances"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.TMR_STRUCTURE_CLEARANCES)

    fun isTmr(bridge: BridgePoint): Boolean {
        if (bridge.clearanceProvider == PROVIDER_WIRE) return true
        return bridge.id.startsWith("qld-tmr-", ignoreCase = true)
    }

    /**
     * When [ff_tmr_structure_clearances][FlagKeys.TMR_STRUCTURE_CLEARANCES] is
     * off, drop TMR pack rows so CBD / QR / BCC GIS still load per their flags.
     */
    fun visibleBridges(bridges: List<BridgePoint>, flags: FeatureFlags): List<BridgePoint> {
        if (isEnabled(flags)) return bridges
        return bridges.filterNot { isTmr(it) }
    }
}
