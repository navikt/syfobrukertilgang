package no.nav.syfo.util

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.util.pipeline.PipelineContext

const val JWT_CLAIM_AZP = "azp"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String = this.call.getCallId()

fun ApplicationCall.getCallId(): String = this.request.headers[NAV_CALL_ID_HEADER].toString()

fun ApplicationCall.getBearerHeader(): String? = this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")

fun ApplicationCall.getConsumerClientId(): String? =
    getBearerHeader()?.let {
        JWT.decode(it).claims[JWT_CLAIM_AZP]?.asString()
    }
