package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * In-app Licenses page (`ff_licenses_page`, ATNA-116).
 *
 * Defaults **on** (Play / attribution norms): Settings → Licenses renders the
 * curated inventory at [INVENTORY_ASSET_PATH]. Human mirror: docs/licenses.md.
 *
 * OpenStreetMap / OpenFreeMap attribution must remain visible to drivers —
 * MapLibre map attribution stays enabled, and the Licenses page lists OSM/OFM.
 *
 * Gherkin: features/licenses_page.feature
 */
object LicensesCatalog {
    const val FLAG_KEY: String = FlagKeys.LICENSES_PAGE
    const val ROUTE: String = "licenses"
    const val SETTINGS_ENTRY_TAG: String = "settings_licenses_entry"
    const val PAGE_TEST_TAG: String = "licenses_page"
    const val SETTINGS_LABEL: String = "Licenses"

    /** Ship / select-user default — legal attribution surface. */
    const val DEFAULT_ENABLED: Boolean = true
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = true
    const val showsInSettings: Boolean = true

    /** Runtime SoT the UI reads (packaged asset). */
    const val INVENTORY_ASSET_PATH: String = "licenses/third_party.json"

    /** MapLibre UiSettings must keep attribution enabled (OSM/OFM). */
    const val mapAttributionRemainsEnabled: Boolean = true
    const val openStreetMapAttributionRequired: Boolean = true

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.LICENSES_PAGE)

    fun shouldShow(flags: FeatureFlags): Boolean = isEnabled(flags)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.LICENSES_PAGE, enabled)
    }
}
