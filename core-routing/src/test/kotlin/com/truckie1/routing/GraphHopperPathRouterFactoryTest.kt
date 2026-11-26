package com.truckie1.routing

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/route_optimisation_graphhopper.feature —
 * live vs stub PathRouter selection (ATNA-9 fold-in).
 * Flag key `ff_graphhopper_live` lives in core-flags; factory takes a boolean.
 */
class GraphHopperPathRouterFactoryTest {

    @Test
    fun selectsStubWhenLiveDisabled() {
        val router = GraphHopperPathRouterFactory.create(
            liveEnabled = false,
            baseUrl = "https://example.invalid/gh",
        )
        assertTrue(router is StubGraphHopperPathRouter)
    }

    @Test
    fun selectsStubWhenBaseUrlBlankEvenIfLiveEnabled() {
        val router = GraphHopperPathRouterFactory.create(
            liveEnabled = true,
            baseUrl = "  ",
        )
        assertTrue(router is StubGraphHopperPathRouter)
    }

    @Test
    fun selectsLiveWhenEnabledAndBaseUrlConfigured() {
        val router = GraphHopperPathRouterFactory.create(
            liveEnabled = true,
            baseUrl = "https://gh.example/api",
            httpClient = { _, _ -> error("should not call in factory test") },
        )
        assertTrue(router is LiveGraphHopperPathRouter)
    }
}
