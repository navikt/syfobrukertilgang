package no.nav.syfo.clients.aktor.domain

data class Identinfo(
    val ident: String,
    val identgruppe: String,
    val gjeldende: Boolean = false
)
