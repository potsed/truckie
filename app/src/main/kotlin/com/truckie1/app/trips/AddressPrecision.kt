package com.truckie1.app.trips

/**
 * How precisely a verified [AddressSuggestion] locates a stop.
 * Navigate must prefer [UNIT] / [HOUSE] / [BUILDING] over [STREET].
 */
enum class AddressPrecision {
    /** G-NAF (or equivalent) unit / flat point. */
    UNIT,
    /** Housenumber / street-number point (G-NAF or Nominatim house_number). */
    HOUSE,
    /** Named building / complex / landuse with housenumber (OSM). */
    BUILDING,
    /** Highway / road centroid — not precise enough for a typed premise. */
    STREET,
    UNKNOWN,
}
