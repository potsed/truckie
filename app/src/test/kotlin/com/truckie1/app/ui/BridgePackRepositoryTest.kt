package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Gherkin: features/bridge_pack.feature — versioned download + local cache.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BridgePackRepositoryTest {

    private fun freshRepo(
        downloader: BridgePackDownloader = NoOpBridgePackDownloader,
    ): CachedBridgePackRepository {
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "bridge_packs_test_${System.nanoTime()}")
        return CachedBridgePackRepository(
            context = context,
            downloader = downloader,
            cacheDir = dir,
        )
    }

    @Test
    fun emptyCacheSeedsFromBundledSampleThenCaches() {
        val repo = freshRepo()
        assertEquals(null, repo.cachedVersion())

        val first = repo.loadActive()
        assertEquals(BridgePackSource.BUNDLED_SAMPLE, first.source)
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, first.manifest.version)
        assertEquals(BridgeMarkersCatalog.REGION, first.manifest.region)
        assertTrue(first.bridges.size >= 3)
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, repo.cachedVersion())

        val second = repo.loadActive()
        assertEquals(BridgePackSource.CACHE, second.source)
        assertEquals(first.bridges.size, second.bridges.size)
        assertEquals(first.bridges.first().id, second.bridges.first().id)
    }

    @Test
    fun preferCacheOverBundledWhenPresent() {
        val repo = freshRepo()
        repo.loadActive() // seed
        val active = repo.loadActive()
        assertEquals(BridgePackSource.CACHE, active.source)
        assertFalse(active.source == BridgePackSource.BUNDLED_SAMPLE)
    }

    @Test
    fun newerBootstrapSampleReseedsStaleCache() {
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "bridge_packs_reseed_${System.nanoTime()}")
        val stale = CachedBridgePackRepository(
            context = context,
            cacheDir = dir,
            bootstrapVersion = "0.1.0-sample",
        )
        stale.loadActive()
        assertEquals("0.1.0-sample", stale.cachedVersion())

        val newer = CachedBridgePackRepository(
            context = context,
            cacheDir = dir,
            bootstrapVersion = CachedBridgePackRepository.BOOTSTRAP_VERSION,
        )
        val reseeded = newer.loadActive()
        assertEquals(BridgePackSource.BUNDLED_SAMPLE, reseeded.source)
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, reseeded.manifest.version)
        assertTrue(reseeded.bridges.size >= 3)
    }

    @Test
    fun refreshUpdatesCacheWhenRemoteIsNewer() {
        val payload = """
            [
              {"id":"au-remote-1","name":"Remote Low Bridge","lat":-27.5,"lng":153.0,"clearanceHeightM":3.5},
              {"id":"au-remote-2","name":"Remote Overpass","lat":-33.9,"lng":151.2}
            ]
        """.trimIndent().toByteArray()
        val repo = freshRepo(FixedPayloadBridgePackDownloader(payload))
        repo.loadActive()
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, repo.cachedVersion())

        val result = repo.refresh(
            BridgePackManifest(
                region = "AU",
                version = "1.1.0",
                downloadUrl = "https://example.invalid/au-bridges-1.1.0.json",
            ),
        )
        assertTrue(result is BridgePackRefreshResult.Updated)
        val updated = (result as BridgePackRefreshResult.Updated).pack
        assertEquals("1.1.0", updated.manifest.version)
        assertEquals(BridgePackSource.CACHE, updated.source)
        assertEquals(2, updated.bridges.size)
        assertEquals("au-remote-1", updated.bridges[0].id)
        assertEquals("3.5 m", BridgeMarkersCatalog.clearanceLabel(updated.bridges[0]))
        assertEquals("1.1.0", repo.cachedVersion())

        val reloaded = repo.loadActive()
        assertEquals("1.1.0", reloaded.manifest.version)
        assertEquals(2, reloaded.bridges.size)
    }

    @Test
    fun refreshKeepsCacheWhenRemoteIsNotNewer() {
        val payload = """[{"id":"x","name":"X","lat":-27.0,"lng":153.0}]""".toByteArray()
        val repo = freshRepo(FixedPayloadBridgePackDownloader(payload))
        repo.loadActive()
        repo.refresh(BridgePackManifest(region = "AU", version = "1.1.0"))
        val result = repo.refresh(BridgePackManifest(region = "AU", version = "1.0.0"))
        assertTrue(result is BridgePackRefreshResult.Unchanged)
        assertEquals("1.1.0", repo.cachedVersion())
        assertEquals(1, (result as BridgePackRefreshResult.Unchanged).pack.bridges.size)
    }

    @Test
    fun versionCompareTreatsSemverAsNewer() {
        assertTrue(BridgePackVersions.isNewer("1.1.0", "1.0.0"))
        assertTrue(BridgePackVersions.isNewer("2.0.0", "1.9.9"))
        assertFalse(BridgePackVersions.isNewer("1.0.0", "1.1.0"))
        assertFalse(BridgePackVersions.isNewer("1.1.0", "1.1.0"))
    }

    @Test
    fun refreshFailsGracefullyWhenDownloaderHasNoHost() {
        val repo = freshRepo(NoOpBridgePackDownloader)
        val seeded = repo.loadActive()
        assertNotNull(seeded)
        val result = repo.refresh(BridgePackManifest(region = "AU", version = "9.0.0"))
        assertTrue(result is BridgePackRefreshResult.Failed)
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, repo.cachedVersion())
    }

    @Test
    fun localFirstLifecycleNeverNetworksOnLoadActive() {
        assertTrue(BridgePackLifecycle.isLocalFirst)
        assertFalse(BridgePackLifecycle.loadActiveUsesNetwork)
        assertTrue(BridgePackLifecycle.refreshOnlyWhenVersionNewer)
        assertTrue(BridgePackLifecycle.seedsBundledSampleToDisk)
        assertEquals("0.4.0-sample", CachedBridgePackRepository.BOOTSTRAP_VERSION)

        val counting = CountingBridgePackDownloader(
            FixedPayloadBridgePackDownloader(
                """[{"id":"x","name":"X","lat":-27.0,"lng":153.0}]""".toByteArray(),
            ),
        )
        val repo = freshRepo(counting)
        repeat(3) { repo.loadActive() }
        assertEquals(
            "loadActive must stay on-device (bundled/cache) — no pack download",
            0,
            counting.downloadCount,
        )
        assertNotNull(repo.cachedVersion())
    }

    @Test
    fun refreshSkipsDownloadWhenRemoteVersionIsNotNewer() {
        val counting = CountingBridgePackDownloader(
            FixedPayloadBridgePackDownloader(
                """[{"id":"x","name":"X","lat":-27.0,"lng":153.0}]""".toByteArray(),
            ),
        )
        val repo = freshRepo(counting)
        repo.loadActive()
        val first = repo.refresh(
            BridgePackManifest(region = "AU", version = "1.1.0", downloadUrl = "https://example.invalid/a"),
        )
        assertTrue(first is BridgePackRefreshResult.Updated)
        assertEquals(1, counting.downloadCount)

        val second = repo.refresh(
            BridgePackManifest(region = "AU", version = "1.1.0", downloadUrl = "https://example.invalid/a"),
        )
        assertTrue(second is BridgePackRefreshResult.Unchanged)
        assertEquals(
            "Same version must not re-fetch (power efficiency)",
            1,
            counting.downloadCount,
        )

        val older = repo.refresh(
            BridgePackManifest(region = "AU", version = "1.0.0", downloadUrl = "https://example.invalid/b"),
        )
        assertTrue(older is BridgePackRefreshResult.Unchanged)
        assertEquals(1, counting.downloadCount)
    }
}
