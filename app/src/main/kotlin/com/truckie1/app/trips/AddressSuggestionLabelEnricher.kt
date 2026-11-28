package com.truckie1.app.trips

/**
 * Preserves typed AU unit / house-number premise on Nominatim suggestions when
 * OSM only returns a street-level place (no housenumber in the display name).
 *
 * Never invents coordinates — only rewrites [AddressSuggestion.label].
 */
object AddressSuggestionLabelEnricher {

    fun enrich(
        premise: AddressPremiseParser.Premise,
        suggestions: List<AddressSuggestion>,
    ): List<AddressSuggestion> {
        if (!premise.hasPremise || suggestions.isEmpty()) return suggestions
        return suggestions.map { suggestion ->
            val enrichedLabel = enrichLabel(suggestion.label, premise)
            if (enrichedLabel == suggestion.label) suggestion
            else suggestion.copy(label = enrichedLabel)
        }
    }

    internal fun enrichLabel(
        label: String,
        premise: AddressPremiseParser.Premise,
    ): String {
        val normalizedLabel = normalizeForCompare(label)
        val houseNumber = premise.houseNumber
        val unit = premise.unit
        val hasHouse = houseNumber == null ||
            containsToken(normalizedLabel, normalizeForCompare(houseNumber))
        val hasUnit = unit == null || unitPresent(normalizedLabel, unit)
        if (hasHouse && hasUnit) return label

        val parts = mutableListOf<String>()
        if (!hasUnit && !unit.isNullOrBlank()) parts += unit
        if (!hasHouse && !houseNumber.isNullOrBlank()) parts += houseNumber
        if (parts.isEmpty()) return label
        return "${parts.joinToString(" ")} $label".trim()
    }

    private fun unitPresent(normalizedLabel: String, unit: String): Boolean {
        val normalizedUnit = normalizeForCompare(unit)
        if (containsToken(normalizedLabel, normalizedUnit)) return true
        val digits = normalizedUnit
            .removePrefix("unit ")
            .removePrefix("unit")
            .removePrefix("u")
            .trim()
        if (digits.isEmpty()) return false
        return containsToken(normalizedLabel, "u$digits") ||
            containsToken(normalizedLabel, "unit $digits") ||
            containsToken(normalizedLabel, "unit$digits")
    }

    private fun containsToken(haystack: String, token: String): Boolean {
        if (token.isBlank()) return false
        val pattern = Regex("(?<![a-z0-9])${Regex.escape(token)}(?![a-z0-9])")
        return pattern.containsMatchIn(haystack)
    }

    private fun normalizeForCompare(value: String): String =
        value.lowercase()
            .replace(Regex("""\s*[-–]\s*"""), "-")
            .replace(Regex("""\s+"""), " ")
            .trim()
}
