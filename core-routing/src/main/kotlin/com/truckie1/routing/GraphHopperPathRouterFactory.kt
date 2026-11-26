package com.truckie1.routing

/**
 * Resolves stub vs live GraphHopper [PathRouter].
 *
 * Live HTTP is used only when [liveEnabled] is true **and** [baseUrl] is non-blank
 * (feature flag `ff_graphhopper_live` + configured base URL in the app).
 */
object GraphHopperPathRouterFactory {
    fun create(
        liveEnabled: Boolean,
        baseUrl: String?,
        httpClient: ((url: String, body: String) -> String)? = null,
    ): PathRouter {
        val trimmed = baseUrl?.trim().orEmpty()
        if (!liveEnabled || trimmed.isEmpty()) {
            return StubGraphHopperPathRouter()
        }
        return if (httpClient != null) {
            LiveGraphHopperPathRouter(baseUrl = trimmed, httpClient = httpClient)
        } else {
            LiveGraphHopperPathRouter(baseUrl = trimmed)
        }
    }
}
