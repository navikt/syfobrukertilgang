package no.nav.syfo.tilgang

import io.ktor.util.*
import no.nav.syfo.client.narmesteleder.NarmestelederClient

class AnsattTilgangService(
    private val narmestelederClient: NarmestelederClient
) {
    @KtorExperimentalAPI
    suspend fun hasAccessToAnsatt(innloggetFnr: String, ansattFnr: String): Boolean {
        // TODO: Do we need innloggetAktorId?
        val ansatte = narmestelederClient.ansatte(innloggetFnr) ?: emptyList()
        // TODO: Do we need ansattAktorId? : response form NL returns ansatt-fnr, ==>  it.ansattFnr == ansattFnr
        return ansatte.any { it.fnr == ansattFnr }
    }
}
