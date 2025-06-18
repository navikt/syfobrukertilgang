package no.nav.syfo.tilgang

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.consumerIdArgument
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerId
import no.nav.syfo.util.getPersonIdent
import no.nav.syfo.util.validateFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo")
private const val PID_CLAIM_NAME = "pid"
const val BASE_PATH_V2: String = "/api/v2/tilgang/ansatt"

fun Route.registerAnsattTilgangApiV2(ansattTilgangService: AnsattTilgangService) {
    route(BASE_PATH_V2) {
        get() {
            try {
                val ansattFnr: String =
                    getPersonIdent()?.takeIf { validateFnr(it) } ?: throw IllegalArgumentException("Fnr mangler")

                val credentials = call.principal<JWTPrincipal>()
                val callId = call.getCallId()
                credentials?.let { creds ->
                    val pidClaim = creds.payload.getClaim(PID_CLAIM_NAME)
                    if (pidClaim.isNull) {
                        LOG.warn(
                            "Mangler credentials for å authorisere bruker for tilgang til ansatt, {}, {}",
                            callIdArgument(callId),
                            consumerIdArgument(getConsumerId())
                        )
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            "Kan ikke hente tilgang til ansatt: 'pid'-claim mangler i token fra id-porten"
                        )
                    } else {
                        val loggedInFnr = pidClaim.asString()
                        if (ansattTilgangService.hasAccessToAnsatt(loggedInFnr, ansattFnr)) {
                            call.respond(true)
                        } else {
                            LOG.warn(
                                "Innlogget bruker har ikke tilgang til oppslått ansatt, {}, {}",
                                callIdArgument(callId),
                                consumerIdArgument(getConsumerId())
                            )
                            call.respond(false)
                        }
                    }
                } ?: run {
                    LOG.warn(
                        "Mangler credentials for å authorisere bruker for tilgang til ansatt, {}, {}",
                        callIdArgument(callId),
                        consumerIdArgument(getConsumerId())
                    )
                    call.respond(HttpStatusCode.Unauthorized, "Kan ikke hente tilgang til ansatt: Mangler credentials")
                }
            } catch (e: IllegalArgumentException) {
                LOG.error(
                    "Kan ikke hente tilgang til ansatt med fnr: {}, {}, {}",
                    e.message,
                    callIdArgument(call.getCallId()),
                    consumerIdArgument(getConsumerId())
                )
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}
