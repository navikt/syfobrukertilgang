package no.nav.syfo.wellknown

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.httpClientProxy

fun getWellKnown(wellKnownUrl: String): WellKnown =
    runBlocking {
        httpClientProxy().use { client ->
            client.get(wellKnownUrl).body<WellKnownDTO>().toWellKnown()
        }
    }
