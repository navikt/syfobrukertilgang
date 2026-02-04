package no.nav.syfo.client.narmesteleder

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.exception.DependencyUnavailableException
import no.nav.syfo.util.configure

class NarmestelederClientTest : FunSpec({
    val tokenClient = mockk<AzureADTokenClient>()
    coEvery { tokenClient.accessToken(any()) } returns "token"

    test("returns ansatte on 200 response") {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                jackson { configure() }
            }
            engine {
                addHandler { _ ->
                    respond(
                        content = """
                            [
                              {
                                "narmesteLederId": "00000000-0000-0000-0000-000000000000",
                                "fnr": "123",
                                "orgnummer": "999",
                                "narmesteLederFnr": "456",
                                "narmesteLederTelefonnummer": null,
                                "narmesteLederEpost": null,
                                "aktivFom": "2020-01-01",
                                "aktivTom": null,
                                "arbeidsgiverForskutterer": null,
                                "skrivetilgang": true,
                                "tilganger": ["OPPFOLGINGSPLAN"]
                              }
                            ]
                        """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            }
        }

        val narmestelederClient = NarmestelederClient(
            httpClient = client,
            narmestelederUrl = "http://narmesteleder",
            narmestelederScope = "scope",
            azureAdTokenClient = tokenClient
        )

        val ansatte = narmestelederClient.ansatte("123", "callid", "consumer")
        ansatte.size shouldBe 1
        ansatte.first().fnr shouldBe "123"
    }

    test("throws DependencyUnavailableException on 500 response") {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                jackson { configure() }
            }
            engine {
                addHandler { _ ->
                    respond(
                        content = """{"message":"error"}""",
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            }
        }

        val narmestelederClient = NarmestelederClient(
            httpClient = client,
            narmestelederUrl = "http://narmesteleder",
            narmestelederScope = "scope",
            azureAdTokenClient = tokenClient
        )

        shouldThrow<DependencyUnavailableException> {
            narmestelederClient.ansatte("123", "callid", "consumer")
        }
    }

    test("throws DependencyUnavailableException on 401 response") {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                jackson { configure() }
            }
            engine {
                addHandler { _ ->
                    respond(
                        content = """{"message":"unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            }
        }

        val narmestelederClient = NarmestelederClient(
            httpClient = client,
            narmestelederUrl = "http://narmesteleder",
            narmestelederScope = "scope",
            azureAdTokenClient = tokenClient
        )

        shouldThrow<DependencyUnavailableException> {
            narmestelederClient.ansatte("123", "callid", "consumer")
        }
    }

    test("throws DependencyUnavailableException on 403 response") {
        val client = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                jackson { configure() }
            }
            engine {
                addHandler { _ ->
                    respond(
                        content = """{"message":"forbidden"}""",
                        status = HttpStatusCode.Forbidden,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            }
        }

        val narmestelederClient = NarmestelederClient(
            httpClient = client,
            narmestelederUrl = "http://narmesteleder",
            narmestelederScope = "scope",
            azureAdTokenClient = tokenClient
        )

        shouldThrow<DependencyUnavailableException> {
            narmestelederClient.ansatte("123", "callid", "consumer")
        }
    }
})
