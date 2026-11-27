package com.truckie1.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/bridge_pack.feature — OSS must not clobber curated bootstrap.
 * ATNA-72: PackChannel + BridgePackMerge domain policy.
 */
class BridgePackMergeTest {

    private fun bridge(id: String, name: String = id) = BridgePoint(
        id = id,
        name = name,
        latitude = -27.0,
        longitude = 153.0,
        clearanceHeightM = 3.8,
    )

    @Test
    fun packChannelClassifiesSampleOssAndPublished() {
        assertEquals(PackChannel.CURATED_SAMPLE, PackChannel.of("0.7.0-sample"))
        assertEquals(PackChannel.CURATED_SAMPLE, PackChannel.of("0.6.0-sample"))
        assertEquals(
            PackChannel.PUBLIC_OSS,
            PackChannel.of(
                version = AuLowBridgesPack.REMOTE_VERSION,
                downloadUrl = AuLowBridgesPack.DOWNLOAD_URL,
            ),
        )
        assertEquals(PackChannel.PUBLIC_OSS, PackChannel.of("0.1.0"))
        assertEquals(PackChannel.PUBLISHED, PackChannel.of("1.1.0"))
        assertEquals(PackChannel.PUBLISHED, PackChannel.of("2.0.0"))
    }

    @Test
    fun mergeByIdKeepsBaseOnlyRowsAndOverlaysRemote() {
        val base = listOf(
            bridge("qld-story-bridge-sample", "Story"),
            bridge("qld-tmr-1", "TMR keep"),
            bridge("shared-1", "Old shared"),
        )
        val remote = listOf(
            bridge("shared-1", "New shared"),
            bridge("qld-oss-1", "OSS only"),
        )
        val merged = BridgePackMerge.mergeById(base, remote)
        assertEquals(4, merged.size)
        assertEquals("Story", merged.first { it.id == "qld-story-bridge-sample" }.name)
        assertEquals("TMR keep", merged.first { it.id == "qld-tmr-1" }.name)
        assertEquals("New shared", merged.first { it.id == "shared-1" }.name)
        assertEquals("OSS only", merged.first { it.id == "qld-oss-1" }.name)
    }

    @Test
    fun crossChannelIntoCuratedSampleMergesInsteadOfReplace() {
        val curated = listOf(
            bridge("qld-story-bridge-sample"),
            bridge("qld-qr-ascot"),
            bridge("qld-tmr-24215"),
        )
        val thinnerOss = listOf(bridge("qld-oss-1", "OSS Low Bridge"))
        val action = BridgePackMerge.apply(
            currentChannel = PackChannel.CURATED_SAMPLE,
            remoteChannel = PackChannel.PUBLIC_OSS,
            currentBridges = curated,
            remoteBridges = thinnerOss,
        )
        assertTrue(action is BridgePackApplyAction.Merge)
        val bridges = (action as BridgePackApplyAction.Merge).bridges
        assertEquals(4, bridges.size)
        assertTrue(bridges.any { it.id == "qld-story-bridge-sample" })
        assertTrue(bridges.any { it.id == "qld-qr-ascot" })
        assertTrue(bridges.any { it.id == "qld-tmr-24215" })
        assertTrue(bridges.any { it.id == "qld-oss-1" })
    }

    @Test
    fun sameChannelNewerFullyReplaces() {
        val current = listOf(bridge("old-1"), bridge("old-2"))
        val remote = listOf(bridge("new-1"))
        val action = BridgePackMerge.apply(
            currentChannel = PackChannel.PUBLIC_OSS,
            remoteChannel = PackChannel.PUBLIC_OSS,
            currentBridges = current,
            remoteBridges = remote,
        )
        assertTrue(action is BridgePackApplyAction.Replace)
        assertEquals(listOf("new-1"), (action as BridgePackApplyAction.Replace).bridges.map { it.id })
    }

    @Test
    fun publishedIntoCuratedSampleAlsoMerges() {
        val curated = listOf(bridge("qld-story-bridge-sample"), bridge("qld-bcc-1"))
        val remote = listOf(bridge("au-remote-1"))
        val action = BridgePackMerge.apply(
            currentChannel = PackChannel.CURATED_SAMPLE,
            remoteChannel = PackChannel.PUBLISHED,
            currentBridges = curated,
            remoteBridges = remote,
        )
        assertTrue(action is BridgePackApplyAction.Merge)
        val ids = (action as BridgePackApplyAction.Merge).bridges.map { it.id }.toSet()
        assertEquals(setOf("qld-story-bridge-sample", "qld-bcc-1", "au-remote-1"), ids)
    }

    @Test
    fun curatedSampleCannotOverwritePublished() {
        val action = BridgePackMerge.apply(
            currentChannel = PackChannel.PUBLIC_OSS,
            remoteChannel = PackChannel.CURATED_SAMPLE,
            currentBridges = listOf(bridge("oss-1")),
            remoteBridges = listOf(bridge("sample-1")),
        )
        assertTrue(action is BridgePackApplyAction.Refuse)
        assertFalse((action as BridgePackApplyAction.Refuse).reason.isBlank())
    }

    @Test
    fun emptyRemoteBodyIsRefused() {
        val action = BridgePackMerge.apply(
            currentChannel = PackChannel.CURATED_SAMPLE,
            remoteChannel = PackChannel.PUBLIC_OSS,
            currentBridges = listOf(bridge("qld-story-bridge-sample")),
            remoteBridges = emptyList(),
        )
        assertTrue(action is BridgePackApplyAction.Refuse)
    }
}
