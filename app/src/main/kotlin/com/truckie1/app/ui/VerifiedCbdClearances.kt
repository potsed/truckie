package com.truckie1.app.ui

import android.content.Context
import com.truckie1.flags.FeatureFlags
import com.truckie1.flags.FlagKeys
import org.json.JSONObject

/**
 * Verified Brisbane CBD sample clearances (ATNA-42).
 *
 * Gherkin: features/verified_cbd_clearances.feature
 *
 * Road vehicle vertical clearance only — never MSQ vessel air draft above HAT.
 * When no signed road clearance is published for a crossing, omit height and
 * mark [BridgeClearanceStatus.UNKNOWN] or [BridgeClearanceStatus.NOT_APPLICABLE].
 */
enum class BridgeClearanceStatus {
    /** Signed / published road clearance from an authoritative AU source. */
    VERIFIED,
    /** Real crossing, but no published road vertical clearance found. */
    UNKNOWN,
    /** Structure is not a vehicle height constraint (e.g. pedestrian/cycle). */
    NOT_APPLICABLE,
    ;

    fun wireName(): String = name.lowercase()

    companion object {
        fun fromWire(raw: String?): BridgeClearanceStatus {
            val key = raw?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.wireName() == key } ?: UNKNOWN
        }
    }
}

data class BridgePackProvenance(
    val retrievedAt: String,
    val sources: List<String>,
    val clearancePolicy: String,
)

object VerifiedCbdClearances {
    const val FLAG_KEY: String = FlagKeys.VERIFIED_CBD_CLEARANCES
    const val META_ASSET: String = "bridges/au_sample_bridges_meta.json"
    /** Retrieval date for CBD sample verification (Australia/Brisbane). */
    const val RETRIEVED_AT: String = "2026-11-23"

    fun isEnabled(flags: FeatureFlags): Boolean =
        flags.isEnabled(FlagKeys.VERIFIED_CBD_CLEARANCES)

    /**
     * When the flag is on, only [BridgeClearanceStatus.VERIFIED] heights may
     * appear as map labels — unknown / not_applicable never invent a figure.
     */
    fun allowsHeightLabel(bridge: BridgePoint, flags: FeatureFlags): Boolean {
        if (bridge.clearanceHeightM == null) return false
        if (!isEnabled(flags)) return true
        return bridge.clearanceStatus == BridgeClearanceStatus.VERIFIED
    }

    fun loadPackMetadata(context: Context): BridgePackProvenance? =
        runCatching {
            context.assets.open(META_ASSET).bufferedReader().use { reader ->
                parseMetadata(reader.readText())
            }
        }.getOrNull()

    fun parseMetadata(raw: String): BridgePackProvenance? =
        runCatching {
            val obj = JSONObject(raw)
            val sourcesJson = obj.optJSONArray("sources") ?: return null
            val sources = buildList {
                for (i in 0 until sourcesJson.length()) {
                    val s = sourcesJson.optString(i).trim()
                    if (s.isNotEmpty()) add(s)
                }
            }
            val policy = obj.optString("clearancePolicy").trim()
            val retrieved = obj.optString("retrievedAt").trim()
            if (retrieved.isEmpty() || sources.isEmpty() || policy.isEmpty()) return null
            BridgePackProvenance(
                retrievedAt = retrieved,
                sources = sources,
                clearancePolicy = policy,
            )
        }.getOrNull()
}
