package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags

/**
 * Single ordered home-map bridge visibility pipeline (ATNA-73).
 *
 * Replaces the inline RootNav chain **and** the duplicate
 * [MapLibreHost.bindBridges] LowBridgeW58 → passage → vehicle-height re-filter.
 *
 * Stages (flag-gated inside each helper):
 * 1. [qld_open] — QR / TMR / BCC open-data gates
 * 2. [w5_8_low] — AU W5-8 low-only eligibility
 * 3. [passage_under] — under-restricting structures when `ff_bridge_passage`
 * 4. [vehicle_height] — conflicts vs active vehicle height + buffer
 *
 * Compose runs [filter] once; style-layer bind uses [forStyleBind] on that
 * pre-filtered list (no second filter pass).
 *
 * Gherkin: features/bridge_passage_pipeline.feature, features/map_style_overlays.feature
 */
object BridgeVisibilityPipeline {
    /** Catalog: one pipeline owns map marker filters (Compose + bind). */
    const val singlePipelineOwnsMapFilters: Boolean = true

    /** Bind receives the pre-filtered list — does not re-apply stages. */
    const val bindReceivesPreFilteredList: Boolean = true

    val STAGE_ORDER: List<String> = listOf(
        "qld_open",
        "w5_8_low",
        "passage_under",
        "vehicle_height",
    )

    data class Input(
        val bridges: List<BridgePoint>,
        val flags: FeatureFlags,
        val vehicleHeightM: Double?,
        val bufferM: Double,
    )

    /**
     * Apply ordered visibility stages. Each stage respects its own feature flag
     * (passthrough when off). Never invents clearance heights.
     */
    fun filter(input: Input): List<BridgePoint> {
        val afterQld = QldOpenClearances.visibleBridges(input.bridges, input.flags)
        val afterW58 = LowBridgeW58.bridgesForW58(afterQld, input.flags)
        val afterPassage = BridgePassageFilter.bridgesRestrictingUnder(
            bridges = afterW58,
            flags = input.flags,
        )
        return VehicleHeightBridgeFilter.bridgesAffectingVehicle(
            bridges = afterPassage,
            flags = input.flags,
            vehicleHeightM = input.vehicleHeightM,
            bufferM = input.bufferM,
        )
    }

    /**
     * Style-layer bind path: list is already filtered by [filter].
     * Returns the same instance — assert-only contract for callers.
     */
    fun forStyleBind(preFiltered: List<BridgePoint>): List<BridgePoint> = preFiltered

    /**
     * @deprecated Use [forStyleBind] — Annotation SymbolManager deleted (ATNA-117).
     */
    @Deprecated(
        message = "Use forStyleBind",
        replaceWith = ReplaceWith("forStyleBind(preFiltered)"),
    )
    fun forSymbolBind(preFiltered: List<BridgePoint>): List<BridgePoint> =
        forStyleBind(preFiltered)
}
