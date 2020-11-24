package no.nav.syfo.clients.aktor.domain

data class IdentinfoListe(
    val identer: List<Identinfo>?,
    val feilmelding: String? = null
)
