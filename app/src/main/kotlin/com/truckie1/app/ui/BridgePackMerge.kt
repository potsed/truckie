package com.truckie1.app.ui

/**
 * Pack provenance channel for AU bridge library refresh (ATNA-72).
 *
 * Separates curated bootstrap (`*-sample`) from public OSS and other published
 * packs so [BridgePackMerge] can refuse silent clobber of dogfood rows.
 *
 * Gherkin: features/bridge_pack.feature, features/au_low_bridges_pack.feature
 */
enum class PackChannel {
    /** Bundled curated bootstrap stamp (`0.7.0-sample`, etc.). */
    CURATED_SAMPLE,

    /** Public [AuLowBridgesPack] GitHub Release channel. */
    PUBLIC_OSS,

    /** Other non-sample published packs (semver without `-sample`). */
    PUBLISHED,
    ;

    /** Non-curated channels that may supersede each other by version. */
    fun isPublishedFamily(): Boolean = this != CURATED_SAMPLE

    companion object {
        /**
         * Resolve channel from a version stamp and optional download URL.
         * URL / known OSS semver → [PUBLIC_OSS]; `*-sample` → [CURATED_SAMPLE];
         * otherwise [PUBLISHED].
         */
        fun of(version: String, downloadUrl: String? = null): PackChannel {
            val trimmed = version.trim()
            if (trimmed.endsWith("-sample")) return CURATED_SAMPLE
            val url = downloadUrl?.trim().orEmpty()
            if (url.contains("au-low-bridges-dataset") || url.contains("au-low-bridges-")) {
                return PUBLIC_OSS
            }
            if (trimmed == AuLowBridgesPack.REMOTE_VERSION) return PUBLIC_OSS
            return PUBLISHED
        }
    }
}

/** Outcome of applying a downloaded remote body onto the on-device pack. */
sealed class BridgePackApplyAction {
    data class Replace(val bridges: List<BridgePoint>) : BridgePackApplyAction()
    data class Merge(val bridges: List<BridgePoint>) : BridgePackApplyAction()
    data class Refuse(val reason: String) : BridgePackApplyAction()
}

/**
 * Single refresh compose policy for AU bridge packs (ATNA-72).
 *
 * [BridgePackVersions.isNewer] only gates **fetch**. This object owns **apply**:
 * cross-channel refresh into [PackChannel.CURATED_SAMPLE] merges by bridge id
 * (remote overlays; curated-only ids kept). Same published-family channel may
 * full-replace. Curated sample never overwrites a published cache.
 *
 * Out of scope: GraphHopper injection, BridgePassage / visibility pipeline.
 * Gherkin: features/bridge_pack.feature
 */
object BridgePackMerge {
    const val mergesCrossChannelIntoCurated: Boolean = true
    const val refusesCuratedOverwriteOfPublished: Boolean = true

    fun shouldFetch(remoteVersion: String, currentVersion: String): Boolean =
        BridgePackVersions.isNewer(remoteVersion, currentVersion)

    /**
     * Union by [BridgePoint.id]: remote wins on conflict; base-only rows kept.
     * Stable order: base order first (with overlays), then remote-only appends.
     */
    fun mergeById(
        base: List<BridgePoint>,
        remote: List<BridgePoint>,
    ): List<BridgePoint> {
        val remoteById = LinkedHashMap<String, BridgePoint>(remote.size)
        remote.forEach { remoteById[it.id] = it }
        val seen = HashSet<String>(base.size + remote.size)
        val out = ArrayList<BridgePoint>(base.size + remote.size)
        for (bridge in base) {
            val overlay = remoteById[bridge.id]
            out += overlay ?: bridge
            seen += bridge.id
        }
        for (bridge in remote) {
            if (bridge.id !in seen) {
                out += bridge
                seen += bridge.id
            }
        }
        return out
    }

    fun apply(
        currentChannel: PackChannel,
        remoteChannel: PackChannel,
        currentBridges: List<BridgePoint>,
        remoteBridges: List<BridgePoint>,
    ): BridgePackApplyAction {
        if (remoteBridges.isEmpty()) {
            return BridgePackApplyAction.Refuse("Remote pack JSON produced no bridges")
        }
        if (remoteChannel == PackChannel.CURATED_SAMPLE && currentChannel.isPublishedFamily()) {
            return BridgePackApplyAction.Refuse(
                "Curated sample cannot overwrite ${currentChannel.name} pack",
            )
        }
        if (currentChannel == PackChannel.CURATED_SAMPLE && remoteChannel.isPublishedFamily()) {
            return BridgePackApplyAction.Merge(
                mergeById(base = currentBridges, remote = remoteBridges),
            )
        }
        return BridgePackApplyAction.Replace(remoteBridges)
    }

    fun apply(
        current: BridgePack,
        remote: BridgePackManifest,
        remoteBridges: List<BridgePoint>,
    ): BridgePackApplyAction = apply(
        currentChannel = PackChannel.of(current.manifest.version, current.manifest.downloadUrl),
        remoteChannel = PackChannel.of(remote.version, remote.downloadUrl),
        currentBridges = current.bridges,
        remoteBridges = remoteBridges,
    )
}
