package com.truckie1.app.ui

/**
 * Shared map-home chrome for circular icon controls (Recenter, orientation lock).
 *
 * **Core requirement:** both controls share one visual treatment — Material filled
 * primary circle, onPrimary icon tint, equal elevation. Only the glyph may differ.
 * Never style the orientation lock with secondaryContainer while Recenter is primary.
 *
 * Gherkin: features/map_first_home.feature, features/map_heading_up.feature
 */
object MapChromeIconControls {
    /** Non-negotiable: Recenter and orientation lock must match. */
    const val consistentVisualTreatmentRequired: Boolean = true

    const val usesCircleShape: Boolean = true
    /** Same filled primary as Recenter — never secondaryContainer for these FABs. */
    const val usesPrimarySurface: Boolean = true
    const val usesOnPrimaryIconTint: Boolean = true
    const val shadowElevationDp: Int = 4
    const val endPaddingDp: Int = 16
    const val bottomPaddingAboveNavBarsDp: Int = 96
    /** Stack step when both Recenter and orientation lock are shown. */
    const val stackedControlStepDp: Int = 64

    const val RECENTER_TEST_TAG: String = "map_recenter_button"
    const val RECENTER_ICON: String = "MyLocation"
    const val ORIENTATION_LOCK_ICON: String = "Navigation"
    const val ORIENTATION_UNLOCK_ICON: String = "Explore"
}
