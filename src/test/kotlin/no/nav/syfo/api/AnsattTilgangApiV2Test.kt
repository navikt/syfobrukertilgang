package no.nav.syfo.api

import com.auth0.jwk.JwkProviderBuilder
import io.kotest.assertions.ktor.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.application.installContentNegotiation
import no.nav.syfo.application.installStatusPages
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.env
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.LEDER_FNR
import no.nav.syfo.testutil.generateTokenXToken
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.BASE_PATH_V2
import no.nav.syfo.tilgang.registerAnsattTilgangApiV2
import no.nav.syfo.util.bearerHeader
import java.net.URL
import java.nio.file.Path

class AnsattTilgangApiV2Test : DescribeSpec({
    val narmestelederClientMock = mockk<NarmestelederClient>(relaxed = true)

    val ansattTilgangService = AnsattTilgangService(narmestelederClientMock)

    val resource = "src/test/resources/jwkset.json"
    val url: URL = Path.of(resource).toUri().toURL()
    val notAcceptedClientId = "4"
    val jwkProviderTokenx = JwkProviderBuilder(url).build()
    val tokenXIssuer = "tokenx-issuer"

    val ansatte: List<Ansatt> = listOf(
        Ansatt(
            virksomhetsnummer = "",
            fnr = ARBEIDSTAKER_FNR
        )
    )

    beforeSpec {
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
                jwkProviderTokenx,
                tokenXIssuer
            )
            application.routing {
                authenticate("tokenx") {
                    registerAnsattTilgangApiV2(ansattTilgangService)
                }
            }

            application.installContentNegotiation()
            application.installStatusPages()

            describe("Check access to Ansatt") {

                describe("with valid JWT and accepted audience") {
                    it("should return 200 false when not leader of Ansatt") {
                        with(
                            handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR)) {
                                addHeader(
                                    HttpHeaders.Authorization,
                                    bearerHeader(
                                        generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                            ?: ""
                                    )
                                )
                            }
                        ) {
                            response shouldHaveStatus HttpStatusCode.OK
                            response.content shouldBe false.toString()
                        }
                    }

                    it("should return 200 true when leader of Ansatt") {
                        with(
                            handleRequest(HttpMethod.Get, getEndpointUrl(ARBEIDSTAKER_FNR)) {
                                addHeader(
                                    HttpHeaders.Authorization,
                                    bearerHeader(
                                        generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                            ?: ""
                                    )
                                )
                            }
                        ) {
                            response shouldHaveStatus HttpStatusCode.OK
                            response.content shouldBe true.toString()
                        }
                    }
                }

                it("should return 401 with valid JWT and unaccepted audience") {
                    with(
                        handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR)) {
                            addHeader(
                                HttpHeaders.Authorization,
                                bearerHeader(
                                    generateTokenXToken(notAcceptedClientId, tokenXIssuer)
                                        ?: ""
                                )
                            )
                        }
                    ) {
                        response shouldHaveStatus HttpStatusCode.Unauthorized
                        response.content shouldBe null
                    }
                }

                it("should return 401 if credentials are missing") {
                    with(handleRequest(HttpMethod.Get, getEndpointUrl(LEDER_FNR))) {
                        response shouldHaveStatus HttpStatusCode.Unauthorized
                        response.content shouldBe null
                    }
                }
            }
        }
    }
})

fun getEndpointUrl(ansattFnr: String): String {
    return "$BASE_PATH_V2/$ansattFnr"
}
