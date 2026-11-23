package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.trips.ClearanceBuffer

/**
 * How a vehicle may relate to a bridge / structure (ATNA-48).
 *
 * Gherkin: features/bridge_passage.feature
 *
 * - [UNDER] — path goes under (rail underbridges, underpasses, signed under clearance)
 * - [OVER] — drive across the deck (river road bridge roadway)
 * - [BOTH] — deck is driveable over, and a published under-path clearance exists
 * - [UNKNOWN] — not classified without inventing data
 *
 * Routing rule (Daniel): **OK to drive over** a low bridge deck; **not OK to
 * drive under** when clearance conflicts with vehicle height + buffer.
 */
enum class BridgePassage {
    UNDER,
    OVER,
    BOTH,
    UNKNOWN,
    ;

    fun wireName(): String = name.lowercase()

    /** True when an under-path clearance restriction may apply. */
    fun restrictsUnder(): Boolean = this == UNDER || this == BOTH

    companion object {
        fun fromWire(raw: String?): BridgePassage {
            val key = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.wireName() == key } ?: UNKNOWN
        }

        /**
         * Resolve pack passage. Explicit wire value wins. Otherwise infer from
         * known providers / CBD samples — **never invents clearance heights**.
         */
        fun resolve(
            id: String,
            clearanceProvider: String?,
            clearanceHeightM: Double?,
            clearanceStatus: BridgeClearanceStatus,
            explicit: BridgePassage?,
        ): BridgePassage {
            if (explicit != null && explicit != UNKNOWN) return explicit
            val idLower = id.lowercase()
            when {
                clearanceProvider == QueenslandRailClearances.PROVIDER_WIRE ||
                    idLower.startsWith("qld-qr-") -> return UNDER
                clearanceProvider == TmrStructureClearances.PROVIDER_WIRE ||
                    idLower.startsWith("qld-tmr-") -> return UNDER
                clearanceProvider == BccRoadHeightRestrictions.PROVIDER_WIRE ||
                    idLower.startsWith("qld-bcc-") -> return UNDER
                idLower.contains("story-bridge") ||
                    idLower.contains("victoria-bridge") -> return BOTH
                idLower.contains("captain-cook") ||
                    idLower.contains("william-jolly") ||
                    idLower.contains("go-between") ||
                    idLower.contains("goodwill") ||
                    idLower.contains("kurilpa") -> return OVER
                // Published road clearance without other cues = under-path restriction.
                clearanceHeightM != null &&
                    clearanceStatus == BridgeClearanceStatus.VERIFIED -> return UNDER
                else -> return UNKNOWN
            }
        }
    }
}

/**
 * Home-map + routing helpers for [BridgePassage] (ATNA-48).
 *
 * When [ff_bridge_passage][FlagKeys.BRIDGE_PASSAGE] is on, height conflicts
 * apply only to structures that restrict **under** passage.
 */
object BridgePassageFilter {
    const val FLAG_KEY: String = FlagKeys.BRIDGE_PASSAGE

    const val ROUTING_RULE: String =
        "OK to drive over a low bridge deck; not OK to drive under it when " +
            "clearance conflicts with vehicle height + system buffer."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.BRIDGE_PASSAGE)

    fun restrictsUnderPassage(bridge: BridgePoint): Boolean =
        bridge.passage.restrictsUnder()

    /**
     * When the flag is on, keep only bridges that restrict under-passage.
     * When off, return [bridges] unchanged.
     */
    fun bridgesRestrictingUnder(
        bridges: List<BridgePoint>,
        flags: FeatureFlags,
    ): List<BridgePoint> {
        if (!isEnabled(flags)) return bridges
        return bridges.filter { restrictsUnderPassage(it) }
    }

    /**
     * Whether a height conflict blocks travel for the given path relation.
     *
     * - Path [BridgePassage.OVER]: never blocked by under clearance (deck OK).
     * - Path [BridgePassage.UNDER]: blocked when the structure restricts under
     *   and published clearance is below vehicle height + buffer.
     * Missing heights never invent a value.
     */
    fun heightConflictBlocksPath(
        bridge: BridgePoint,
        vehicleHeightM: Double,
        bufferM: Double,
        pathRelation: BridgePassage,
    ): Boolean {
        if (pathRelation == BridgePassage.OVER) return false
        if (!restrictsUnderPassage(bridge)) return false
        val clearance = bridge.clearanceHeightM ?: return false
        val threshold = vehicleHeightM + ClearanceBuffer.clamp(bufferM)
        return clearance < threshold
    }
}
