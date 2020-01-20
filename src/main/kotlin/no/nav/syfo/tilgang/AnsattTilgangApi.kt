package no.nav.syfo.tilgang

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val basePath: String = "/api/v1/tilgang/ansatt"

fun Route.registerAnsattTilgangApi(
) {
    route(basePath) {
        get("/{fnr}") {
            try {
                val fnr: String = call.parameters["fnr"]?.takeIf { validateFnr(it) }
                        ?: throw IllegalArgumentException("Fnr mangler")

                val callId = getCallId()

                when (true) {
                    true -> {
                        call.respond(HttpStatusCode.NoContent)
                    }
                    else -> {
                        log.error("Innlogget bruker har ikke tilgang til oppslått ansatt, {}", CallIdArgument(callId))
                        call.respond(HttpStatusCode.Forbidden, "Ikke tilgang til ansatt")
                    }
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente tilgang til ansatt med fnr: {}, {}", e.message, CallIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}
