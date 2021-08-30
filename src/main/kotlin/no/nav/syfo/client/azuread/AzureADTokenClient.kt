package no.nav.syfo.client.azuread

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector
import java.time.Instant
import java.time.LocalDateTime

class AzureADTokenClient(
    private val baseUrl: String,
    private val clientId: String,
    private val clientSecret: String
) {
    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
    }

    private var token: Token = Token(
        token_type = "not a token",
        expires_in = 0,
        ext_expires_in = 0,
        access_token = "not a token",
        expires_on = Instant.now(),
        not_before = "",
        resource = ""
    )
    private var expiry: LocalDateTime = LocalDateTime.now().minusYears(100)

    suspend fun accessToken(resource: String): Token {
        if (isExpired()) {
            token = requestAccessToken(resource)
            expiry = LocalDateTime.now().plusSeconds(token.expires_in).minusSeconds(10)
        }
        return token
    }

    private fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiry)

    private suspend fun requestAccessToken(resource: String): Token {
        val response: HttpResponse = client.post(baseUrl) {
            body = FormDataContent(Parameters.build {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("grant_type", "client_credentials")
                append("scope", resource)
            })
            accept(ContentType.Application.Json)
        }
        return response.receive()
    }
}

data class Token(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
    val ext_expires_in: Long,
    val expires_on: Instant?,
    val not_before: String?,
    val resource: String?
)
