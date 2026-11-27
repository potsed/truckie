package com.truckie1.app.trips

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gherkin: features/async_routing_io.feature — Nominatim hops off caller.
 */
class NominatimAddressAutocompleteAsyncTest {

    @Test
    fun suggestDispatchesHttpClientOntoIoDispatcher() = runBlocking {
        val ioEntered = AtomicBoolean(false)
        val clientSawIo = AtomicBoolean(false)
        val io = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                ioEntered.set(true)
                block.run()
            }
        }
        val autocomplete = NominatimAddressAutocomplete(
            client = AddressHttpClient { _, _ ->
                clientSawIo.set(ioEntered.get())
                "[]"
            },
            ioDispatcher = io,
        )
        autocomplete.suggest("Brisbane")
        assertTrue("Nominatim must hop to IO before HTTP", ioEntered.get())
        assertTrue("HTTP client must run after IO hop", clientSawIo.get())
    }
}
