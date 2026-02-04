package no.nav.syfo.client.narmesteleder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.client.narmesteleder.domain.NarmesteLederRelasjon
import no.nav.syfo.exception.DependencyUnavailableException
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_FAIL
import no.nav.syfo.metric.COUNT_CALL_NARMESTELEDER_SUCCESS
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class NarmestelederClient(
    private val httpClient: HttpClient,
    private val narmestelederUrl: String,
    private val narmestelederScope: String,
    private val azureAdTokenClient: AzureADTokenClient
) {

    suspend fun ansatte(
        innloggetFnr: String,
        callId: String?,
        consumerId: String?
    ): List<Ansatt> {
        val token = azureAdTokenClient.accessToken(narmestelederScope)
        val url = getAnsatteUrl()

        return try {
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(token))
                header("Narmeste-Leder-Fnr", innloggetFnr)
                callId?.let { header(NAV_CALL_ID_HEADER, it) }
                consumerId?.let { header(NAV_CONSUMER_ID_HEADER, it) }
                accept(ContentType.Application.Json)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    COUNT_CALL_NARMESTELEDER_SUCCESS.inc()
                    val narmesteLederRelasjonListe = response.body<List<NarmesteLederRelasjon>>()
                    narmesteLederRelasjonListe.map {
                        Ansatt(
                            fnr = it.fnr,
                            virksomhetsnummer = it.orgnummer
                        )
                    }
                }

                else -> {
                    COUNT_CALL_NARMESTELEDER_FAIL.inc()
                    LOG.warn("Feil ved henting av liste over ansatte fra narmesteleder: status=${response.status.value}")
                    throw DependencyUnavailableException("Narmesteleder returned ${response.status.value}")
                }
            }
        } catch (e: DependencyUnavailableException) {
            throw e
        } catch (e: Exception) {
            COUNT_CALL_NARMESTELEDER_FAIL.inc()
            throw DependencyUnavailableException("Narmesteleder request failed", e)
        }
    }

    private fun getAnsatteUrl(): String {
        return "$narmestelederUrl/leder/narmesteleder/aktive"
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NarmestelederClient::class.java)
    }
}
