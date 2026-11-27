package com.truckie1.routing

import com.truckie1.trips.RouteEndpoint
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/async_routing_io.feature — live PathRouter hops off caller.
 */
class LiveGraphHopperPathRouterAsyncTest {

    @Test
    fun pathDispatchesHttpClientOntoIoDispatcher() = runBlocking {
        val ioEntered = AtomicBoolean(false)
        val clientSawIo = AtomicBoolean(false)
        val io = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                ioEntered.set(true)
                block.run()
            }
        }
        val json = """
            {
              "paths": [{
                "distance": 100.0,
                "time": 60000,
                "points": {
                  "coordinates": [
                    [153.10, -27.42],
                    [153.11, -27.43]
                  ]
                }
              }]
            }
        """.trimIndent()
        val router = LiveGraphHopperPathRouter(
            baseUrl = "https://gh.example",
            httpClient = { _, _ ->
                clientSawIo.set(ioEntered.get())
                json
            },
            ioDispatcher = io,
        )

        val result = router.path(
            RouteEndpoint("A", -27.42, 153.10),
            RouteEndpoint("B", -27.43, 153.11),
        )

        assertTrue(result is PathResult.Ok)
        assertTrue("live path must hop to IO before HTTP", ioEntered.get())
        assertTrue("HTTP client must run after IO hop", clientSawIo.get())
    }

    @Test
    fun pathStillParsesOnInjectedClient() = runBlocking {
        val threadName = AtomicReference<String?>(null)
        val router = LiveGraphHopperPathRouter(
            baseUrl = "https://gh.example",
            httpClient = { _, _ ->
                threadName.set(Thread.currentThread().name)
                """
                {
                  "paths": [{
                    "time": 1000,
                    "points": { "coordinates": [[153.0, -27.0], [153.1, -27.1]] }
                  }]
                }
                """.trimIndent()
            },
            ioDispatcher = object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
            },
        )
        when (val result = router.path(
            RouteEndpoint("A", -27.0, 153.0),
            RouteEndpoint("B", -27.1, 153.1),
        )) {
            is PathResult.Ok -> assertEquals(RoutePath.SOURCE_LIVE, result.path.source)
            is PathResult.Blocked -> error(result.reason)
        }
        assertTrue(threadName.get() != null)
    }
}
