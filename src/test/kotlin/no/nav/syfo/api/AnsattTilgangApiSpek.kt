package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.env
import no.nav.syfo.log
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.LEDER_FNR
import no.nav.syfo.testutil.generateTokenXToken
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.basePathV2
import no.nav.syfo.tilgang.registerAnsattTilgangApiV2
import no.nav.syfo.util.bearerHeader
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.nio.file.Paths

@InternalAPI
object AnsattTilgangApiSpek : Spek({
    val narmestelederClientMock = mockk<NarmestelederClient>()

    val ansattTilgangService = AnsattTilgangService(narmestelederClientMock)

    val issuerUrl = "https://sts.issuer.net/myid"

    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    val jwkProvider = JwkProviderBuilder(uri).build()
    val acceptedClientId = "2"
    val notAcceptedClientId = "4"
    val jwkProviderTokenx = JwkProviderBuilder(uri).build()
    val tokenXIssuer = "tokenx-issuer"

    val ansatte = listOf(
        Ansatt(
            virksomhetsnummer = "",
            fnr = ARBEIDSTAKER_FNR
        )
    )

    beforeEachTest {
        coEvery {
            narmestelederClientMock.ansatte(ARBEIDSTAKER_FNR)
        } returns ansatte

        coEvery {
            narmestelederClientMock.ansatte(LEDER_FNR)
        } returns ansatte
    }

    describe("AnsattTilgangApi") {
        with(TestApplicationEngine()) {
            start()

            application.installAuthentication(
                jwkProvider,
                issuerUrl,
                acceptedClientId,
                jwkProviderTokenx,
                tokenXIssuer
            )
            application.routing {
                authenticate("tokenx") {
                    registerAnsattTilgangApiV2(ansattTilgangService)
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
                    log.error("Caught exception: ${cause.message}", cause)
                    throw cause
                }
            }

            describe("Check access to Ansatt") {

                describe("with valid JWT and accepted audience") {
                    it("should return 200 false when not leader of Ansatt") {
                        with(
                            handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR)) {
                                addHeader(
                                    HttpHeaders.Authorization,
                                    bearerHeader(
                                        generateTokenXToken(env.syfobrukertilgangTokenXClientId, issuer = tokenXIssuer)
                                            ?: ""
                                    )
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            response.content shouldBeEqualTo false.toString()
                        }
                    }

                    it("should return 200 true when leader of Ansatt") {
                        with(
                            handleRequest(HttpMethod.Get, getEndpointUrl(ARBEIDSTAKER_FNR)) {
                                addHeader(
                                    HttpHeaders.Authorization,
                                    bearerHeader(
                                        generateTokenXToken(env.syfobrukertilgangTokenXClientId, issuer = tokenXIssuer)
                                            ?: ""
                                    )
                                )
                            }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK
                            response.content shouldBeEqualTo true.toString()
                        }
                    }
                }

                it("should return 401 with valid JWT and unaccepted audience") {
                    with(
                        handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR)) {
                            addHeader(
                                HttpHeaders.Authorization,
                                bearerHeader(
                                    generateTokenXToken(notAcceptedClientId)
                                        ?: ""
                                )
                            )
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                        response.content shouldBeEqualTo null
                    }
                }

                it("should return 401 if credentials are missing") {
                    with(handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR))) {
                        response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                        response.content shouldBeEqualTo null
                    }
                }
            }
        }
    }
})

fun getEndpointUrl(ansattFnr: String): String {
    return "$basePathV2/$ansattFnr"
}
