package com.truckie1.app.ui

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Loads AU bridge points for SymbolManager — **local-first** (power efficiency).
 *
 * - [loadActive] never hits the network: reads versioned on-device cache, or seeds
 *   from the bundled AU sample asset onto disk when empty / when bootstrap advances.
 * - [refresh] downloads **only** when [BridgePackVersions.isNewer] says the remote
 *   manifest is newer than the cached stamp. Same/older versions stay on disk.
 *
 * Live OSM/NHVR pack ingest remains ATNA-11. Gherkin: features/bridge_pack.feature
 */
interface BridgePackRepository {
    fun loadActive(): BridgePack
    fun cachedVersion(): String?
    fun refresh(remote: BridgePackManifest): BridgePackRefreshResult
}

class CachedBridgePackRepository(
    private val context: Context,
    private val downloader: BridgePackDownloader = NoOpBridgePackDownloader,
    private val region: String = BridgeMarkersCatalog.REGION,
    private val bootstrapVersion: String = BOOTSTRAP_VERSION,
    private val cacheDir: File = File(context.filesDir, CACHE_DIR_NAME),
) : BridgePackRepository {

    /**
     * On-device only — never calls [BridgePackDownloader].
     * Bundled sample → disk cache; re-seeds when bootstrap `-sample` stamp advances.
     */
    override fun loadActive(): BridgePack {
        val cached = readCache()
        if (cached != null) {
            // Advance baked-in sample: re-seed when bootstrap stamp is newer than cache.
            val cachedVersion = cached.manifest.version
            if (cachedVersion.endsWith("-sample") &&
                BridgePackVersions.isNewer(bootstrapVersion, cachedVersion)
            ) {
                return seedFromBundled()
            }
            return cached
        }
        return seedFromBundled()
    }

    private fun seedFromBundled(): BridgePack {
        val bridges = BridgeMarkersCatalog.loadFromAssets(context)
        val pack = BridgePack(
            manifest = BridgePackManifest(
                region = region,
                version = bootstrapVersion,
                downloadUrl = null,
            ),
            bridges = bridges,
            source = BridgePackSource.BUNDLED_SAMPLE,
        )
        if (bridges.isNotEmpty()) {
            writeCache(pack.manifest, bridges)
        }
        // First load exposes BUNDLED_SAMPLE; later loads read CACHE.
        return pack
    }

    override fun cachedVersion(): String? = readManifestFile()?.optString("version")?.takeIf { it.isNotBlank() }

    override fun refresh(remote: BridgePackManifest): BridgePackRefreshResult {
        val current = loadActive()
        if (remote.region != region) {
            return BridgePackRefreshResult.Failed(
                reason = "Region mismatch: expected $region, got ${remote.region}",
                fallback = current,
            )
        }
        val currentVersion = cachedVersion() ?: current.manifest.version
        if (!BridgePackVersions.isNewer(remote.version, currentVersion)) {
            val fromCache = readCache() ?: current.copy(source = BridgePackSource.CACHE)
            return BridgePackRefreshResult.Unchanged(fromCache)
        }
        val bytes = downloader.download(remote).getOrElse { err ->
            return BridgePackRefreshResult.Failed(
                reason = err.message ?: "Download failed",
                fallback = current,
            )
        }
        val bridges = BridgeMarkersCatalog.parseJson(bytes.toString(Charsets.UTF_8))
        if (bridges.isEmpty()) {
            return BridgePackRefreshResult.Failed(
                reason = "Remote pack JSON produced no bridges",
                fallback = current,
            )
        }
        writeCache(remote, bridges)
        val updated = BridgePack(
            manifest = remote,
            bridges = bridges,
            source = BridgePackSource.CACHE,
        )
        return BridgePackRefreshResult.Updated(updated)
    }

    private fun readCache(): BridgePack? {
        val manifest = readManifestFile() ?: return null
        val version = manifest.optString("version").trim()
        val regionValue = manifest.optString("region").trim().ifBlank { region }
        if (version.isEmpty()) return null
        val bridgesFile = File(cacheDir, BRIDGES_FILE)
        if (!bridgesFile.isFile) return null
        val bridges = BridgeMarkersCatalog.parseJson(bridgesFile.readText())
        if (bridges.isEmpty()) return null
        return BridgePack(
            manifest = BridgePackManifest(
                region = regionValue,
                version = version,
                downloadUrl = manifest.optString("downloadUrl").takeIf { it.isNotBlank() },
            ),
            bridges = bridges,
            source = BridgePackSource.CACHE,
        )
    }

    private fun writeCache(manifest: BridgePackManifest, bridges: List<BridgePoint>) {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val manifestJson = JSONObject()
            .put("region", manifest.region)
            .put("version", manifest.version)
            .put("downloadUrl", manifest.downloadUrl)
        File(cacheDir, MANIFEST_FILE).writeText(manifestJson.toString())
        // Persist the same JSON array shape the asset / remote pack uses.
        val array = org.json.JSONArray()
        bridges.forEach { bridge ->
            val obj = JSONObject()
                .put("id", bridge.id)
                .put("name", bridge.name)
                .put("lat", bridge.latitude)
                .put("lng", bridge.longitude)
            if (bridge.clearanceHeightM != null) {
                obj.put("clearanceHeightM", bridge.clearanceHeightM)
            }
            obj.put("clearanceStatus", bridge.clearanceStatus.wireName())
            if (bridge.clearanceSource != null) {
                obj.put("clearanceSource", bridge.clearanceSource)
            }
            if (bridge.clearanceSourceId != null) {
                obj.put("clearanceSourceId", bridge.clearanceSourceId)
            }
            if (bridge.clearanceRetrievedAt != null) {
                obj.put("clearanceRetrievedAt", bridge.clearanceRetrievedAt)
            }
            if (bridge.clearanceProvider != null) {
                obj.put("clearanceProvider", bridge.clearanceProvider)
            }
            if (bridge.passage != BridgePassage.UNKNOWN) {
                obj.put("passage", bridge.passage.wireName())
            }
            array.put(obj)
        }
        File(cacheDir, BRIDGES_FILE).writeText(array.toString())
    }

    private fun readManifestFile(): JSONObject? {
        val file = File(cacheDir, MANIFEST_FILE)
        if (!file.isFile) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    companion object {
        const val CACHE_DIR_NAME: String = "bridge_packs"
        const val MANIFEST_FILE: String = "manifest.json"
        const val BRIDGES_FILE: String = "bridges.json"
        /**
         * Version stamped when seeding from the baked-in AU sample asset.
         * Bump when `assets/bridges/au_sample_bridges.json` changes so installs
         * with an older `-sample` cache re-seed without a remote fetch
         * (ATNA-41 snap; ATNA-42 verified CBD → 0.4.0-sample;
         * ATNA-44 Queensland Rail SEQ → 0.5.0-sample;
         * ATNA-46 TMR + BCC GIS → 0.6.0-sample;
         * ATNA-48 passage over/under → 0.7.0-sample).
         */
        const val BOOTSTRAP_VERSION: String = "0.7.0-sample"
    }
}
