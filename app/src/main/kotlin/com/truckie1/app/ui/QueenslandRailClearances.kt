package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Queensland Rail SEQ road-under-rail clearances (ATNA-44).
 *
 * Gherkin: features/queensland_rail_clearances.feature
 *
 * Authoritative source (retrieved [RETRIEVED_AT]):
 * [SOURCE_URL]
 *
 * Pack entries are local-first in the bundled AU sample. [visibleBridges]
 * hides QR rows when the flag is off so CBD BCC data still loads. Never invent
 * heights for bridges absent from the published page.
 */
object QueenslandRailClearances {
    const val FLAG_KEY: String = FlagKeys.QUEENSLAND_RAIL_CLEARANCES
    const val SOURCE_URL: String =
        "https://www.queenslandrail.com.au/forcustomers/safetysecurity/railbridges"
    /** Retrieval date for QR SEQ ingest (Australia/Brisbane). */
    const val RETRIEVED_AT: String = "2026-11-23"
    const val PROVIDER_WIRE: String = "queensland_rail"
    /** Published Low clearance <4.6m + Clearance 4.6m to <5.4m rows on the QR page. */
    const val PUBLISHED_ENTRY_COUNT: Int = 106

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.QUEENSLAND_RAIL_CLEARANCES)

    fun isQueenslandRail(bridge: BridgePoint): Boolean {
        if (bridge.clearanceProvider == PROVIDER_WIRE) return true
        if (bridge.id.startsWith("qld-qr-", ignoreCase = true)) return true
        return bridge.clearanceSource?.contains("Queensland Rail", ignoreCase = true) == true &&
            bridge.id.startsWith("qld-qr-", ignoreCase = true)
    }

    /**
     * When [ff_queensland_rail_clearances][FlagKeys.QUEENSLAND_RAIL_CLEARANCES] is
     * off, drop QR pack rows so verified BCC CBD crossings still show.
     */
    fun visibleBridges(bridges: List<BridgePoint>, flags: FeatureFlags): List<BridgePoint> {
        if (isEnabled(flags)) return bridges
        return bridges.filterNot { isQueenslandRail(it) }
    }
}
