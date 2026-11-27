package com.truckie1.app.ui

/**
 * Versioned Australia-first bridge library pack.
 * Long-term source of truth for `ff_bridge_markers` (not the baked-in sample alone).
 * Gherkin: features/bridge_pack.feature
 */
enum class BridgePackSource {
    /** Active data from on-device versioned cache. */
    CACHE,
    /** Bootstrap from `assets/bridges/au_sample_bridges.json` when cache is empty. */
    BUNDLED_SAMPLE,
}

data class BridgePackManifest(
    val region: String,
    val version: String,
    /** HTTPS URL for the bridges JSON body; null means bundled / injected payload. */
    val downloadUrl: String? = null,
) {
    init {
        require(region.isNotBlank()) { "region required" }
        require(version.isNotBlank()) { "version required" }
    }
}

data class BridgePack(
    val manifest: BridgePackManifest,
    val bridges: List<BridgePoint>,
    val source: BridgePackSource,
)

sealed class BridgePackRefreshResult {
    data class Updated(val pack: BridgePack) : BridgePackRefreshResult()
    data class Unchanged(val pack: BridgePack) : BridgePackRefreshResult()
    data class Failed(val reason: String, val fallback: BridgePack) : BridgePackRefreshResult()
}

/**
 * Downloads raw bridges JSON for a manifest. Production will use HTTPS;
 * tests / dogfood inject bytes (no live CDN required in this PR).
 */
fun interface BridgePackDownloader {
    fun download(manifest: BridgePackManifest): Result<ByteArray>
}

/** No network — always fails. Used until a real pack host is configured. */
object NoOpBridgePackDownloader : BridgePackDownloader {
    override fun download(manifest: BridgePackManifest): Result<ByteArray> =
        Result.failure(IllegalStateException("No remote bridge pack host configured"))
}

/**
 * HTTPS downloader for the AU low-bridges Release asset (ATNA-50).
 * Uses [manifest.downloadUrl]; fails closed when the URL is missing.
 */
class UrlBridgePackDownloader(
    private val openStream: (String) -> ByteArray = { url ->
        java.net.URI.create(url).toURL().openStream().use { it.readBytes() }
    },
) : BridgePackDownloader {
    override fun download(manifest: BridgePackManifest): Result<ByteArray> {
        val url = manifest.downloadUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            return Result.failure(IllegalStateException("Bridge pack downloadUrl missing"))
        }
        if (!url.startsWith("https://")) {
            return Result.failure(IllegalStateException("Bridge pack URL must be HTTPS"))
        }
        return runCatching { openStream(url) }
    }
}

/** Test / dogfood downloader that returns a fixed JSON payload. */
class FixedPayloadBridgePackDownloader(
    private val payload: ByteArray,
) : BridgePackDownloader {
    override fun download(manifest: BridgePackManifest): Result<ByteArray> = Result.success(payload)
}

/** Test helper: counts download attempts (local-first / power-efficiency asserts). */
class CountingBridgePackDownloader(
    private val delegate: BridgePackDownloader,
) : BridgePackDownloader {
    var downloadCount: Int = 0
        private set

    override fun download(manifest: BridgePackManifest): Result<ByteArray> {
        downloadCount += 1
        return delegate.download(manifest)
    }
}

/**
 * Local-first pack lifecycle (power efficiency).
 * Gherkin: features/bridge_pack.feature
 *
 * Cold start / Jump / map home read **on-device** data only (bundled asset → disk
 * cache). Network is used solely by [BridgePackRepository.refresh] when a remote
 * manifest advertises a **newer** version. Live OSM/NHVR ingest stays ATNA-11.
 */
object BridgePackLifecycle {
    const val isLocalFirst: Boolean = true
    const val loadActiveUsesNetwork: Boolean = false
    const val refreshOnlyWhenVersionNewer: Boolean = true
    const val seedsBundledSampleToDisk: Boolean = true
}

object BridgePackVersions {
    /**
     * Semver-ish compare: major.minor.patch numeric tuples; non-numeric → string.
     * A published (non-`-sample`) remote is treated as newer than an on-device
     * bootstrap `-sample` stamp for **fetch** gating (e.g. OSS `0.1.0` vs
     * `0.7.0-sample`). Apply/compose is owned by [BridgePackMerge] — cross-channel
     * refresh merges by id and must not clobber curated rows (ATNA-72).
     */
    fun isNewer(candidate: String, current: String): Boolean {
        val candidateSample = candidate.trim().endsWith("-sample")
        val currentSample = current.trim().endsWith("-sample")
        if (currentSample && !candidateSample) return true
        if (!currentSample && candidateSample) return false
        val a = parse(candidate)
        val b = parse(current)
        if (a != null && b != null) {
            for (i in 0 until maxOf(a.size, b.size)) {
                val av = a.getOrElse(i) { 0 }
                val bv = b.getOrElse(i) { 0 }
                if (av != bv) return av > bv
            }
            return false
        }
        return candidate > current
    }

    private fun parse(version: String): List<Int>? {
        // Allow bootstrap stamps like "0.2.0-sample".
        val numeric = version.trim().substringBefore('-')
        val parts = numeric.split('.')
        if (parts.isEmpty()) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
