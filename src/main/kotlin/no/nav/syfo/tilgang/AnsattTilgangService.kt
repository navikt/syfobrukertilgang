package no.nav.syfo.tilgang

import no.nav.syfo.client.narmesteleder.NarmestelederClient

class AnsattTilgangService(
    private val narmestelederClient: NarmestelederClient
) {
    suspend fun hasAccessToAnsatt(innloggetFnr: String, ansattFnr: String): Boolean {
        val ansatte = narmestelederClient.ansatte(innloggetFnr) ?: emptyList()
        return ansatte.any { it.fnr == ansattFnr }
    }
}
