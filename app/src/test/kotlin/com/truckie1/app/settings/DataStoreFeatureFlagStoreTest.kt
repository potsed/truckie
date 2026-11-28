package com.truckie1.app.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.truckie1.app.ui.DemoModeCatalog
import com.truckie1.app.ui.DemoModeController
import com.truckie1.flags.FlagKeys
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Gherkin: features/feature_flag_persist.feature — DataStore survives "restart".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DataStoreFeatureFlagStoreTest {

    private fun freshStore(): DataStoreFeatureFlagStore {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = File(
            context.filesDir,
            "feature_flags_test_${System.nanoTime()}.preferences_pb",
        )
        return DataStoreFeatureFlagStore(produceFile = { file })
    }

    @Test
    fun demoModeSurvivesRebindFromSameDataStore() {
        // Android forbids two DataStore instances on one file in-process; process
        // death is simulated by re-binding LocalFeatureFlags from loadAll().
        val store = freshStore()
        val firstFlags = FeatureFlagPersistence.bind(store)
        DemoModeController(firstFlags).setEnabled(true)
        assertTrue(DemoModeCatalog.isEnabled(firstFlags))
        assertTrue(store.loadAll()[FlagKeys.DEMO_MODE] == true)

        val restarted = FeatureFlagPersistence.bind(store)
        assertTrue(DemoModeCatalog.isEnabled(restarted))
    }

    @Test
    fun freshDataStoreKeepsDemoOff() {
        val store = freshStore()
        val flags = FeatureFlagPersistence.bind(store)
        assertFalse(DemoModeCatalog.isEnabled(flags))
        assertFalse(store.loadAll().containsKey(FlagKeys.DEMO_MODE))
    }
}
