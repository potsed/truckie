package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/licenses_page.feature — flag + Settings entry (ATNA-116).
 */
class LicensesCatalogTest {

    @Test
    fun flagKeyAndDefaultsOnForAttributionSurface() {
        assertEquals("ff_licenses_page", FlagKeys.LICENSES_PAGE)
        assertEquals(FlagKeys.LICENSES_PAGE, LicensesCatalog.FLAG_KEY)
        assertTrue(LicensesCatalog.DEFAULT_ENABLED)
        assertTrue(LicensesCatalog.ENABLED_BY_DEFAULT_IN_DEBUG)
        assertTrue(LicensesCatalog.showsInSettings)
        assertEquals("licenses", LicensesCatalog.ROUTE)
        assertEquals("settings_licenses_entry", LicensesCatalog.SETTINGS_ENTRY_TAG)
        assertEquals("licenses_page", LicensesCatalog.PAGE_TEST_TAG)
    }

    @Test
    fun flagToggleGatesSettingsEntry() {
        val flags = LocalFeatureFlags()
        assertFalse(LicensesCatalog.isEnabled(flags))
        assertFalse(LicensesCatalog.shouldShow(flags))

        LicensesCatalog.setEnabled(flags, true)
        assertTrue(LicensesCatalog.isEnabled(flags))
        assertTrue(LicensesCatalog.shouldShow(flags))

        LicensesCatalog.setEnabled(flags, false)
        assertFalse(LicensesCatalog.shouldShow(flags))
    }

    @Test
    fun inventoryAssetPathAndMapAttributionContract() {
        assertEquals("licenses/third_party.json", LicensesCatalog.INVENTORY_ASSET_PATH)
        assertTrue(LicensesCatalog.mapAttributionRemainsEnabled)
        assertTrue(LicensesCatalog.openStreetMapAttributionRequired)
    }
}
