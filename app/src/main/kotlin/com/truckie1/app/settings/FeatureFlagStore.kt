package com.truckie1.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.truckie1.flags.LocalFeatureFlags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Durable feature-flag overrides (ATNA-93).
 *
 * One write path: [LocalFeatureFlags.setEnabled] → [FeatureFlagStore.set].
 * Demo mode and heading-up use the same store — no parallel Demo prefs.
 *
 * Gherkin: features/feature_flag_persist.feature
 */
interface FeatureFlagStore {
    fun loadAll(): Map<String, Boolean>
    fun set(key: String, enabled: Boolean)
}

/** Unit-test store (no Android DataStore). */
class InMemoryFeatureFlagStore(
    initial: Map<String, Boolean> = emptyMap(),
) : FeatureFlagStore {
    private val values = initial.toMutableMap()

    override fun loadAll(): Map<String, Boolean> = values.toMap()

    override fun set(key: String, enabled: Boolean) {
        values[key] = enabled
    }
}

/**
 * Preferences DataStore-backed store for the app process.
 * Sync facade matches [LocalFeatureFlags] (hydrate at Application.onCreate).
 */
class DataStoreFeatureFlagStore(
    private val dataStore: DataStore<Preferences>,
) : FeatureFlagStore {

    constructor(context: Context) : this(
        PreferenceDataStoreFactory.create(
            produceFile = {
                context.applicationContext.preferencesDataStoreFile(
                    FeatureFlagPersistence.DATA_STORE_NAME,
                )
            },
        ),
    )

    /** Test / custom file (Robolectric isolation). */
    constructor(produceFile: () -> File) : this(
        PreferenceDataStoreFactory.create(produceFile = produceFile),
    )

    override fun loadAll(): Map<String, Boolean> = runBlocking {
        dataStore.data.first()
            .asMap()
            .mapNotNull { (key, value) ->
                (value as? Boolean)?.let { key.name to it }
            }
            .toMap()
    }

    override fun set(key: String, enabled: Boolean) {
        runBlocking {
            dataStore.edit { prefs ->
                prefs[booleanPreferencesKey(key)] = enabled
            }
        }
    }
}

/**
 * Binds [LocalFeatureFlags] to a single [FeatureFlagStore].
 * Seeds (debug / DEFAULT_ENABLED) use [LocalFeatureFlags.seedIfAbsent] and
 * never write the store.
 */
object FeatureFlagPersistence {
    const val CATALOG_ID: String = "feature_flags"
    const val DATA_STORE_NAME: String = "truckie_feature_flags"

    fun bind(store: FeatureFlagStore): LocalFeatureFlags =
        LocalFeatureFlags(
            initial = store.loadAll(),
            onOverrideChanged = store::set,
        )
}
