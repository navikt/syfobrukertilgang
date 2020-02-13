package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.authenticate
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.api.*
import no.nav.syfo.application.installAuthentication
import no.nav.syfo.client.aktor.AktorService
import no.nav.syfo.client.aktor.AktorregisterClient
import no.nav.syfo.client.azuread.AzureADTokenClient
import no.nav.syfo.client.narmesteleder.NarmestelederClient
import no.nav.syfo.client.sts.StsRestClient
import no.nav.syfo.tilgang.AnsattTilgangService
import no.nav.syfo.tilgang.registerAnsattTilgangApi
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.MainApplicationKt")

@KtorExperimentalAPI
fun main() {
    val vaultSecrets = VaultSecrets(
            clientId = getFileAsString("/secrets/azuread/syfobrukertilgang/client_id"),
            clientSecret = getFileAsString("/secrets/azuread/syfobrukertilgang/client_secret"),
            serviceuserPassword = getFileAsString("/secrets/serviceuser/password"),
            serviceuserUsername = getFileAsString("/secrets/serviceuser/username")
    )

    val server = embeddedServer(Netty, applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = env.applicationPort
        }

        module {
            init()
            serverModule(vaultSecrets)
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10, TimeUnit.SECONDS)
    })

    server.start(wait = false)
}


val state: ApplicationState = ApplicationState(running = false, initialized = false)
val env: Environment = getEnvironment()

fun Application.init() {
    isDev {
        state.running = true
    }

    isProd {
        state.running = true
    }
}

@KtorExperimentalAPI
fun Application.serverModule(vaultSecrets: VaultSecrets) {
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
    installAuthentication(
            jwkProvider,
            wellKnown.issuer,
            env.aadb2cClientId
    )

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception", cause, getCallId())
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

    val stsClientRest = StsRestClient(env.stsRestUrl, vaultSecrets.serviceuserUsername, vaultSecrets.serviceuserPassword)
    val aktorregisterClient = AktorregisterClient(env.aktoerregisterV1Url, stsClientRest)
    val aktorService = AktorService(aktorregisterClient)

    val azureADTokenClient = AzureADTokenClient(
            baseUrl = env.aadAccessTokenUrl,
            clientId = vaultSecrets.clientId,
            clientSecret = vaultSecrets.clientSecret
    )
    val narmestelederClient = NarmestelederClient(
            env.narmestelederUrl,
            env.narmestelederId,
            azureADTokenClient
    )

    val ansattTilgangService = AnsattTilgangService(aktorService, narmestelederClient)

    routing {
        registerPodApi(state)
        registerPrometheusApi()
        authenticate("jwt") {
            registerAnsattTilgangApi(ansattTilgangService)
        }
    }

    state.initialized = true
}

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
