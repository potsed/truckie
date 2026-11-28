package com.truckie1.app.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Gherkin: features/owned_au_address_pack.feature — local-first pack refresh.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AddressPackRepositoryTest {

    private fun freshRepo(
        downloader: AddressPackDownloader = NoOpAddressPackDownloader,
    ): CachedAddressPackRepository {
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "address_packs_test_${System.nanoTime()}")
        return CachedAddressPackRepository(
            context = context,
            downloader = downloader,
            cacheDir = dir,
        )
    }

    @Test
    fun emptyCacheSeedsFromBundledQldSlice() {
        val repo = freshRepo()
        assertNull(repo.cachedVersion())

        val first = repo.loadActive()
        assertEquals(AddressPackSource.BUNDLED_SAMPLE, first.source)
        assertEquals(OwnedAddressPack.REGION, first.manifest.region)
        assertEquals(OwnedAddressPack.BOOTSTRAP_VERSION, first.manifest.version)
        assertTrue(first.addresses.any { it.id == "GAQLD163067541" })
        assertEquals(OwnedAddressPack.BOOTSTRAP_VERSION, repo.cachedVersion())

        val second = repo.loadActive()
        assertEquals(AddressPackSource.CACHE, second.source)
        assertEquals(first.addresses.size, second.addresses.size)
    }

    @Test
    fun refreshUpdatesCacheWhenRemoteIsNewer() {
        val remoteJson = """
            {
              "manifest": {
                "region": "QLD",
                "version": "0.2.0",
                "generatedAt": "2026-11-28",
                "schemaVersion": 1,
                "recordCount": 1,
                "source": {
                  "name": "Geoscape G-NAF",
                  "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
                  "retrievedAt": "2026-11-28"
                }
              },
              "addresses": [
                {
                  "id": "GAQLD999999999",
                  "lat": -27.5,
                  "lon": 153.0,
                  "formatted": "1 Remote Street Brisbane QLD 4000",
                  "number": "1",
                  "street": "Remote Street",
                  "locality": "Brisbane",
                  "state": "QLD",
                  "postcode": "4000",
                  "precision": "house",
                  "source": {
                    "name": "Geoscape G-NAF",
                    "url": "https://data.gov.au/data/dataset/19432f89-dc3a-4ef3-b943-5326ef1dbecc",
                    "retrievedAt": "2026-11-28"
                  }
                }
              ]
            }
        """.trimIndent().toByteArray()
        val repo = freshRepo(FixedPayloadAddressPackDownloader(remoteJson))
        repo.loadActive()

        val result = repo.refresh(OwnedAddressPack.remoteManifest().copy(version = "0.2.0"))
        assertTrue(result is AddressPackRefreshResult.Updated)
        val updated = (result as AddressPackRefreshResult.Updated).pack
        assertEquals("0.2.0", updated.manifest.version)
        assertEquals(AddressPackSource.CACHE, updated.source)
        assertTrue(updated.addresses.any { it.id == "GAQLD999999999" })
        assertEquals("0.2.0", repo.cachedVersion())

        val reloaded = repo.loadActive()
        assertEquals("0.2.0", reloaded.manifest.version)
        assertEquals(AddressPackSource.CACHE, reloaded.source)
    }

    @Test
    fun loadActiveNeverNetworks() {
        var downloads = 0
        val counting = AddressPackDownloader {
            downloads += 1
            Result.failure(IllegalStateException("network"))
        }
        val repo = freshRepo(counting)
        repo.loadActive()
        repo.loadActive()
        assertEquals(0, downloads)
    }
}
