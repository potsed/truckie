package com.truckie1.app

import android.os.Build
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Android Auto is gated to API 23+ and the ff_android_auto flag.
 */
object AndroidAutoGate {
    const val MIN_API = 23

    fun isSupportedApi(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= MIN_API

    fun isEnabled(
        flags: FeatureFlags,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = flags.isEnabled(FlagKeys.ANDROID_AUTO) && isSupportedApi(sdkInt)
}
