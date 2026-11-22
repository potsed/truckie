package com.truckie1.trips

/**
 * Operating mass along a trip: tare + on-board cargo mass after each stop.
 */
object OnBoardMass {
    fun operatingMassAfterStopT(
        tareT: Double,
        stops: List<TripStop>,
        throughIndexInclusive: Int,
    ): Double {
        require(tareT > 0) { "tareT must be positive" }
        val cargoMass = OnBoardCargo.afterStops(stops, throughIndexInclusive).sumOf { it.massT }
        return tareT + cargoMass
    }

    fun peakOperatingMassT(tareT: Double, stops: List<TripStop>): Double {
        require(tareT > 0) { "tareT must be positive" }
        if (stops.isEmpty()) return tareT
        var peak = tareT
        for (i in stops.indices) {
            val atStop = operatingMassAfterStopT(tareT, stops, i)
            if (atStop > peak) peak = atStop
        }
        return peak
    }

    fun exceedsGvm(tareT: Double, gvmT: Double, stops: List<TripStop>): Boolean =
        peakOperatingMassT(tareT, stops) > gvmT
}
