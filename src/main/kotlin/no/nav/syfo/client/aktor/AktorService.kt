package no.nav.syfo.client.aktor

import no.nav.syfo.client.aktor.domain.Fodselsnummer
import no.nav.syfo.log

class AktorService(private val aktorregisterClient: AktorregisterClient) {

    suspend fun getAktorForFodselsnummer(fodselsnummer: Fodselsnummer, callId: String) =
        aktorregisterClient.getAktorId(fodselsnummer.value, callId).mapLeft {
            throw IllegalStateException("Fant ikke aktor")
        }

    suspend fun aktorForFodselsnummer(fodselsnummer: Fodselsnummer, callId: String): String? {
        var aktorId: String? = null
        getAktorForFodselsnummer(fodselsnummer, callId).mapLeft {
            log.info("Fant ikke aktorId for fnr")
            throw IllegalStateException("Fant ikke fnr")
        }.map {
            aktorId = it
        }
        return aktorId
    }
}
