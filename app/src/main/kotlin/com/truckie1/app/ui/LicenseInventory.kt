package com.truckie1.app.ui

import android.content.Context
import org.json.JSONObject

/**
 * Curated third-party licence inventory (ATNA-116).
 *
 * Machine-readable SoT: [LicensesCatalog.INVENTORY_ASSET_PATH].
 * The Licenses UI renders from this model — do not hardcode a parallel list
 * only in Compose.
 */
data class LicenseEntry(
    val id: String,
    val name: String,
    val license: String,
    val copyright: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val component: String? = null,
    val version: String? = null,
    val attributionRequired: Boolean = false,
)

data class LicenseInventory(
    val version: Int,
    val entries: List<LicenseEntry>,
    val note: String? = null,
) {
    companion object {
        fun loadFromAssets(context: Context): LicenseInventory {
            val path = LicensesCatalog.INVENTORY_ASSET_PATH
            val json = context.assets.open(path).bufferedReader().use { it.readText() }
            return parseJson(json)
        }

        fun parseJson(json: String): LicenseInventory {
            val root = JSONObject(json)
            val array = root.optJSONArray("entries") ?: org.json.JSONArray()
            val entries = buildList {
                for (i in 0 until array.length()) {
                    val row = array.getJSONObject(i)
                    add(
                        LicenseEntry(
                            id = row.getString("id"),
                            name = row.getString("name"),
                            license = row.getString("license"),
                            copyright = optNullableString(row, "copyright"),
                            url = optNullableString(row, "url"),
                            notes = optNullableString(row, "notes"),
                            component = optNullableString(row, "component"),
                            version = optNullableString(row, "version"),
                            attributionRequired = row.optBoolean("attributionRequired", false),
                        ),
                    )
                }
            }
            return LicenseInventory(
                version = root.optInt("version", 1),
                entries = entries,
                note = optNullableString(root, "note"),
            )
        }

        /**
         * org.json [JSONObject.optString] returns the literal `"null"` for JSON null —
         * treat JSON null / blank as absent.
         */
        private fun optNullableString(obj: JSONObject, key: String): String? {
            if (!obj.has(key) || obj.isNull(key)) return null
            return obj.getString(key).takeIf { it.isNotBlank() }
        }
    }
}
