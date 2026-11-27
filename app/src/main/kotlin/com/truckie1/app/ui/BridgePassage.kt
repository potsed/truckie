package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.routing.BridgePassagePolicy
import com.truckie1.routing.BridgePassageRelation

/**
 * App alias for the shared [BridgePassageRelation] (ATNA-73).
 *
 * One canonical type in core-routing — no twin enum versus routing.
 *
 * Gherkin: features/bridge_passage.feature, features/bridge_passage_pipeline.feature
 */
typealias BridgePassage = BridgePassageRelation

/**
 * Pack passage inference for home-map / bootstrap rows (ATNA-48).
 *
 * Explicit wire `passage` wins. Otherwise infer from known providers / CBD
 * samples — **never invents clearance heights**. Prefer wire values in JSON.
 */
object BridgePassageInference {
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
        if (explicit != null && explicit != BridgePassage.UNKNOWN) return explicit
        val idLower = id.lowercase()
        return when {
            clearanceProvider == QueenslandRailClearances.PROVIDER_WIRE ||
                idLower.startsWith("qld-qr-") -> BridgePassage.UNDER
            clearanceProvider == TmrStructureClearances.PROVIDER_WIRE ||
                idLower.startsWith("qld-tmr-") -> BridgePassage.UNDER
            clearanceProvider == BccRoadHeightRestrictions.PROVIDER_WIRE ||
                idLower.startsWith("qld-bcc-") -> BridgePassage.UNDER
            idLower.contains("story-bridge") ||
                idLower.contains("victoria-bridge") -> BridgePassage.BOTH
            idLower.contains("captain-cook") ||
                idLower.contains("william-jolly") ||
                idLower.contains("go-between") ||
                idLower.contains("goodwill") ||
                idLower.contains("kurilpa") -> BridgePassage.OVER
            // Published road clearance without other cues = under-path restriction.
            clearanceHeightM != null &&
                clearanceStatus == BridgeClearanceStatus.VERIFIED -> BridgePassage.UNDER
            else -> BridgePassage.UNKNOWN
        }
    }
}

/**
 * Home-map flag-gated facade over [BridgePassagePolicy] (ATNA-48 / ATNA-73).
 *
 * When [ff_bridge_passage][FlagKeys.BRIDGE_PASSAGE] is on, height conflicts
 * apply only to structures that restrict **under** passage.
 */
object BridgePassageFilter {
    const val FLAG_KEY: String = FlagKeys.BRIDGE_PASSAGE

    const val ROUTING_RULE: String = BridgePassagePolicy.ROUTING_RULE

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.BRIDGE_PASSAGE)

    fun restrictsUnderPassage(bridge: BridgePoint): Boolean =
        BridgePassagePolicy.restrictsUnder(bridge.passage)

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
     * Delegates to [BridgePassagePolicy] — same semantics as routing.
     */
    fun heightConflictBlocksPath(
        bridge: BridgePoint,
        vehicleHeightM: Double,
        bufferM: Double,
        pathRelation: BridgePassage,
    ): Boolean =
        BridgePassagePolicy.heightConflictBlocksPath(
            clearanceHeightM = bridge.clearanceHeightM,
            structurePassage = bridge.passage,
            vehicleHeightM = vehicleHeightM,
            bufferM = bufferM,
            pathRelation = pathRelation,
        )
}
