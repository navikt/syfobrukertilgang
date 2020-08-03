package no.nav.syfo.client.azuread

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.httpPost
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime

class AzureADTokenClient(
    private val baseUrl: String,
    private val clientId: String,
    private val clientSecret: String
) {
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

    fun accessToken(resource: String): Token {
        if (isExpired()) {
            token = requestAccessToken(resource)
            expiry = LocalDateTime.now().plusSeconds(token.expires_in).minusSeconds(10)
        }
        return token
    }

    private fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiry)

    private fun requestAccessToken(resource: String): Token {
        val (_, _, result) = baseUrl.httpPost(
            listOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "grant_type" to "client_credentials",
                "resource" to resource
            )
        ).response()

        return objectMapper.readValue(result.get())
    }

    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AzureADTokenClient::class.java)
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
