package no.nav.syfo.client.sts

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
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket

data class OidcToken(
    val access_token: String,
    val expires_in: Long,
    val token_type: String
)

@InternalAPI
object StsRestClientTest : Spek({
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
                get("/rest/v1/sts/token") {
                    val params = call.request.queryParameters
                    if (params["grant_type"].equals("client_credentials") && params["scope"].equals("openid")) {
                        call.respond(default_token)
                    }
                }
            }
        }.start()

        val stsRestClient = StsRestClient(
            baseUrl = mockHttpServerUrl,
            username = "username",
            password = "password"
        )

        afterGroup {
            mockServer.stop(1L, 10L)
        }

        describe("OIDC Token") {
            it("should parse a token successfully") {
                val token: String = stsRestClient.token()

                token shouldEqual default_token.access_token
            }
        }
    }
})

private val default_token = OidcToken(
    access_token = "default access token",
    expires_in = 3600,
    token_type = "Bearer"
)
