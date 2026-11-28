package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gherkin: features/licenses_page.feature — Settings entry + Licenses UI (ATNA-116).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LicensesComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsShowsLicensesEntryWhenFlagOn() {
        val flags = LocalFeatureFlags()
        LicensesCatalog.setEnabled(flags, true)
        var opened = false

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showLicensesEntry = LicensesCatalog.shouldShow(flags),
                    onOpenLicenses = { opened = true },
                )
            }
        }

        composeRule.onNodeWithTag(LicensesCatalog.SETTINGS_ENTRY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LicensesCatalog.SETTINGS_ENTRY_TAG).performClick()
        assertTrue(opened)
    }

    @Test
    fun settingsHidesLicensesEntryWhenFlagOff() {
        val flags = LocalFeatureFlags()
        LicensesCatalog.setEnabled(flags, false)

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showLicensesEntry = LicensesCatalog.shouldShow(flags),
                    onOpenLicenses = {},
                )
            }
        }

        composeRule.onNodeWithTag(LicensesCatalog.SETTINGS_ENTRY_TAG).assertDoesNotExist()
    }

    @Test
    fun licensesPageRendersInventoryEntries() {
        val inventory = LicenseInventory(
            version = 1,
            entries = listOf(
                LicenseEntry(
                    id = "maplibre-android",
                    name = "MapLibre Native Android",
                    license = "BSD-2-Clause",
                    copyright = "Copyright (c) MapLibre contributors",
                    url = "https://github.com/maplibre/maplibre-native",
                    notes = "Map render SDK",
                    attributionRequired = false,
                ),
                LicenseEntry(
                    id = "openfreemap-osm",
                    name = "OpenFreeMap / OpenStreetMap",
                    license = "ODbL",
                    copyright = "© OpenStreetMap contributors",
                    url = "https://www.openstreetmap.org/copyright",
                    notes = "Basemap tiles; attribution required",
                    attributionRequired = true,
                ),
            ),
        )

        composeRule.setContent {
            TruckieTheme {
                LicensesScreen(inventory = inventory)
            }
        }

        composeRule.onNodeWithTag(LicensesCatalog.PAGE_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag("license_entry_maplibre-android").assertIsDisplayed()
        composeRule.onNodeWithTag("license_entry_openfreemap-osm").assertIsDisplayed()
        composeRule.onNodeWithText("MapLibre Native Android", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Licence: BSD-2-Clause", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Licence: ODbL", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap contributors", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun stubNoteNoLongerClaimsLicensesAreLaterOnly() {
        val flags = LocalFeatureFlags()
        LicensesCatalog.setEnabled(flags, true)

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showLicensesEntry = true,
                    onOpenLicenses = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "licenses land in later milestones",
            substring = true,
            ignoreCase = true,
        ).assertDoesNotExist()
    }
}
