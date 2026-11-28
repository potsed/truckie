package com.truckie1.app.trips

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Owned AU address pack — Truckie's stop-geocode **source of truth** (ATNA-109 / ATNA-111).
 *
 * Gherkin: features/owned_au_address_pack.feature
 * Plan: docs/au-address-datasource.md
 * ETL: docs/owned-address-pack.md + tools/gnaf-etl/
 *
 * **G-NAF data first only** — one G-NAF-backed pack/search path. Do not merge
 * other address datasets. Nominatim = typeahead UI at most until G-NAF resolves
 * the hit (not SoT).
 *
 * National G-NAF is **not** APK-bundled. Bootstrap is a QLD Kallangur dogfood
 * slice; versioned region packs download like bridges.
 */
object OwnedAddressPack {
    const val FLAG_KEY: String = FlagKeys.OWNED_ADDRESS_PACK

    /** Bootstrap / QLD Kallangur dogfood stamp (ATNA-111). */
    const val BOOTSTRAP_VERSION: String = "0.1.0-kallangur-sample"

    /** First published remote QLD pack semver (attach Release asset when publishing). */
    const val REMOTE_VERSION: String = "0.1.0"

    const val REGION: String = "QLD"

    const val ASSET_PATH: String = "addresses/au_sample_addresses.json"

    const val REPO_URL: String = "https://github.com/potsed/truckie"

    /**
     * GitHub Release asset URL for the QLD G-NAF pack envelope (JSON).
     * Publish via `tools/gnaf-etl` then attach as
     * `qld-gnaf-$REMOTE_VERSION.json` on tag `address-pack-v$REMOTE_VERSION`.
     */
    const val DOWNLOAD_URL: String =
        "$REPO_URL/releases/download/address-pack-v$REMOTE_VERSION/" +
            "qld-gnaf-$REMOTE_VERSION.json"

    const val SCHEMA_NOTE: String =
        "JSON primary: manifest + addresses[]. Required per row: id, lat, lon, " +
            "formatted, number, street, locality, state, postcode, precision, " +
            "source{name,url,retrievedAt}. Prefer null over inventing. " +
            "SoT rows use unit/house/building precision. QLD-first G-NAF only."

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
            name = "Geoscape G-NAF",
            url = "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
            retrievedAt = "2026-11-28",
        ),
        recordCount = 0,
        schemaVersion = AddressPackManifest.SCHEMA_VERSION,
        downloadUrl = null,
    )

    fun remoteManifest(): AddressPackManifest = AddressPackManifest(
        region = REGION,
        version = REMOTE_VERSION,
        generatedAt = "2026-11-28",
        source = AddressSource(
            name = "Geoscape G-NAF",
            url = "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
            retrievedAt = "2026-11-28",
        ),
        recordCount = 0,
        schemaVersion = AddressPackManifest.SCHEMA_VERSION,
        downloadUrl = DOWNLOAD_URL,
    )

    fun downloaderFor(flags: FeatureFlags): AddressPackDownloader =
        if (isEnabled(flags)) {
            UrlAddressPackDownloader()
        } else {
            NoOpAddressPackDownloader
        }
}
