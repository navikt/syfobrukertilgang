package no.nav.syfo.client.narmesteleder.domain

import java.time.LocalDate

data class NarmesteLederRelasjon(
    val aktorId: String,
    val fnr: String,
    val orgnummer: String,
    val narmesteLederFnr: String,
    val narmesteLederTelefonnummer: String?,
    val narmesteLederEpost: String?,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate,
    val arbeidsgiverForskutterer: Boolean?,
    val skrivetilgang: Boolean,
    val tilganger: List<Tilgang>
)

enum class Tilgang {
    SYKMELDING,
    SYKEPENGESOKNAD,
    MOTE,
    OPPFOLGINGSPLAN
}
