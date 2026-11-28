package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Owned AU address pack — Truckie's stop-geocode **source of truth** (ATNA-109).
 *
 * Gherkin: features/owned_au_address_pack.feature
 * Plan: docs/au-address-datasource.md
 *
 * **G-NAF data first only** — one G-NAF-backed pack/search path. Do not merge
 * other address datasets. Nominatim = typeahead UI at most until G-NAF resolves
 * the hit (not SoT).
 *
 * National G-NAF is **not** APK-bundled. Bootstrap is an empty schema envelope
 * (or tiny G-NAF sample later); versioned region packs download like bridges.
 *
 * Interim #69 (`ff_gnaf_address_geocode` + QLD sample) stays until this pack path
 * ships a real QLD extract; then that flag evolves into / is superseded by
 * [FLAG_KEY].
 */
object OwnedAddressPack {
    const val FLAG_KEY: String = FlagKeys.OWNED_ADDRESS_PACK

    /** Bootstrap / empty-schema stamp (no national dump). */
    const val BOOTSTRAP_VERSION: String = "0.0.0-sample"

    const val REGION: String = "AU"

    const val ASSET_PATH: String = "addresses/au_sample_addresses.json"

    const val SCHEMA_NOTE: String =
        "JSON primary: manifest + addresses[]. Required per row: id, lat, lon, " +
            "formatted, number, street, locality, state, postcode, precision, " +
            "source{name,url,retrievedAt}. Prefer null over inventing. " +
            "SoT rows use unit/house/building precision."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.OWNED_ADDRESS_PACK)

    /**
     * When the flag is off, owned pack must not be consulted for autocomplete.
     */
    fun shouldConsultPack(flags: FeatureFlags): Boolean = isEnabled(flags)

    fun bootstrapManifest(): AddressPackManifest = AddressPackManifest(
        region = REGION,
        version = BOOTSTRAP_VERSION,
        generatedAt = "2026-11-28",
        source = AddressSource(
            name = "Geoscape G-NAF (scaffold — empty pack)",
            url = "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
            retrievedAt = "2026-11-28",
        ),
        recordCount = 0,
        schemaVersion = AddressPackManifest.SCHEMA_VERSION,
        downloadUrl = null,
    )
}
