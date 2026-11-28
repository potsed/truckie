package com.truckie1.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/route_follow_roads.feature — Valhalla polyline6 decode. */
class EncodedPolylineDecoderTest {

    @Test
    fun decodesClassicGooglePolylineExamplePrecision5() {
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = EncodedPolylineDecoder.decode(encoded, precision = 5)
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 1e-5)
        assertEquals(-120.2, points[0].longitude, 1e-5)
    }

    @Test
    fun emptyOrBlankYieldsEmptyList() {
        assertTrue(EncodedPolylineDecoder.decode("").isEmpty())
        assertTrue(EncodedPolylineDecoder.decode("   ").isEmpty())
    }
}
