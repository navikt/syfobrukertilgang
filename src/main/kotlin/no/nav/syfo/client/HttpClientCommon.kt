package no.nav.syfo.client

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.jackson.*
import no.nav.syfo.util.configure
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.io.IOException
import java.net.ProxySelector
import java.util.concurrent.TimeUnit

const val RETRY_DELAY = 500L

private fun HttpClientConfig<out HttpClientEngineConfig>.commonConfig(expectSuccess: Boolean) {
    install(ContentNegotiation) {
        jackson { configure() }
    }
    install(HttpTimeout) {
        connectTimeoutMillis = TimeUnit.SECONDS.toMillis(2)
        requestTimeoutMillis = TimeUnit.SECONDS.toMillis(5)
        socketTimeoutMillis = TimeUnit.SECONDS.toMillis(5)
    }
    install(HttpRequestRetry) {
        retryOnExceptionIf(1) { _, cause ->
            cause is IOException || cause is ConnectTimeoutException || cause is SocketTimeoutException
        }
        constantDelay(RETRY_DELAY)
    }
    this.expectSuccess = expectSuccess
}

val defaultConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {
    commonConfig(expectSuccess = false)
}

val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
    commonConfig(expectSuccess = true)
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

private val defaultClient: HttpClient by lazy { HttpClient(CIO, defaultConfig) }
private val proxyClient: HttpClient by lazy { HttpClient(Apache, proxyConfig) }

fun httpClientDefault(): HttpClient = defaultClient
fun httpClientProxy(): HttpClient = proxyClient
