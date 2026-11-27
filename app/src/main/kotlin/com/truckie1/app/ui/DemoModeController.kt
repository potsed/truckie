package com.truckie1.app.ui

import com.truckie1.flags.LocalFeatureFlags
import java.util.concurrent.atomic.AtomicInteger

/**
 * Owns Demo mode enable/disable: flips `ff_demo_mode` only.
 *
 * Isolation is entirely [DualModeAppDataSource] swapping Real vs Demo stores —
 * no backup/restore, no [ensureSeededIfDemo], no competing seed paths (ATNA-75).
 *
 * Call [setEnabled] from Settings. UI keys refreshes off [revision].
 *
 * Gherkin: features/demo_mode.feature
 */
class DemoModeController(
    private val flags: LocalFeatureFlags,
) {
    private val revisionCounter = AtomicInteger(0)

    /** Bumps whenever demo mode is toggled — UI keys refreshes off this. */
    fun revision(): Int = revisionCounter.get()

    fun setEnabled(enabled: Boolean) {
        val currentlyOn = DemoModeCatalog.isEnabled(flags)
        if (enabled == currentlyOn) return
        DemoModeCatalog.setEnabled(flags, enabled = enabled)
        revisionCounter.incrementAndGet()
    }
}
