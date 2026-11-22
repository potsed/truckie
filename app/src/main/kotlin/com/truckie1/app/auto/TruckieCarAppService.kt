package com.truckie1.app.auto

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.truckie1.app.AndroidAutoGate
import com.truckie1.app.TruckieApp

/**
 * Android Auto entry. Host validation + session creation only when API/flag allow.
 */
class TruckieCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        val app = applicationContext as TruckieApp
        check(AndroidAutoGate.isEnabled(app.featureFlags)) {
            "Android Auto disabled (need API ${AndroidAutoGate.MIN_API}+ and ff_android_auto)"
        }
        return TruckieCarSession()
    }
}
