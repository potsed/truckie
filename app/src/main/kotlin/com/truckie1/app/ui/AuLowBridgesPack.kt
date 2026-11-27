package com.truckie1.app.ui

import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys

/**
 * Public AU low-bridges JSON pack — Truckie's **source pack** (ATNA-50).
 *
 * Gherkin: features/au_low_bridges_pack.feature
 *
 * Canonical repo: https://github.com/potsed/au-low-bridges-dataset (JSON only).
 * Shared full schema with the OSS pack — no thinner parallel model.
 * Local-first: [CachedBridgePackRepository.loadActive] never networks;
 * [CachedBridgePackRepository.refresh] downloads only when remote is newer,
 * then [BridgePackMerge] merges into curated bootstrap (ATNA-72 — no clobber).
 *
 * Required row fields: id, lat, lng, clearanceHeightM, source{name,url,retrievedAt}.
 * Prefer null/unknown over inventing. Roadside signs override digital packs.
 */
object AuLowBridgesPack {
    const val FLAG_KEY: String = FlagKeys.AU_LOW_BRIDGES_PACK

    /** First public OSS pack semver (`v0.1.0` release). */
    const val REMOTE_VERSION: String = "0.1.0"

    const val REGION: String = BridgeMarkersCatalog.REGION

    const val REPO_URL: String = "https://github.com/potsed/au-low-bridges-dataset"

    /** GitHub Release asset URL for the Truckie wire array (JSON). */
    const val DOWNLOAD_URL: String =
        "https://github.com/potsed/au-low-bridges-dataset/releases/download/" +
            "v$REMOTE_VERSION/au-low-bridges-$REMOTE_VERSION.json"

    const val SCHEMA_NOTE: String =
        "Shared full schema with potsed/au-low-bridges-dataset: required clearanceHeightM + " +
            "source{name,url,retrievedAt}; roadUnder/roadOver/passage/state/lga/weight/" +
            "signage when known; null over inventing. JSON only."

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.AU_LOW_BRIDGES_PACK)

    fun remoteManifest(): BridgePackManifest = BridgePackManifest(
        region = REGION,
        version = REMOTE_VERSION,
        downloadUrl = DOWNLOAD_URL,
    )

    /**
     * Downloader used when the flag is on. Tests inject [FixedPayloadBridgePackDownloader].
     */
    fun downloaderFor(flags: FeatureFlags): BridgePackDownloader =
        if (isEnabled(flags)) {
            UrlBridgePackDownloader()
        } else {
            NoOpBridgePackDownloader
        }
}

/**
 * Upstream citation for a pack row (required on the OSS / Truckie source pack).
 */
data class BridgeSource(
    val name: String,
    val url: String,
    val retrievedAt: String,
) {
    init {
        require(name.isNotBlank()) { "source.name required" }
        require(url.isNotBlank()) { "source.url required" }
        require(retrievedAt.isNotBlank()) { "source.retrievedAt required" }
    }

    fun citation(): String = "$name — $url"
}
