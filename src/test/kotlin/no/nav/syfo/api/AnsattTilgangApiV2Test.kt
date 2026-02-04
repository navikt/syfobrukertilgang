package no.nav.syfo.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import java.net.URL
import java.nio.file.Path
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.application.installContentNegotiation
import no.nav.syfo.application.installStatusPages
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.client.narmesteleder.domain.Ansatt
import no.nav.syfo.exception.DependencyUnavailableException
import no.nav.syfo.env
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.LEDER_FNR
import no.nav.syfo.testutil.generateTokenXToken
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.BASE_PATH_V2
import no.nav.syfo.tilgang.registerAnsattTilgangApiV2
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

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
            narmestelederClientMock.ansatte(ARBEIDSTAKER_FNR, any(), any())
        } returns ansatte

        coEvery {
            narmestelederClientMock.ansatte(LEDER_FNR, any(), any())
        } returns ansatte
    }

    describe("AnsattTilgangApi") {
        describe("Check access to Ansatt") {

            describe("with valid JWT and accepted audience") {
                it("should return 200 false when not leader of Ansatt") {
                    getTestApplication(ansattTilgangService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {
                            headers.append(
                                "Authorization",
                                generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                    ?: ""
                            )
                            headers.append(NAV_PERSONIDENT_HEADER, LEDER_FNR)
                        }
                        response shouldHaveStatus HttpStatusCode.OK
                        response.body<String>() shouldBe false.toString()
                    }
                }

                it("should return 200 true when leader of Ansatt") {
                    getTestApplication(ansattTilgangService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {
                            headers.append(
                                "Authorization",
                                generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                    ?: ""
                            )
                            headers.append(NAV_PERSONIDENT_HEADER, ARBEIDSTAKER_FNR)
                        }
                        response shouldHaveStatus HttpStatusCode.OK
                        response.body<String>() shouldBe true.toString()
                    }
                }

                it("should return 503 when dependency fails") {
                    val failingClient = mockk<NarmestelederClient>()
                    val failingService = AnsattTilgangService(failingClient)
                    coEvery {
                        failingClient.ansatte(LEDER_FNR, any(), any())
                    } throws DependencyUnavailableException("down")

                    getTestApplication(failingService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {
                            headers.append(
                                "Authorization",
                                generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                    ?: ""
                            )
                            headers.append(NAV_PERSONIDENT_HEADER, LEDER_FNR)
                        }
                        response shouldHaveStatus HttpStatusCode.ServiceUnavailable
                    }
                }
            }

            describe("with valid JWT and unacceptet personident or audience") {
                it("should return 400 with missing personident header") {
                    getTestApplication(ansattTilgangService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {
                            headers.append(
                                "Authorization",
                                generateTokenXToken(env.syfobrukertilgangTokenXClientId, tokenXIssuer)
                                    ?: ""
                            )
                        }
                        response shouldHaveStatus HttpStatusCode.BadRequest
                        response.body<String>() shouldBe "Fnr mangler"
                    }
                }


                it("should return 401 with valid JWT and unaccepted audience") {
                    getTestApplication(ansattTilgangService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {
                            headers.append(
                                "Authorization", generateTokenXToken(notAcceptedClientId, tokenXIssuer)
                                    ?: ""
                            )
                            headers.append(NAV_PERSONIDENT_HEADER, LEDER_FNR)
                        }
                        response shouldHaveStatus HttpStatusCode.Unauthorized
                    }
                }

                it("should return 401 if credentials are missing") {
                    getTestApplication(ansattTilgangService, jwkProviderTokenx) {
                        val response = client.get(BASE_PATH_V2) {}
                        response shouldHaveStatus HttpStatusCode.Unauthorized
                    }
                }
            }
        }
    }
})

private fun getTestApplication(
    ansattTilgangService: AnsattTilgangService,
    jwkProviderTokenx: JwkProvider,
    fn: suspend ApplicationTestBuilder.() -> Unit
) {
    testApplication {
        application {
            installContentNegotiation()
            installStatusPages()
            installAuthentication(
                jwkProviderTokenx, "tokenx-issuer"
            )
            routing {
                authenticate("tokenx") {
                    registerAnsattTilgangApiV2(ansattTilgangService)
                }
            }
        }
        fn(this)
    }
}
