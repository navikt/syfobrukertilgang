package no.nav.syfo.tilgang

import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.domain.Fodselsnummer

class AnsattTilgangService(
        private val aktorService: AktorService
) {
    fun hasAccessToAnsatt(loggedInFnr: String, ansattFnr: String, callId: String): Boolean {
        val innloggetAktorId = aktorService.aktorForFodselsnummer(Fodselsnummer(loggedInFnr), callId)
        val ansattAktorId = aktorService.aktorForFodselsnummer(Fodselsnummer(ansattFnr), callId)
        return innloggetAktorId == ansattAktorId
    }
}
