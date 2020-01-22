package no.nav.syfo.tilgang

import io.ktor.application.call
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val basePath: String = "/api/v1/tilgang/ansatt"

fun Route.registerAnsattTilgangApi() {
    route(basePath) {
        get("/{fnr}") {
            try {
                val ansattFnr: String = call.parameters["fnr"]?.takeIf { validateFnr(it) }
                        ?: throw IllegalArgumentException("Fnr mangler")

                val callId = getCallId()

                val credentials = call.principal<JWTPrincipal>()

                credentials?.let { creds ->
                    val loggedInFnr = creds.payload.subject
                    if (hasAccessToAnsatt(loggedInFnr, ansattFnr)) {
                        call.respond(true)
                    } else {
                        log.warn("Innlogget bruker har ikke tilgang til oppslått ansatt, {}", CallIdArgument(callId))
                        call.respond(false)
                    }
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente tilgang til ansatt med fnr: {}, {}", e.message, CallIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}

fun hasAccessToAnsatt(loggedInFnr: String, ansattFnr: String): Boolean {
    return loggedInFnr == ansattFnr
}
