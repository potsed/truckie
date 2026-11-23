package com.truckie1.app.ui

/**
 * Text stacks for MapLibre Annotation [org.maplibre.android.plugins.annotation.SymbolManager]
 * labels on the OpenFreeMap Liberty basemap.
 *
 * MapLibre’s default `text-font` is `Open Sans Regular, Arial Unicode MS Regular`.
 * OpenFreeMap only hosts **Noto Sans** under
 * `https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf`. A 404 for the
 * default stack prevents the **entire** symbol (icon + text) from painting —
 * which made W5-8 bridge markers invisible after Jump even when SymbolManagers
 * were bound to the live style.
 *
 * Gherkin: features/bridge_markers.feature
 */
object MapLibreSymbolText {
    /** Exact fontstack directory name on OpenFreeMap’s glyph host. */
    const val OPENFREEMAP_REGULAR: String = "Noto Sans Regular"

    /** Pass to [org.maplibre.android.plugins.annotation.SymbolOptions.withTextFont]. */
    val openFreeMapTextFont: Array<String> = arrayOf(OPENFREEMAP_REGULAR)

    /**
     * When true, icons still draw if text collides or glyphs fail — belt-and-suspenders
     * with an explicit Noto stack.
     */
    const val textOptional: Boolean = true

    /** Catalog flag for tests / docs: clearance labels must not use MapLibre defaults. */
    const val requiresOpenFreeMapCompatibleTextFont: Boolean = true
}
