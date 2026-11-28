package com.truckie1.app.ui

/**
 * Pure gate for MapLibreHost style loading.
 *
 * AndroidView `update` runs on every Compose recomposition (location fixes,
 * follow state). Calling [org.maplibre.android.maps.MapLibreMap.setStyle]
 * more than once before the first load finishes — or binding overlays to a
 * style that is no longer `map.style` — orphans GeoJSON layers so W5-8 bridge
 * markers never appear.
 *
 * ATNA-117: same coalescing / identity rules; overlays are style layers
 * (not Annotation managers).
 */
enum class StyleEnsureAction {
    /** Invoke setStyle once; queue this callback for the load result. */
    START_LOAD,

    /** A setStyle is already in flight — queue only, do not start another. */
    ENQUEUE_WHILE_LOADING,

    /** Reuse map.style; optionally recreate overlays if identity drifted. */
    REUSE_EXISTING,
}

data class StyleEnsureDecision(
    val action: StyleEnsureAction,
    val recreateManagers: Boolean,
) {
    /** Alias — overlays recreate when style identity drifts (ATNA-117). */
    val recreateOverlays: Boolean get() = recreateManagers
}

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
     * When a style-layer overlay's layer is absent from the live style,
     * the overlay was orphaned — drop tracking and recreate before setGeoJson.
     */
    fun mustRecreateOverlay(
        hasBoundOverlay: Boolean,
        styleContainsLayer: Boolean,
    ): Boolean = hasBoundOverlay && !styleContainsLayer

    /**
     * @deprecated Use [mustRecreateOverlay] — Annotation managers deleted (ATNA-117).
     */
    @Deprecated(
        message = "Use mustRecreateOverlay",
        replaceWith = ReplaceWith(
            "mustRecreateOverlay(hasBoundOverlay, styleContainsLayer)",
            "com.truckie1.app.ui.MapLibreStyleLifecycle",
        ),
    )
    fun mustRecreateAnnotationManager(
        hasManager: Boolean,
        styleContainsManagerLayer: Boolean,
    ): Boolean = mustRecreateOverlay(
        hasBoundOverlay = hasManager,
        styleContainsLayer = styleContainsManagerLayer,
    )
}
