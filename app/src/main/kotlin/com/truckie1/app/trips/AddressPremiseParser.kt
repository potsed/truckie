package com.truckie1.app.trips

/**
 * Parses AU-style unit / house-number **premise** from a typed address query.
 *
 * Nominatim/OSM often returns street-level hits without `housenumber` for
 * residential AU addresses. Truckie keeps the verified place coordinates and
 * uses this premise to enrich the display label (ATNA-108).
 */
object AddressPremiseParser {

    data class Premise(
        val unit: String?,
        val houseNumber: String?,
        val remainder: String,
    ) {
        val hasPremise: Boolean
            get() = !unit.isNullOrBlank() || !houseNumber.isNullOrBlank()

        /** Display fragment such as `U21 1-31` or `42`. */
        fun formatPrefix(): String =
            listOfNotNull(unit?.takeIf { it.isNotBlank() }, houseNumber?.takeIf { it.isNotBlank() })
                .joinToString(" ")
    }

    private val slashUnitHouse = Regex(
        """^\s*(\d+[A-Za-z]?)\s*/\s*(\d+[A-Za-z]?(?:\s*[-–]\s*\d+[A-Za-z]?)?)\s+(.+)$""",
    )
    private val unitWordThenHouse = Regex(
        """^\s*((?:[Uu](?:nit)?\.?\s*)\d+[A-Za-z]?)\s+(\d+[A-Za-z]?(?:\s*[-–]\s*\d+[A-Za-z]?)?)\s+(.+)$""",
    )
    private val unitOnlyThenStreet = Regex(
        """^\s*((?:[Uu](?:nit)?\.?\s*)\d+[A-Za-z]?)\s+(.+)$""",
    )
    private val houseThenStreet = Regex(
        """^\s*(\d+[A-Za-z]?(?:\s*[-–]\s*\d+[A-Za-z]?)?)\s+(.+)$""",
    )

    fun parse(query: String): Premise {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Premise(unit = null, houseNumber = null, remainder = "")
        }

        slashUnitHouse.matchEntire(trimmed)?.let { m ->
            return Premise(
                unit = m.groupValues[1],
                houseNumber = normalizeRange(m.groupValues[2]),
                remainder = m.groupValues[3].trim(),
            )
        }
        unitWordThenHouse.matchEntire(trimmed)?.let { m ->
            return Premise(
                unit = normalizeUnit(m.groupValues[1]),
                houseNumber = normalizeRange(m.groupValues[2]),
                remainder = m.groupValues[3].trim(),
            )
        }
        unitOnlyThenStreet.matchEntire(trimmed)?.let { m ->
            val rest = m.groupValues[2].trim()
            // Avoid treating "Unit 5 Queen…" as unit-only when a house number follows
            // (already handled above). Street-only remainder after unit is valid.
            return Premise(
                unit = normalizeUnit(m.groupValues[1]),
                houseNumber = null,
                remainder = rest,
            )
        }
        houseThenStreet.matchEntire(trimmed)?.let { m ->
            val rest = m.groupValues[2].trim()
            if (looksLikeStreetRemainder(rest)) {
                return Premise(
                    unit = null,
                    houseNumber = normalizeRange(m.groupValues[1]),
                    remainder = rest,
                )
            }
        }
        return Premise(unit = null, houseNumber = null, remainder = trimmed)
    }

    private fun normalizeUnit(raw: String): String {
        val compact = raw.trim().replace(Regex("""\s+"""), " ")
        Regex("""(?i)^u\.?\s*(\d+[A-Za-z]?)$""").matchEntire(compact)?.let {
            return "U${it.groupValues[1]}"
        }
        Regex("""(?i)^unit\.?\s*(\d+[A-Za-z]?)$""").matchEntire(compact)?.let {
            return "Unit ${it.groupValues[1]}"
        }
        return compact
    }

    private fun normalizeRange(raw: String): String =
        raw.trim().replace(Regex("""\s*[-–]\s*"""), "-")

    /**
     * House-number parse only when the remainder looks like a road / suburb,
     * not another leading numeric token alone.
     */
    private fun looksLikeStreetRemainder(remainder: String): Boolean {
        if (remainder.isBlank()) return false
        val first = remainder.split(Regex("""\s+"""), limit = 2).first()
        return first.any { it.isLetter() }
    }
}
