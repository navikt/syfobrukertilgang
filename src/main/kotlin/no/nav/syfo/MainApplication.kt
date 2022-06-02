package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.api.getWellKnown
import no.nav.syfo.api.getWellKnownWellKnownTokenX
import no.nav.syfo.api.registerPodApi
import no.nav.syfo.api.registerPrometheusApi
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.registerAnsattTilgangApi
import no.nav.syfo.tilgang.registerAnsattTilgangApiV2
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getConsumerId
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.MainApplicationKt")

@KtorExperimentalAPI
fun main() {
    val server = embeddedServer(
        Netty,
        applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            config = HoconApplicationConfig(ConfigFactory.load())

            connector {
                port = env.applicationPort
            }

            module {
                init()
                serverModule()
            }
        }
    )
    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}

val state: ApplicationState = ApplicationState(running = false, initialized = false)
val env: Environment = getEnvironment()

@KtorExperimentalAPI
fun Application.init() {
    isDev {
        state.running = true
    }

    isProd {
        state.running = true
    }
}

@KtorExperimentalAPI
fun Application.serverModule() {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

    install(CallId) {
        retrieve { it.request.headers["X-Nav-CallId"] }
        retrieve { it.request.headers[HttpHeaders.XCorrelationId] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }

    val wellKnown = getWellKnown(env.aadb2cDiscoveryUrl)
    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    val wellKnownTokenX = getWellKnownWellKnownTokenX(env.tokenXWellKnownUrl)

    val jwkProviderTokenX = JwkProviderBuilder(URL(wellKnownTokenX.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    installAuthentication(
        jwkProvider,
        wellKnown.issuer,
        env.aadb2cClientId,
        jwkProviderTokenX,
        wellKnownTokenX.issuer
    )

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception: ${cause.message}", cause, getCallId(), getConsumerId())
            throw cause
        }
    }

    isProd {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.contains(Regex("is_alive|is_ready|prometheus"))) {
                proceed()
                return@intercept
            }
        }
    }

    val azureADTokenClient = AzureADTokenClient(
        baseUrl = env.aadTokenEndpoint,
        clientId = env.aadClientId,
        clientSecret = env.aadClientSecret
    )

    val narmestelederClient = NarmestelederClient(
        env.narmestelederUrl,
        env.narmestelederScope,
        azureADTokenClient
    )

    val ansattTilgangService = AnsattTilgangService(narmestelederClient)

    routing {
        registerPodApi(state)
        registerPrometheusApi()
        authenticate("jwt") {
            registerAnsattTilgangApi(ansattTilgangService)
        }
        authenticate("tokenx") {
            registerAnsattTilgangApiV2(ansattTilgangService)
        }
    }

    state.initialized = true
}

@KtorExperimentalAPI
val Application.envKind
    get() = environment.config.property("ktor.environment").getString()

@KtorExperimentalAPI
fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

@KtorExperimentalAPI
fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
