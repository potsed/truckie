package com.truckie1.app.ui

import com.truckie1.flags.FlagKeys
import com.truckie1.flags.LocalFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Gherkin: features/au_low_bridges_pack.feature — shared OSS source pack schema.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuLowBridgesPackTest {

    @Test
    fun flagKeyAndRemoteManifest() {
        assertEquals("ff_au_low_bridges_pack", FlagKeys.AU_LOW_BRIDGES_PACK)
        assertEquals("ff_au_low_bridges_pack", AuLowBridgesPack.FLAG_KEY)
        assertEquals("0.1.0", AuLowBridgesPack.REMOTE_VERSION)
        assertTrue(AuLowBridgesPack.DOWNLOAD_URL.startsWith("https://"))
        assertTrue(AuLowBridgesPack.DOWNLOAD_URL.contains("au-low-bridges-dataset"))
        assertTrue(AuLowBridgesPack.DOWNLOAD_URL.contains("au-low-bridges-0.1.0.json"))
        val manifest = AuLowBridgesPack.remoteManifest()
        assertEquals("AU", manifest.region)
        assertEquals("0.1.0", manifest.version)
        assertEquals(AuLowBridgesPack.DOWNLOAD_URL, manifest.downloadUrl)
        assertTrue(
            "Public 0.1.0 must fetch/merge over bootstrap 0.7.0-sample",
            BridgePackVersions.isNewer(
                AuLowBridgesPack.REMOTE_VERSION,
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
            ),
        )
        assertFalse(
            BridgePackVersions.isNewer(
                CachedBridgePackRepository.BOOTSTRAP_VERSION,
                AuLowBridgesPack.REMOTE_VERSION,
            ),
        )
        assertEquals(
            PackChannel.CURATED_SAMPLE,
            PackChannel.of(CachedBridgePackRepository.BOOTSTRAP_VERSION),
        )
        assertEquals(
            PackChannel.PUBLIC_OSS,
            PackChannel.of(AuLowBridgesPack.REMOTE_VERSION, AuLowBridgesPack.DOWNLOAD_URL),
        )
    }

    @Test
    fun downloaderIsNoOpWhenFlagOff() {
        val flags = LocalFeatureFlags()
        assertFalse(AuLowBridgesPack.isEnabled(flags))
        assertSame(NoOpBridgePackDownloader, AuLowBridgesPack.downloaderFor(flags))
    }

    @Test
    fun downloaderIsHttpsWhenFlagOn() {
        val flags = LocalFeatureFlags(mapOf(FlagKeys.AU_LOW_BRIDGES_PACK to true))
        assertTrue(AuLowBridgesPack.isEnabled(flags))
        assertTrue(AuLowBridgesPack.downloaderFor(flags) is UrlBridgePackDownloader)
    }

    @Test
    fun parsesSharedPackSchemaWithoutInventing() {
        val json = """
            [
              {
                "id": "qld-tmr-24215",
                "name": "Pacific Hwy/Gold Coast Hwy Underpass",
                "lat": -27.925744,
                "lng": 153.332819,
                "state": "QLD",
                "lga": null,
                "roadUnder": "Pacific Highway (Pacific Motorway)",
                "roadOver": null,
                "clearanceHeightM": 4.3,
                "passage": "under",
                "clearanceStatus": "verified",
                "clearanceSourceId": "TMR-24215",
                "clearanceProvider": "tmr_structure_clearances",
                "weightRestriction": null,
                "signageNote": "Physical roadside signs override this pack.",
                "source": {
                  "name": "TMR Structure Clearances in Queensland",
                  "url": "https://www.data.qld.gov.au/dataset/structure-clearances-queensland",
                  "retrievedAt": "2026-11-23"
                }
              }
            ]
        """.trimIndent()
        val bridges = BridgeMarkersCatalog.parseJson(json)
        assertEquals(1, bridges.size)
        val b = bridges[0]
        assertEquals(4.3, b.clearanceHeightM!!, 0.0)
        assertEquals("QLD", b.state)
        assertNull(b.lga)
        assertEquals("Pacific Highway (Pacific Motorway)", b.roadUnder)
        assertNull(b.roadOver)
        assertNull(b.weightRestriction)
        assertEquals(BridgePassage.UNDER, b.passage)
        assertEquals("TMR Structure Clearances in Queensland", b.source!!.name)
        assertTrue(b.source!!.url.startsWith("https://"))
        assertEquals("2026-11-23", b.source!!.retrievedAt)
        assertEquals("2026-11-23", b.clearanceRetrievedAt)
        assertTrue(b.clearanceSource!!.contains("TMR Structure Clearances"))
        assertTrue(b.signageNote!!.contains("roadside signs"))
    }

    @Test
    fun urlDownloaderRequiresHttps() {
        val downloader = UrlBridgePackDownloader()
        val bad = downloader.download(
            BridgePackManifest(region = "AU", version = "1.0.0", downloadUrl = "http://example.invalid/x"),
        )
        assertTrue(bad.isFailure)
        val missing = downloader.download(
            BridgePackManifest(region = "AU", version = "1.0.0", downloadUrl = null),
        )
        assertTrue(missing.isFailure)
    }

    @Test
    fun refreshFromOssPackUpdatesCacheWhenNewer() {
        val payload = """
            [
              {
                "id": "qld-oss-1",
                "name": "OSS Low Bridge",
                "lat": -27.5,
                "lng": 153.0,
                "state": "QLD",
                "roadUnder": "Example Road",
                "clearanceHeightM": 3.9,
                "passage": "under",
                "clearanceStatus": "verified",
                "source": {
                  "name": "Test Source",
                  "url": "https://example.invalid/source",
                  "retrievedAt": "2026-11-23"
                }
              }
            ]
        """.trimIndent().toByteArray()
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "au_low_bridges_${System.nanoTime()}")
        val counting = CountingBridgePackDownloader(FixedPayloadBridgePackDownloader(payload))
        val repo = CachedBridgePackRepository(
            context = context,
            downloader = counting,
            cacheDir = dir,
        )
        val seeded = repo.loadActive()
        val curatedCount = seeded.bridges.size
        assertEquals(CachedBridgePackRepository.BOOTSTRAP_VERSION, repo.cachedVersion())
        assertEquals(0, counting.downloadCount)

        val result = repo.refresh(AuLowBridgesPack.remoteManifest())
        assertTrue(result is BridgePackRefreshResult.Updated)
        assertEquals(1, counting.downloadCount)
        assertEquals("0.1.0", repo.cachedVersion())
        val active = repo.loadActive()
        assertEquals("0.1.0", active.manifest.version)
        // Merge preserves curated bootstrap; OSS row overlays by id.
        assertTrue(active.bridges.size >= curatedCount)
        val oss = active.bridges.first { it.id == "qld-oss-1" }
        assertEquals("Example Road", oss.roadUnder)
        assertEquals("Test Source", oss.source!!.name)
        assertTrue(active.bridges.any { it.id == "qld-story-bridge-sample" })
        // loadActive stays local-first
        assertEquals(1, counting.downloadCount)
    }
}
