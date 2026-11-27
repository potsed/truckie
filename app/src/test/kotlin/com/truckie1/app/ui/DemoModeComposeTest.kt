package com.truckie1.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import com.truckie1.profiles.InMemoryVehicleProfileRepository
import com.truckie1.profiles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Gherkin: features/demo_mode.feature — Settings Demo mode switch (ATNA-75).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DemoModeComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsDemoModeSwitchTogglesFlagAndSwapsDataSource() {
        val flags = LocalFeatureFlags()
        val realRepo = InMemoryVehicleProfileRepository()
        realRepo.save(
            Vehicle(
                rego = "REAL99",
                nickname = "Real",
                vehicleType = "Rigid",
                make = "Volvo",
                model = "FM",
                year = 2020,
                heightM = 4.0,
                widthM = 2.5,
                lengthM = 10.0,
                massT = 10.0,
                tareT = 6.0,
                gvmT = 15.0,
                gcmT = 15.0,
            ),
        )
        val data = DualModeAppDataSource(
            real = RealAppDataSource(vehicles = realRepo),
            demo = DemoAppDataSource.create(today = LocalDate.of(2026, 11, 27)),
            flags = flags,
        )
        val controller = DemoModeController(flags = flags)

        composeRule.setContent {
            TruckieTheme {
                SettingsScreen(
                    bufferM = 0.3,
                    featureFlags = flags,
                    showDemoModeSwitch = true,
                    onDemoModeChanged = { enabled -> controller.setEnabled(enabled) },
                )
            }
        }
        composeRule.onNodeWithText(DemoModeCatalog.SETTINGS_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOff()

        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).performClick()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOn()
        assertTrue(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertEquals(listOf(DemoDataset.VEHICLE_REGO), data.vehicles.all().map { it.rego })
        assertTrue(DemoDataset.isDemoRouteId(data.routes.activeRoute()!!.id))
        assertEquals(6, data.routes.activeRoute()!!.stops.size)
        assertFalse(
            DemoDataset.hasDemoRevealingProductCopy(
                routeName = data.routes.activeRoute()?.name,
                vehicleNickname = data.vehicles.all().first().nickname,
                vehicleNotes = data.vehicles.all().first().notes,
            ),
        )

        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).performClick()
        composeRule.onNodeWithTag(DemoModeCatalog.SETTINGS_TEST_TAG).assertIsOff()
        assertFalse(flags.isEnabled(FlagKeys.DEMO_MODE))
        assertEquals(listOf("REAL99"), data.vehicles.all().map { it.rego })
        val active = data.routes.activeRoute()
        assertTrue(active == null || !DemoDataset.isDemoRouteId(active.id))
    }
}
