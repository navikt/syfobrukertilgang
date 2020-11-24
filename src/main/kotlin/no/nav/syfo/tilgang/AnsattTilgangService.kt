package no.nav.syfo.tilgang

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.domain.Fodselsnummer
import no.nav.syfo.client.narmesteleder.NarmestelederClient

class AnsattTilgangService(
    private val aktorService: AktorService,
    private val narmestelederClient: NarmestelederClient
) {
    suspend fun hasAccessToAnsatt(loggedInFnr: String, ansattFnr: String, callId: String): Boolean {
        val innloggetAktorId = aktorService.aktorForFodselsnummer(Fodselsnummer(loggedInFnr), callId)
        val ansattAktorId = aktorService.aktorForFodselsnummer(Fodselsnummer(ansattFnr), callId)

        innloggetAktorId?.let {
            val ansatte = narmestelederClient.ansatte(innloggetAktorId, callId) ?: emptyList()
            return ansatte.any { it.aktorId == ansattAktorId }
        }
        return false
    }
}
