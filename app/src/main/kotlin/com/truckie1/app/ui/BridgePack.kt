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

/** Test / dogfood downloader that returns a fixed JSON payload. */
class FixedPayloadBridgePackDownloader(
    private val payload: ByteArray,
) : BridgePackDownloader {
    override fun download(manifest: BridgePackManifest): Result<ByteArray> = Result.success(payload)
}

object BridgePackVersions {
    /** Semver-ish compare: major.minor.patch numeric tuples; non-numeric → string. */
    fun isNewer(candidate: String, current: String): Boolean {
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
