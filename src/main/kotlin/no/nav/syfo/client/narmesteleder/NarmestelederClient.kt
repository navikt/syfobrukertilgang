package no.nav.syfo.client.narmesteleder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.client.narmesteleder.domain.NarmesteLederRelasjon
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_FAIL
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_SUCCESS
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class NarmestelederClient(
    private val baseUrl: String,
    private val narmestelederId: String,
    private val azureAdTokenClient: AzureADTokenClient
) {
    private val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
            }
        }
    }

    suspend fun ansatte(innloggetAktorId: String, callId: String): List<Ansatt>? {
        val bearer = azureAdTokenClient.accessToken(narmestelederId).access_token

        val url = getAnsatteUrl(innloggetAktorId)
        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, bearerHeader(bearer))
            header(NAV_CALL_ID_HEADER, callId)
            header(NAV_CONSUMER_ID_HEADER, APP_CONSUMER_ID)
            accept(ContentType.Application.Json)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                COUNT_CALL_NARMESTELEDER_SUCCESS.inc()
                val narmesteLederRelasjonListe = response.receive<List<NarmesteLederRelasjon>>()
                return narmesteLederRelasjonListe.map {
                    Ansatt(
                        aktorId = it.aktorId,
                        virksomhetsnummer = it.orgnummer
                    )
                }
            }
            else -> {
                COUNT_CALL_NARMESTELEDER_FAIL.inc()
                LOG.error("Error while requesting ansatte from syfonarmesteleder: status=${response.status.value}")
                return null
            }
        }
    }

    private fun getAnsatteUrl(ansattAktorId: String): String {
        return "$baseUrl/syfonarmesteleder/narmesteLeder/$ansattAktorId"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NarmestelederClient::class.java)
    }
}
