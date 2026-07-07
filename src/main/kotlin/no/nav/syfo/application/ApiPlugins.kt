package no.nav.syfo.application

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.configure
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerClientId
import java.util.UUID

fun Application.installContentNegotiation() {
    install(ContentNegotiation) {
        jackson { configure() }
    }
}

fun Application.installCallId() {
    install(CallId) {
        retrieve { it.request.headers["X-Nav-CallId"] }
        retrieve { it.request.headers[HttpHeaders.XCorrelationId] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }
}

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val callId = call.getCallId()
            val consumerClientId = call.getConsumerClientId()
            val logExceptionMessage = "Caught exception, callId=$callId, consumerClientId=$consumerClientId"
            val log = call.application.log
            log.error(logExceptionMessage, cause)

            var isUnexpectedException = false

            val responseStatus: HttpStatusCode =
                when (cause) {
                    is ResponseException -> {
                        cause.response.status
                    }

                    is IllegalArgumentException -> {
                        HttpStatusCode.BadRequest
                    }

                    else -> {
                        isUnexpectedException = true
                        HttpStatusCode.InternalServerError
                    }
                }
            val message =
                if (isUnexpectedException) {
                    "The server reported an unexpected error and cannot complete the request."
                } else {
                    cause.message ?: "Unknown error"
                }
            call.respond(responseStatus, message)
        }
    }
}
