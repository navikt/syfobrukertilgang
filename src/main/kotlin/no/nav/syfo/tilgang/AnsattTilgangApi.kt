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

fun Route.registerAnsattTilgangApi(ansattTilgangService: AnsattTilgangService) {
    route(basePath) {
        get("/{fnr}") {
            try {
                val ansattFnr: String = call.parameters["fnr"]?.takeIf { validateFnr(it) }
                        ?: throw IllegalArgumentException("Fnr mangler")

                val credentials = call.principal<JWTPrincipal>()
                val callId = getCallId()

                credentials?.let { creds ->
                    val loggedInFnr = creds.payload.subject
                    if (ansattTilgangService.hasAccessToAnsatt(loggedInFnr, ansattFnr, callId)) {
                        call.respond(true)
                    } else {
                        log.warn("Innlogget bruker har ikke tilgang til oppslått ansatt, {}, {}", callIdArgument(callId), consumerIdArgument(getConsumerId()))
                        call.respond(false)
                    }
                } ?: run {
                    log.warn("Mangler credentials for å authorisere bruker for tilgang til ansatt, {}, {}", callIdArgument(callId), consumerIdArgument(getConsumerId()))
                    call.respond(HttpStatusCode.Unauthorized, "Kan ikke hente tilgang til ansatt: Mangler credentials")
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente tilgang til ansatt med fnr: {}, {}, {}", e.message, callIdArgument(getCallId()), consumerIdArgument(getConsumerId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}
