package no.nav.syfo.tilgang

class AnsattTilgangService {
    fun hasAccessToAnsatt(loggedInFnr: String, ansattFnr: String): Boolean {
        return loggedInFnr == ansattFnr
    }
}
