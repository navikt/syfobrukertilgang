package no.nav.syfo.client.aktor

import arrow.core.Either
import arrow.core.flatMap
import com.github.kittinunf.fuel.httpGet
import no.nav.syfo.client.sts.StsRestClient
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AktorregisterClient::class.java)

class AktorregisterClient(val baseUrl: String, val stsRestClient: StsRestClient) {

    fun getIdenter(ident: String, callId: String): Either<String, List<Ident>> {
        val bearer = stsRestClient.token()

        val (_, _, result) = "$baseUrl/identer?gjeldende=true".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to callId,
                        "Nav-Consumer-Id" to "syfobrukertilgang",
                        "Nav-Personidenter" to ident
                ))
                .responseString()

        val response = JSONObject(result.get())

        val identResponse = response.getJSONObject(ident)

        return if (identResponse.isNull("identer")) {
            val errorMessage = identResponse.getString("feilmelding")
            log.error("lookup gjeldende identer feilet med feilmelding $errorMessage")
            Either.Left(errorMessage)
        } else {
            val identer = identResponse.getJSONArray("identer")

            Either.Right(identer.map {
                it as JSONObject
            }.map {
                Ident(it.getString("ident"), it.getEnum(IdentType::class.java, "identgruppe"))
            })
        }
    }

    private fun getIdent(ident: String, type: IdentType, callId: String): Either<String, String> {
        return getIdenter(ident, callId).flatMap {
            Either.Right(it.first {
                it.type == type
            }.ident)
        }
    }

    fun getAktorId(ident: String, callId: String): Either<String, String> {
        return getIdent(ident, IdentType.AktoerId, callId)
    }

    fun getNorskIdent(ident: String, callId: String): Either<String, String> {
        return getIdent(ident, IdentType.NorskIdent, callId)
    }
}

enum class IdentType {
    AktoerId, NorskIdent
}

data class Ident(
        val ident: String,
        val type: IdentType
)
