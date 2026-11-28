package com.truckie1.routing

import com.truckie1.trips.OptimizeMode

/**
 * First-class truck routing goals for truck routing model / path requests.
 *
 * Among tours that satisfy bridge / weight / toll constraints (and soft
 * non-residential through-route preference with minimised residential
 * penetration), prefer the most **cost-effective and efficient** option
 * ([OptimizeMode.FASTEST] or [OptimizeMode.CHEAPEST] — same idea as
 * `ff_route_optimize`).
 *
 * Residential: soft-avoid for through-routing; when needed for a residential
 * delivery, use only the access required to reach the stop — never hard-block.
 * Full bridge/weight packs and live Custom Model remain Phase D.
 */
data class TruckRoutingPreferences(
    /** Reject edges below vehicle/cargo clearance (low bridges). */
    val avoidLowBridges: Boolean = true,
    /** Reject edges below required operating mass (road weight limits). */
    val avoidWeightRestrictedRoads: Boolean = true,
    /** Toll road preference (full cost UX lands with ff_tolls / Phase E). */
    val tollPreference: TollPreference = TollPreference.ALLOW_ALL,
    /**
     * Soft preference: prefer non-residential through-routes; when a residential
     * delivery requires residential roads, keep exposure to **only what is
     * required** to reach that stop (minimise penetration — not a through-route).
     * Never hard-block residential access for deliveries.
     */
    val preferNonResidentialThroughRoutes: Boolean = true,
    /**
     * When residential access is needed for a delivery stop, minimise how far
     * the path penetrates residential roads (last-mile / access only).
     */
    val minimiseResidentialPenetration: Boolean = true,
    /**
     * Among safe constrained tours, pick Fastest or Cheapest
     * (cost-effective / efficient for SOO drivers).
     */
    val optimizeMode: OptimizeMode = OptimizeMode.FASTEST,
) {
    companion object {
        /** Default SOO truck profile for Navigate / path finding. */
        val DEFAULT: TruckRoutingPreferences = TruckRoutingPreferences()
    }
}
