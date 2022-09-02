package no.nav.syfo.tilgang

import io.ktor.application.call
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.util.callIdArgument
import no.nav.syfo.util.consumerIdArgument
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerId
import no.nav.syfo.util.validateFnr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo")
private val PID_CLAIM_NAME = "pid"
const val basePathV2: String = "/api/v2/tilgang/ansatt"

@KtorExperimentalAPI
fun Route.registerAnsattTilgangApiV2(ansattTilgangService: AnsattTilgangService) {
    route(basePathV2) {
        get("/{fnr}") {
            try {
                val ansattFnr: String =
                    call.parameters["fnr"]?.takeIf { validateFnr(it) } ?: throw IllegalArgumentException("Fnr mangler")

                val credentials = call.principal<JWTPrincipal>()
                val callId = getCallId()
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
                    callIdArgument(getCallId()),
                    consumerIdArgument(getConsumerId())
                )
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilgang til ansatt")
            }
        }
    }
}
