package com.truckie1.app.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Settings preference: optional **post-route** navigation handoff app.
 *
 * Key: [KEY] (`navigation_app_package`)
 * Default: [VALUE_IN_APP] — stay on Truckie MapLibre after GraphHopper calculates.
 * External: installed app package for handoff **after** in-app GraphHopper path finding
 * (bridges/weight constraints). External apps never replace GraphHopper for routing.
 */
object NavigationAppPreference {
    const val KEY: String = "navigation_app_package"
    const val VALUE_IN_APP: String = "com.truckie1.in_app"
    const val PREFS_NAME: String = "truckie_settings"

    fun showsInSettings(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.NAVIGATE)

    fun options(query: InstalledNavigationAppQuery): List<NavigationAppSelection> =
        listOf(NavigationAppSelection.InAppTruckie) +
            query.list().map {
                NavigationAppSelection.External(
                    packageName = it.packageName,
                    label = it.label,
                )
            }
}

sealed class NavigationAppSelection {
    abstract val packageName: String
    abstract val label: String
    abstract val isInApp: Boolean

    data object InAppTruckie : NavigationAppSelection() {
        override val packageName: String = NavigationAppPreference.VALUE_IN_APP
        override val label: String = "Truckie (in-app map)"
        override val isInApp: Boolean = true
    }

    data class External(
        override val packageName: String,
        override val label: String,
    ) : NavigationAppSelection() {
        override val isInApp: Boolean = false
    }

    companion object {
        fun fromStored(packageName: String?, labelLookup: (String) -> String?): NavigationAppSelection {
            if (packageName.isNullOrBlank() || packageName == NavigationAppPreference.VALUE_IN_APP) {
                return InAppTruckie
            }
            return External(
                packageName = packageName,
                label = labelLookup(packageName) ?: packageName,
            )
        }
    }
}

data class InstalledNavigationApp(
    val packageName: String,
    val label: String,
)

fun interface InstalledNavigationAppQuery {
    fun list(): List<InstalledNavigationApp>
}

interface NavigationAppStore {
    fun packageName(): String
    fun selection(labelLookup: (String) -> String? = { null }): NavigationAppSelection
    fun setSelection(selection: NavigationAppSelection)
}

/** Unit-test store (no Android SharedPreferences). */
class InMemoryNavigationAppStore(
    initialPackage: String = NavigationAppPreference.VALUE_IN_APP,
) : NavigationAppStore {
    private var stored = initialPackage
    private var labels = mutableMapOf<String, String>()

    override fun packageName(): String = stored

    override fun selection(labelLookup: (String) -> String?): NavigationAppSelection =
        NavigationAppSelection.fromStored(stored) { pkg ->
            labels[pkg] ?: labelLookup(pkg)
        }

    override fun setSelection(selection: NavigationAppSelection) {
        stored = selection.packageName
        if (selection is NavigationAppSelection.External) {
            labels[selection.packageName] = selection.label
        }
    }
}

/** SharedPreferences-backed store for the app process. */
class SharedNavigationAppStore(
    context: Context,
) : NavigationAppStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(NavigationAppPreference.PREFS_NAME, Context.MODE_PRIVATE)

    override fun packageName(): String =
        prefs.getString(NavigationAppPreference.KEY, NavigationAppPreference.VALUE_IN_APP)
            ?: NavigationAppPreference.VALUE_IN_APP

    override fun selection(labelLookup: (String) -> String?): NavigationAppSelection =
        NavigationAppSelection.fromStored(packageName(), labelLookup)

    override fun setSelection(selection: NavigationAppSelection) {
        prefs.edit().putString(NavigationAppPreference.KEY, selection.packageName).apply()
    }
}

/**
 * Queries PackageManager for apps that can handle navigation / geo VIEW intents.
 * Requires `<queries>` declarations for geo and google.navigation schemes.
 */
class PackageManagerNavigationAppQuery(
    private val packageManager: PackageManager,
) : InstalledNavigationAppQuery {
    override fun list(): List<InstalledNavigationApp> {
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=0,0")),
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=0,0")),
        )
        val seen = linkedMapOf<String, InstalledNavigationApp>()
        for (intent in intents) {
            val resolves = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in resolves) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (pkg == "com.truckie1.app") continue
                if (seen.containsKey(pkg)) continue
                val label = info.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                seen[pkg] = InstalledNavigationApp(packageName = pkg, label = label)
            }
        }
        return seen.values.toList()
    }
}

/**
 * Builds an ACTION_VIEW handoff intent for an external navigation app.
 * Destination is the route **end** (round trip uses the same end coordinates).
 */
object ExternalNavigationHandoff {
    fun buildIntent(
        packageName: String,
        latitude: Double,
        longitude: Double,
    ): Intent {
        val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }
}
