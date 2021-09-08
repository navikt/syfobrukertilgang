package no.nav.syfo.tilgang

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val basePath: String = "/api/v1/tilgang/ansatt"

@KtorExperimentalAPI
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
                    if (ansattTilgangService.hasAccessToAnsatt(loggedInFnr, ansattFnr)) {
                        call.respond(true)
                    } else {
                        LOG.warn("Innlogget bruker har ikke tilgang til oppslått ansatt, {}, {}", callIdArgument(callId), consumerIdArgument(getConsumerId()))
                        call.respond(false)
                    }
                } ?: run {
                    LOG.warn("Mangler credentials for å authorisere bruker for tilgang til ansatt, {}, {}", callIdArgument(callId), consumerIdArgument(getConsumerId()))
                    call.respond(HttpStatusCode.Unauthorized, "Kan ikke hente tilgang til ansatt: Mangler credentials")
                }
            } catch (e: IllegalArgumentException) {
                LOG.warn("Kan ikke hente tilgang til ansatt med fnr: {}, {}, {}", e.message, callIdArgument(getCallId()), consumerIdArgument(getConsumerId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}
