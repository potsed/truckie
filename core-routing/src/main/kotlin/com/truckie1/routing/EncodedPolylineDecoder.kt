package com.truckie1.routing

/**
 * Decodes encoded polyline strings (Google algorithm).
 *
 * Valhalla uses **precision 6**; Google / some GraphHopper responses use 5.
 */
object EncodedPolylineDecoder {

    fun decode(encoded: String, precision: Int = 6): List<LatLngPoint> {
        val trimmed = encoded.trim()
        if (trimmed.isEmpty()) return emptyList()
        val factor = Math.pow(10.0, precision.toDouble())
        val out = ArrayList<LatLngPoint>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < trimmed.length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                if (index >= trimmed.length) return out
                b = trimmed[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            result = 0
            shift = 0
            do {
                if (index >= trimmed.length) return out
                b = trimmed[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            out += LatLngPoint(
                latitude = lat / factor,
                longitude = lng / factor,
            )
        }
        return out
    }
}
