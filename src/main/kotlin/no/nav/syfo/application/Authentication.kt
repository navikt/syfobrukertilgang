package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.env
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo.application")

fun Application.installAuthentication(
    jwkProviderTokenX: JwkProvider,
    tokenXIssuer: String
) {
    install(Authentication) {
        jwt(name = "tokenx") {
            authHeader {
                if (it.getToken() == null) {
                    return@authHeader null
                }
                return@authHeader HttpAuthHeader.Single("Bearer", it.getToken()!!)
            }
            verifier(jwkProviderTokenX, tokenXIssuer)
            validate { credentials ->
                when {
                    hasSyfobrukertilgangAudience(
                        credentials, env.syfobrukertilgangTokenXClientId
                    ) && isNiva4(credentials) -> {
                        JWTPrincipal(credentials.payload)
                    }

                    else -> {
                        LOG.warn(
                            "Auth: Unexpected audience for jwt {}, {}, {}",
                            StructuredArguments.keyValue("issuer", credentials.payload.issuer),
                            StructuredArguments.keyValue("audience", credentials.payload.audience),
                            StructuredArguments.keyValue("acr claim", credentials.payload.getClaim("acr").asString()),
                        )
                        null
                    }
                }
            }
        }
    }
}

fun ApplicationCall.getToken(): String? {
    if (request.header("Authorization") != null) {
        return request.header("Authorization")!!.removePrefix("Bearer ")
    }
    return null
}

fun hasSyfobrukertilgangAudience(credentials: JWTCredential, clientId: String): Boolean {
    return credentials.payload.audience.contains(clientId)
}

fun isNiva4(credentials: JWTCredential): Boolean {
    return "Level4" == credentials.payload.getClaim("acr").asString()
}
