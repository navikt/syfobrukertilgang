package no.nav.syfo.client.aktor

import arrow.core.Either
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.env
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_AKTORID
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import kotlin.test.assertTrue

data class RSIdent(
    val ident: String,
    val identgruppe: String,
    val gjeldende: Boolean
)

data class RSAktor(
    val identer: List<RSIdent>? = null,
    val feilmelding: String? = null
)

@InternalAPI
object AktorregisterClientTest : Spek({
    val stsOidcClientMock = mockk<StsRestClient>()

    with(TestApplicationEngine()) {
        start()

        application.install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }

        val mockHttpServerPort = ServerSocket(0).use { it.localPort }
        val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
        val mockServer = embeddedServer(Netty, mockHttpServerPort) {
            install(ContentNegotiation) {
                jackson {}
            }
            routing {
                get("/${env.aktoerregisterV1Url}/identer") {
                    when (call.request.headers["Nav-Personidenter"]) {
                        ARBEIDSTAKER_FNR -> {
                            call.respond(mapOf(ARBEIDSTAKER_FNR to RSAktor(
                                listOf(RSIdent(
                                    ident = ARBEIDSTAKER_FNR,
                                    identgruppe = IdentType.NorskIdent.name,
                                    gjeldende = true
                                )),
                                feilmelding = null
                            )))
                        }
                        ARBEIDSTAKER_AKTORID -> {
                            call.respond(mapOf(ARBEIDSTAKER_AKTORID to RSAktor(
                                listOf(RSIdent(
                                    ident = ARBEIDSTAKER_AKTORID,
                                    identgruppe = IdentType.AktoerId.name,
                                    gjeldende = true
                                )),
                                feilmelding = null
                            )))
                        }
                        else -> error("Something went wrong")
                    }
                }
            }
        }.start()

        val aktoerIdClient = AktorregisterClient("$mockHttpServerUrl/${env.aktoerregisterV1Url}", stsOidcClientMock)

        beforeEachTest {
            coEvery { stsOidcClientMock.token() } returns "oidctoken"
        }

        afterEachTest {
        }

        afterGroup {
            mockServer.stop(1L, 10L)
        }

        describe("AktorIdClient successful") {
            it("Get fnr for aktor that exists") {
                var fnr: String? = null
                runBlocking {
                    val lookupResult = aktoerIdClient.getIdenter(ARBEIDSTAKER_FNR, "callId")
                    assertTrue(lookupResult is Either.Right)
                    fnr = lookupResult.b.first { it.type == IdentType.NorskIdent }.ident
                }

                fnr shouldEqual ARBEIDSTAKER_FNR
            }

            it("Get aktør for fnr that exists") {
                var aktorId: String? = null
                runBlocking {
                    val lookupResult = aktoerIdClient.getIdenter(ARBEIDSTAKER_AKTORID, "callId")
                    assertTrue(lookupResult is Either.Right)
                    aktorId = lookupResult.b.first { it.type == IdentType.AktoerId }.ident
                }

                aktorId shouldEqual ARBEIDSTAKER_AKTORID
            }
        }
    }
})
