package com.truckie1.routing

import org.junit.Assert.assertTrue
import org.junit.Test

/** Gherkin: features/route_follow_roads.feature — Valhalla factory selection. */
class ValhallaPathRouterFactoryTest {

    @Test
    fun selectsStubWhenLiveDisabled() {
        val router = ValhallaPathRouterFactory.create(
            liveEnabled = false,
            baseUrl = "https://api.simplerouting.io/valhalla",
            apiKey = "k",
        )
        assertTrue(router is StubGraphHopperPathRouter)
    }

    @Test
    fun selectsStubWhenBaseUrlBlank() {
        val router = ValhallaPathRouterFactory.create(
            liveEnabled = true,
            baseUrl = "  ",
            apiKey = "k",
        )
        assertTrue(router is StubGraphHopperPathRouter)
    }

    @Test
    fun selectsLiveWhenEnabledAndBaseUrlConfigured() {
        val router = ValhallaPathRouterFactory.create(
            liveEnabled = true,
            baseUrl = "https://api.simplerouting.io/valhalla",
            apiKey = "k",
            httpClient = { _, _, _ -> error("should not call in factory test") },
        )
        assertTrue(router is LiveValhallaPathRouter)
    }
}
