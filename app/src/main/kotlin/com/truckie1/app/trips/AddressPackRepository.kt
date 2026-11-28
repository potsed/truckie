package com.truckie1.app.trips

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Local-first G-NAF address pack lifecycle (ATNA-111).
 *
 * - [loadActive] never networks: bundled QLD slice → disk cache.
 * - [refresh] downloads only when remote version is newer (bridges pattern).
 *
 * Gherkin: features/owned_au_address_pack.feature
 */
interface AddressPackRepository {
    fun loadActive(): AddressPack
    fun cachedVersion(): String?
    fun refresh(remote: AddressPackManifest): AddressPackRefreshResult
}

sealed class AddressPackRefreshResult {
    data class Updated(val pack: AddressPack) : AddressPackRefreshResult()
    data class Unchanged(val pack: AddressPack) : AddressPackRefreshResult()
    data class Failed(val reason: String, val fallback: AddressPack) : AddressPackRefreshResult()
}

fun interface AddressPackDownloader {
    fun download(manifest: AddressPackManifest): Result<ByteArray>
}

object NoOpAddressPackDownloader : AddressPackDownloader {
    override fun download(manifest: AddressPackManifest): Result<ByteArray> =
        Result.failure(IllegalStateException("No remote address pack host configured"))
}

class UrlAddressPackDownloader(
    private val openStream: (String) -> ByteArray = { url ->
        java.net.URI.create(url).toURL().openStream().use { it.readBytes() }
    },
) : AddressPackDownloader {
    override fun download(manifest: AddressPackManifest): Result<ByteArray> {
        val url = manifest.downloadUrl?.trim().orEmpty()
        if (url.isEmpty()) {
            return Result.failure(IllegalStateException("Address pack downloadUrl missing"))
        }
        if (!url.startsWith("https://")) {
            return Result.failure(IllegalStateException("Address pack URL must be HTTPS"))
        }
        return runCatching { openStream(url) }
    }
}

class FixedPayloadAddressPackDownloader(
    private val payload: ByteArray,
) : AddressPackDownloader {
    override fun download(manifest: AddressPackManifest): Result<ByteArray> =
        Result.success(payload)
}

object AddressPackVersions {
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
        val numeric = version.trim().substringBefore('-')
        val parts = numeric.split('.')
        if (parts.isEmpty()) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }
}

class CachedAddressPackRepository(
    private val context: Context,
    private val downloader: AddressPackDownloader = NoOpAddressPackDownloader,
    private val region: String = OwnedAddressPack.REGION,
    private val bootstrapVersion: String = OwnedAddressPack.BOOTSTRAP_VERSION,
    private val assetPath: String = OwnedAddressPack.ASSET_PATH,
    private val cacheDir: File = File(context.filesDir, CACHE_DIR_NAME),
) : AddressPackRepository {

    override fun loadActive(): AddressPack {
        val cached = readCache()
        if (cached != null) {
            val cachedVersion = cached.manifest.version
            if (cachedVersion.endsWith("-sample") &&
                AddressPackVersions.isNewer(bootstrapVersion, cachedVersion)
            ) {
                return seedFromBundled()
            }
            return cached
        }
        return seedFromBundled()
    }

    private fun seedFromBundled(): AddressPack {
        val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val parsed = AddressPackJson.parse(json)
        val pack = parsed.copy(
            manifest = parsed.manifest.copy(version = bootstrapVersion),
            source = AddressPackSource.BUNDLED_SAMPLE,
        )
        if (pack.addresses.isNotEmpty()) {
            writeCache(pack)
        }
        return pack
    }

    override fun cachedVersion(): String? =
        readManifestFile()?.optString("version")?.takeIf { it.isNotBlank() }

    override fun refresh(remote: AddressPackManifest): AddressPackRefreshResult {
        val current = loadActive()
        if (remote.region != region) {
            return AddressPackRefreshResult.Failed(
                reason = "Region mismatch: expected $region, got ${remote.region}",
                fallback = current,
            )
        }
        val currentVersion = cachedVersion() ?: current.manifest.version
        if (!AddressPackVersions.isNewer(remote.version, currentVersion)) {
            val fromCache = readCache() ?: current.copy(source = AddressPackSource.CACHE)
            return AddressPackRefreshResult.Unchanged(fromCache)
        }
        val bytes = downloader.download(remote).getOrElse { err ->
            return AddressPackRefreshResult.Failed(
                reason = err.message ?: "Download failed",
                fallback = current,
            )
        }
        val remotePack = AddressPackJson.parse(bytes.toString(Charsets.UTF_8)).copy(
            manifest = remote,
            source = AddressPackSource.CACHE,
        )
        writeCache(remotePack)
        return AddressPackRefreshResult.Updated(remotePack)
    }

    private fun readCache(): AddressPack? {
        val manifestObj = readManifestFile() ?: return null
        val version = manifestObj.optString("version").trim()
        if (version.isEmpty()) return null
        val bodyFile = File(cacheDir, ADDRESSES_FILE)
        if (!bodyFile.isFile) return null
        val pack = AddressPackJson.parse(bodyFile.readText())
        return pack.copy(
            manifest = pack.manifest.copy(
                version = version,
                region = manifestObj.optString("region").trim().ifBlank { region },
                downloadUrl = manifestObj.optString("downloadUrl").takeIf { it.isNotBlank() },
            ),
            source = AddressPackSource.CACHE,
        )
    }

    private fun writeCache(pack: AddressPack) {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val manifestJson = JSONObject()
            .put("region", pack.manifest.region)
            .put("version", pack.manifest.version)
            .put("downloadUrl", pack.manifest.downloadUrl)
        File(cacheDir, MANIFEST_FILE).writeText(manifestJson.toString())
        File(cacheDir, ADDRESSES_FILE).writeText(AddressPackJson.stringify(pack))
    }

    private fun readManifestFile(): JSONObject? {
        val file = File(cacheDir, MANIFEST_FILE)
        if (!file.isFile) return null
        return runCatching { JSONObject(file.readText()) }.getOrNull()
    }

    companion object {
        const val CACHE_DIR_NAME: String = "address_packs"
        const val MANIFEST_FILE: String = "manifest.json"
        const val ADDRESSES_FILE: String = "addresses.json"
    }
}
