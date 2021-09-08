package no.nav.syfo.client.narmesteleder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.client.narmesteleder.domain.NarmesteLederRelasjon
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_FAIL
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_SUCCESS
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class NarmestelederClient(
    private val narmestelederUrl: String,
    private val narmestelederScope: String,
    private val azureAdTokenClient: AzureADTokenClient
) {
    @KtorExperimentalAPI
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

    @KtorExperimentalAPI
    suspend fun ansatte(innloggetFnr: String): List<Ansatt>? {
        val token = azureAdTokenClient.accessToken(narmestelederScope)!!.access_token
        val url = getAnsatteUrl()

        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, bearerHeader(token))
            header("Narmeste-Leder-Fnr", innloggetFnr)
            accept(ContentType.Application.Json)
        }
        when (response.status) {
            HttpStatusCode.OK -> {
                COUNT_CALL_NARMESTELEDER_SUCCESS.inc()
                val narmesteLederRelasjonListe = response.receive<List<NarmesteLederRelasjon>>()
                return narmesteLederRelasjonListe.map {
                    Ansatt(
                        fnr = it.fnr,
                        virksomhetsnummer = it.orgnummer
                    )
                }
            }
            else -> {
                COUNT_CALL_NARMESTELEDER_FAIL.inc()
                LOG.error("Feil ved henting av liste over ansatte fra narmesteleder: status=${response.status.value}")
                return null
            }
        }
    }

    private fun getAnsatteUrl(): String {
        return "$narmestelederUrl/leder/narmesteleder/aktive"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NarmestelederClient::class.java)
    }
}
