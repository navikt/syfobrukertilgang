package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.*
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.InternalAPI
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.log
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.LEDER_FNR
import no.nav.syfo.testutil.generateJWT
import no.nav.syfo.tilgang.basePath
import no.nav.syfo.tilgang.registerAnsattTilgangApi
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths

@InternalAPI
object AnsattTilgangApiSpek : Spek({

    val issuerUrl = "https://sts.issuer.net/myid"

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()
    val consumerClientId = "1"
    val acceptedClientId = "2"
    val notAcceptedClientId = "4"

    describe("AnsattTilgangApi") {
        with(TestApplicationEngine()) {
            start()

            application.installAuthentication(
                    jwkProvider,
                    issuerUrl,
                    acceptedClientId

            )
            application.routing {
                authenticate("jwt") {
                    registerAnsattTilgangApi()
                }
            }

            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }
            application.install(StatusPages) {
                exception<Throwable> { cause ->
                    call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
                    log.error("Caught exception", cause)
                    throw cause
                }
            }

            describe("Check access to Ansatt") {

                describe("with valid JWT and accepted audience") {
                    it("should return 200 true when Ansatt is self") {
                        with(handleRequest(HttpMethod.Get, getEndpointUrl(ARBEIDSTAKER_FNR)) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(generateJWT(consumerClientId, acceptedClientId)
                                    ?: ""))
                        }) {
                            response.status() shouldEqual HttpStatusCode.OK
                            response.content shouldEqual true.toString()
                        }
                    }

                    it("should return 200 false when Ansatt is not self") {
                        with(handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR)) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(generateJWT(consumerClientId, acceptedClientId)
                                    ?: ""))
                        }) {
                            response.status() shouldEqual HttpStatusCode.OK
                            response.content shouldEqual false.toString()
                        }
                    }
                }

                it("should return 401 with valid JWT and unaccepted audience") {
                    with(handleRequest(HttpMethod.Get, getEndpointUrl(ARBEIDSTAKER_FNR)) {
                        addHeader(HttpHeaders.Authorization, bearerHeader(generateJWT(consumerClientId, notAcceptedClientId)
                                ?: ""))
                    }) {
                        response.status() shouldEqual HttpStatusCode.Unauthorized
                        response.content shouldEqual null
                    }
                }
            }
        }
    }
})

fun getEndpointUrl(ansattFnr: String): String {
    return "$basePath/$ansattFnr"
}