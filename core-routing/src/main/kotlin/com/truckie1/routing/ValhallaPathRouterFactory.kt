package com.truckie1.routing

/**
 * Resolves stub vs live Valhalla [PathRouter] (ATNA-113).
 *
 * Live when [liveEnabled] and [baseUrl] are set (`ff_valhalla_live` + credentials).
 * Optional [apiKey] becomes `Authorization: Bearer` (Simple Routing interim).
 * Self-hosted / offline tiles: set base URL, leave API key blank.
 */
object ValhallaPathRouterFactory {
    fun create(
        liveEnabled: Boolean,
        baseUrl: String?,
        apiKey: String? = null,
        httpClient: ((url: String, body: String, bearerToken: String?) -> String)? = null,
    ): PathRouter {
        val trimmed = baseUrl?.trim().orEmpty()
        if (!liveEnabled || trimmed.isEmpty()) {
            return StubGraphHopperPathRouter()
        }
        val key = apiKey?.trim()?.ifEmpty { null }
        return if (httpClient != null) {
            LiveValhallaPathRouter(baseUrl = trimmed, apiKey = key, httpClient = httpClient)
        } else {
            LiveValhallaPathRouter(baseUrl = trimmed, apiKey = key)
        }
    }
}
