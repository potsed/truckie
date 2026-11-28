package com.truckie1.app.trips

/**
 * Versioned Australia address pack schema (ATNA-109).
 *
 * JSON is the canonical SoT wire format (bridges-pack pattern). Optional
 * Room/FTS on device is a **derived** index only.
 *
 * Gherkin: features/owned_au_address_pack.feature
 * Plan: docs/au-address-datasource.md
 */
enum class AddressPrecisionLevel {
    UNIT,
    HOUSE,
    BUILDING,
    STREET,
    ;

    companion object {
        fun fromWire(value: String): AddressPrecisionLevel =
            when (value.trim().lowercase()) {
                "unit" -> UNIT
                "house" -> HOUSE
                "building" -> BUILDING
                "street" -> STREET
                else -> error("Unknown address precision: $value")
            }
    }

    fun toWire(): String = name.lowercase()
}

/**
 * Upstream citation for an address row (required on SoT rows).
 */
data class AddressSource(
    val name: String,
    val url: String,
    val retrievedAt: String,
) {
    init {
        require(name.isNotBlank()) { "source.name required" }
        require(url.isNotBlank()) { "source.url required" }
        require(retrievedAt.isNotBlank()) { "source.retrievedAt required" }
    }
}

data class AddressPackManifest(
    val region: String,
    val version: String,
    val generatedAt: String,
    val source: AddressSource,
    val recordCount: Int,
    val schemaVersion: Int = SCHEMA_VERSION,
    /** HTTPS URL for the addresses JSON body; null means bundled / injected. */
    val downloadUrl: String? = null,
) {
    init {
        require(region.isNotBlank()) { "region required" }
        require(version.isNotBlank()) { "version required" }
        require(generatedAt.isNotBlank()) { "generatedAt required" }
        require(recordCount >= 0) { "recordCount must be >= 0" }
        require(schemaVersion >= 1) { "schemaVersion must be >= 1" }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

/**
 * One premise-level address. Coordinates and [id] are never invented.
 */
data class AddressPackRecord(
    val id: String,
    val lat: Double,
    val lon: Double,
    val formatted: String,
    val number: String,
    val street: String,
    val locality: String,
    val state: String,
    val postcode: String,
    val precision: AddressPrecisionLevel,
    val source: AddressSource,
    val unit: String? = null,
    val gnafPid: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id required" }
        require(formatted.isNotBlank()) { "formatted required" }
        require(street.isNotBlank()) { "street required" }
        require(locality.isNotBlank()) { "locality required" }
        require(state.isNotBlank()) { "state required" }
        require(postcode.isNotBlank()) { "postcode required" }
        require(lat in -90.0..90.0) { "lat out of range" }
        require(lon in -180.0..180.0) { "lon out of range" }
    }

    val isPremisePrecise: Boolean
        get() = precision == AddressPrecisionLevel.UNIT ||
            precision == AddressPrecisionLevel.HOUSE ||
            precision == AddressPrecisionLevel.BUILDING
}

enum class AddressPackSource {
    /** Active data from on-device versioned cache. */
    CACHE,
    /** Bootstrap from `assets/addresses/au_sample_addresses.json` when cache empty. */
    BUNDLED_SAMPLE,
}

data class AddressPack(
    val manifest: AddressPackManifest,
    val addresses: List<AddressPackRecord>,
    val source: AddressPackSource,
)

/**
 * Minimal JSON parse for the owned address pack envelope.
 * Empty `addresses` is valid (schema scaffold / pre-ETL).
 */
object AddressPackJson {
    fun parse(json: String): AddressPack {
        val root = org.json.JSONObject(json)
        val manifestObj = root.getJSONObject("manifest")
        val sourceObj = manifestObj.getJSONObject("source")
        val manifest = AddressPackManifest(
            region = manifestObj.getString("region"),
            version = manifestObj.getString("version"),
            generatedAt = manifestObj.getString("generatedAt"),
            source = parseSource(sourceObj),
            recordCount = manifestObj.getInt("recordCount"),
            schemaVersion = manifestObj.optInt("schemaVersion", AddressPackManifest.SCHEMA_VERSION),
            downloadUrl = optNullableString(manifestObj, "downloadUrl"),
        )
        val array = root.optJSONArray("addresses") ?: org.json.JSONArray()
        val records = buildList {
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                add(
                    AddressPackRecord(
                        id = row.getString("id"),
                        lat = row.getDouble("lat"),
                        lon = row.getDouble("lon"),
                        formatted = row.getString("formatted"),
                        number = row.optString("number").orEmpty(),
                        street = row.getString("street"),
                        locality = row.getString("locality"),
                        state = row.getString("state"),
                        postcode = row.getString("postcode"),
                        precision = AddressPrecisionLevel.fromWire(row.getString("precision")),
                        source = parseSource(row.getJSONObject("source")),
                        unit = optNullableString(row, "unit"),
                        gnafPid = optNullableString(row, "gnafPid"),
                    ),
                )
            }
        }
        return AddressPack(
            manifest = manifest,
            addresses = records,
            source = AddressPackSource.BUNDLED_SAMPLE,
        )
    }

    /**
     * org.json [org.json.JSONObject.optString] returns the literal `"null"` for JSON null —
     * treat that as absent.
     */
    fun optNullableString(obj: org.json.JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    fun parseSource(obj: org.json.JSONObject): AddressSource = AddressSource(
        name = obj.getString("name"),
        url = obj.getString("url"),
        retrievedAt = obj.getString("retrievedAt"),
    )
}
