package com.truckie1.app.ui

/**
 * Pure gate for MapLibreHost style loading.
 *
 * AndroidView `update` runs on every Compose recomposition (location fixes, jump
 * token, follow state). Calling [org.maplibre.android.maps.MapLibreMap.setStyle]
 * more than once before the first load finishes — or binding SymbolManagers to a
 * style that is no longer `map.style` — orphans annotations so W5-8 bridge markers
 * never appear even after Jump to sample bridges.
 */
enum class StyleEnsureAction {
    /** Invoke setStyle once; queue this callback for the load result. */
    START_LOAD,

    /** A setStyle is already in flight — queue only, do not start another. */
    ENQUEUE_WHILE_LOADING,

    /** Reuse map.style; optionally recreate managers if identity drifted. */
    REUSE_EXISTING,
}

data class StyleEnsureDecision(
    val action: StyleEnsureAction,
    val recreateManagers: Boolean,
)

object MapLibreStyleLifecycle {
    fun decide(
        styleReady: Boolean,
        styleLoading: Boolean,
        hasExistingFullyLoadedStyle: Boolean,
        boundStyleMatchesExisting: Boolean,
    ): StyleEnsureDecision {
        if (styleReady && hasExistingFullyLoadedStyle) {
            return StyleEnsureDecision(
                action = StyleEnsureAction.REUSE_EXISTING,
                recreateManagers = !boundStyleMatchesExisting,
            )
        }
        if (styleLoading) {
            return StyleEnsureDecision(
                action = StyleEnsureAction.ENQUEUE_WHILE_LOADING,
                recreateManagers = false,
            )
        }
        return StyleEnsureDecision(
            action = StyleEnsureAction.START_LOAD,
            recreateManagers = false,
        )
    }

    /**
     * When a SymbolManager/LineManager's layer is absent from the live style,
     * the manager was orphaned — drop and recreate before create().
     */
    fun mustRecreateAnnotationManager(
        hasManager: Boolean,
        styleContainsManagerLayer: Boolean,
    ): Boolean = hasManager && !styleContainsManagerLayer
}
