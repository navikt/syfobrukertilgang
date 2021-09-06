package no.nav.syfo.tilgang

import io.ktor.util.*
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import org.slf4j.LoggerFactory

class AnsattTilgangService(
    private val narmestelederClient: NarmestelederClient
) {
    @KtorExperimentalAPI
    suspend fun hasAccessToAnsatt(innloggetFnr: String, ansattFnr: String): Boolean {
        // TODO: Do we need innloggetAktorId?
        LOG.warn(">>>>Response innloggetFnr: $innloggetFnr")
        LOG.warn(">>>>Response ansattFnr: $ansattFnr")

        val ansatte = narmestelederClient.ansatte(innloggetFnr) ?: emptyList()
        // TODO: Do we need ansattAktorId? : response form NL returns ansatt-fnr, ==>  it.ansattFnr == ansattFnr
        LOG.warn(">>>>Response narmestelederClient: $ansatte")
        return ansatte.any { it.fnr == ansattFnr }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AnsattTilgangService::class.java)
    }
}
