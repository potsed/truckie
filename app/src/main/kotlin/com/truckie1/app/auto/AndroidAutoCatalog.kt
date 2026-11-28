package com.truckie1.app.auto

import android.os.Build
import com.truckie1.app.AndroidAutoGate
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags

/**
 * Android Auto surfaces (`ff_android_auto`, ATNA-10 / ATNA-95 contracts).
 *
 * Defaults **off** in production and debug until Cars App
 * [androidx.car.app.navigation.model.NavigationTemplate] quality review and
 * clear next-destination on the car UI ship. Gate delegates to
 * [AndroidAutoGate] (flag + API 23+).
 *
 * Scaffold today: [TruckieCarAppService] + MessageTemplate. Phone keeps the
 * screen active while navigating; when Auto is connected the phone may blank
 * to save power.
 *
 * Gherkin: features/android_auto.feature
 */
object AndroidAutoCatalog {
    const val FLAG_KEY: String = FlagKeys.ANDROID_AUTO
    const val MIN_API: Int = AndroidAutoGate.MIN_API

    const val DEFAULT_ENABLED: Boolean = false
    const val ENABLED_BY_DEFAULT_IN_DEBUG: Boolean = false

    const val usesScaffoldMessageTemplateToday: Boolean = true
    const val shipsNavigationTemplateYet: Boolean = false
    const val requiresNavigationTemplateBeforeProduction: Boolean = true
    const val nextDestinationMustShowClearlyOnAuto: Boolean = true
    const val trackedBy: String = "ATNA-10"

    const val keepPhoneScreenActiveWhileNavigating: Boolean = true
    const val phoneMayBlankWhenAutoConnected: Boolean = true
    const val releaseHostValidationIsStrict: Boolean = true

    fun isEnabled(
        flags: FeatureFlags,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = AndroidAutoGate.isEnabled(flags, sdkInt)

    fun setEnabled(flags: LocalFeatureFlags, enabled: Boolean) {
        flags.setEnabled(FlagKeys.ANDROID_AUTO, enabled)
    }
}
