package no.nav.syfo.client.aktor

import no.nav.syfo.client.aktor.domain.AktorId
import no.nav.syfo.client.aktor.domain.Fodselsnummer

import no.nav.syfo.log

class AktorService(private val aktorregisterClient: AktorregisterClient) {

    fun getFodselsnummerForAktor(aktorId: AktorId, callId: String) =
        aktorregisterClient.getNorskIdent(aktorId.aktor, callId).mapLeft {
            throw IllegalStateException("Fant ikke aktor")
        }

    fun getAktorForFodselsnummer(fodselsnummer: Fodselsnummer, callId: String) =
        aktorregisterClient.getAktorId(fodselsnummer.value, callId).mapLeft {
            throw IllegalStateException("Fant ikke aktor")
        }

    fun fodselsnummerForAktor(aktorId: AktorId, callId: String): String? {
        var fnr: String? = null
        getFodselsnummerForAktor(aktorId, callId).mapLeft {
            log.info("Fant ikke fnr for Aktor")
            throw IllegalStateException("Fant ikke aktor")
        }.map {
            fnr = it
        }
        return fnr
    }

    fun aktorForFodselsnummer(fodselsnummer: Fodselsnummer, callId: String): String? {
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
