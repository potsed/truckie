package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * AU W5-8 (“LOW LEVEL BRIDGE”) eligibility (ATNA-45).
 *
 * Gherkin: features/low_bridge_w5_8.feature
 *
 * Not every bridge is a low bridge. The W5-8 diamond is only for verified
 * restricted / low clearances — never for unknown, not_applicable, or high
 * open spans without a published low restriction.
 *
 * Threshold [LOW_CLEARANCE_MAX_M] = **4.6 m**, matching Queensland Rail’s
 * “Low clearance &lt;4.6m” table band on the SEQ railbridges page. QR
 * road-under-rail rows with a **published** clearance are height-restricted
 * underpasses and qualify even when listed in the 4.6–&lt;5.4 m band.
 */
object LowBridgeW58 {
    const val FLAG_KEY: String = FlagKeys.W5_8_LOW_ONLY

    /**
     * Maximum verified road clearance (metres) that qualifies as “low” for
     * non-QR assets. Aligns with QR’s “Low clearance &lt;4.6m” band (not the
     * page intro’s broader “under 5.3 m” wording for signage fitment).
     */
    const val LOW_CLEARANCE_MAX_M: Double = 4.6

    const val THRESHOLD_RATIONALE: String =
        "≤ 4.6 m matches Queensland Rail’s published “Low clearance <4.6m” " +
            "SEQ table band. QR road-under-rail rows with a published clearance " +
            "are treated as low/underbridge restrictions even at 4.6–5.4 m."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.W5_8_LOW_ONLY)

    /**
     * Whether this pack row may use the AU W5-8 low-level-bridge symbol.
     * Requires [BridgeClearanceStatus.VERIFIED] and a published height; QR
     * underbridges with published clearance always qualify; other sources
     * qualify only when height ≤ [LOW_CLEARANCE_MAX_M].
     */
    fun qualifiesForW58(bridge: BridgePoint): Boolean {
        if (bridge.clearanceStatus != BridgeClearanceStatus.VERIFIED) return false
        val height = bridge.clearanceHeightM ?: return false
        if (QueenslandRailClearances.isQueenslandRail(bridge)) return true
        return height <= LOW_CLEARANCE_MAX_M
    }

    /**
     * When [ff_w5_8_low_only][FlagKeys.W5_8_LOW_ONLY] is on, only qualifying
     * low bridges are returned for W5-8 style-layer binding. When off,
     * returns the input list unchanged (legacy all-markers behaviour).
     */
    fun bridgesForW58(bridges: List<BridgePoint>, flags: FeatureFlags): List<BridgePoint> {
        if (!isEnabled(flags)) return bridges
        return bridges.filter { qualifiesForW58(it) }
    }
}
