package no.nav.syfo.client.narmesteleder.domain

import java.time.LocalDate

data class NarmesteLederRelasjon(
    val aktorId: String,
    val orgnummer: String,
    val narmesteLederAktorId: String,
    val narmesteLederTelefonnummer: String?,
    val narmesteLederEpost: String?,
    val aktivFom: LocalDate,
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
