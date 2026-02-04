package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.syfo.api.registerPodApi
import no.nav.syfo.api.registerPrometheusApi
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.application.installCallId
import no.nav.syfo.application.installContentNegotiation
import no.nav.syfo.application.installStatusPages
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.client.httpClientProxy
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.registerAnsattTilgangApiV2
import no.nav.syfo.util.jwkProvider
import no.nav.syfo.wellknown.getWellKnown
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val state: ApplicationState = ApplicationState()
const val SERVER_SHUTDOWN_GRACE_PERIOD = 10L
const val SERVER_SHUTDOWN_TIMEOUT = 10L

fun main() {
    val server = embeddedServer(
        factory = Netty,
        environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            config = HoconApplicationConfig(ConfigFactory.load())
        },
        configure = {
            connector {
                port = env.applicationPort
            }

        },
        module = {
            state.running = true
            serverModule()
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(SERVER_SHUTDOWN_GRACE_PERIOD, SERVER_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}

val env: Environment = getEnvironment()

fun Application.serverModule() {
    installContentNegotiation()
    installCallId()

    val wellKnownTokenX = getWellKnown(
        wellKnownUrl = env.tokenXWellKnownUrl
    )

    val jwkProviderTokenX = jwkProvider(wellKnownTokenX.jwksUri)

    installAuthentication(
        jwkProviderTokenX,
        wellKnownTokenX.issuer
    )

    installStatusPages()

    isProd {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.contains(Regex("is_alive|is_ready|prometheus"))) {
                proceed()
                return@intercept
            }
        }
    }

    val azureADTokenClient = AzureADTokenClient(
        httpClient = httpClientProxy(),
        baseUrl = env.aadTokenEndpoint,
        clientId = env.aadClientId,
        clientSecret = env.aadClientSecret
    )

    val narmestelederClient = NarmestelederClient(
        httpClient = httpClientDefault(),
        env.narmestelederUrl,
        env.narmestelederScope,
        azureADTokenClient
    )

    val ansattTilgangService = AnsattTilgangService(narmestelederClient)

    routing {
        registerPodApi(state)
        registerPrometheusApi()
        authenticate("tokenx") {
            registerAnsattTilgangApiV2(ansattTilgangService)
        }
    }

    state.initialized = true
}

val Application.envKind
    get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}

data class ApplicationState(var running: Boolean = false, var initialized: Boolean = false)
