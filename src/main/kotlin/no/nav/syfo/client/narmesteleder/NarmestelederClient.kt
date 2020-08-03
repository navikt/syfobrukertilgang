package no.nav.syfo.client.narmesteleder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpGet
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.client.narmesteleder.domain.NarmesteLederRelasjon
import no.nav.syfo.metric.*
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class NarmestelederClient(
    private val baseUrl: String,
    private val narmestelederId: String,
    private val azureAdTokenClient: AzureADTokenClient
) {
    fun ansatte(innloggetAktorId: String, callId: String): List<Ansatt>? {
        val bearer = azureAdTokenClient.accessToken(narmestelederId).access_token

        COUNT_CALL_NARMESTELEDER.inc()

        val (_, response, result) = getAnsatteUrl(innloggetAktorId).httpGet()
            .header(mapOf(
                "Authorization" to bearerHeader(bearer),
                "Accept" to "application/json",
                "Nav-Call-Id" to callId,
                "Nav-Consumer-Id" to "syfobrukertilgang"
            ))
            .responseString()

        result.fold(success = {
            COUNT_CALL_NARMESTELEDER_SUCCESS.inc()
            val narmesteLederRelasjonListe = objectMapper.readValue<List<NarmesteLederRelasjon>>(result.get())
            return narmesteLederRelasjonListe.map {
                Ansatt(
                    aktorId = it.aktorId,
                    virksomhetsnummer = it.orgnummer
                )
            }
        }, failure = {
            COUNT_CALL_NARMESTELEDER_FAIL.inc()
            LOG.info("Request with url: $baseUrl failed with reponse code ${response.statusCode}")
            val exception = it.exception
            LOG.error("Error while requesting ansatte from syfonarmesteleder: ${exception.message}", exception)
            return null
        })
    }

    private fun getAnsatteUrl(ansattAktorId: String): String {
        return "$baseUrl/syfonarmesteleder/narmesteLeder/$ansattAktorId"
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(NarmestelederClient::class.java)
    }
}
