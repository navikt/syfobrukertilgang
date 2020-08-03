package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import net.logstash.logback.argument.StructuredArguments

fun Application.installAuthentication(
    jwkProvider: JwkProvider,
    issuer: String,
    aadb2cClientId: String
) {
    install(Authentication) {
        jwt(name = "jwt") {
            verifier(jwkProvider, issuer)
            validate { credentials ->
                if (!credentials.payload.audience.contains(aadb2cClientId)) {
                    log.warn(
                        "Auth: Unexpected audience for jwt {}, {}, {}",
                        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
                        StructuredArguments.keyValue("audience", credentials.payload.audience),
                        StructuredArguments.keyValue("expectedAudience", aadb2cClientId)
                    )
                    null
                } else {
                    JWTPrincipal(credentials.payload)
                }
            }
        }
    }
}
