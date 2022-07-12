package no.nav.syfo.client.azuread

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.time.Instant
import kotlin.collections.set

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

    private var azureAdTokenMap: HashMap<String, AzureAdResponse> = HashMap()

    suspend fun accessToken(scope: String): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        val azureAdResponse = azureAdTokenMap[scope]

        if (azureAdResponse == null || azureAdResponse.issuedOn!!.plusSeconds(azureAdResponse.expires_in).isBefore(omToMinutter)) {
            LOG.info("Henter nytt token fra Azure AD for scope $scope")

            val request = FormDataContent(
                Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("grant_type", "client_credentials")
                    append("scope", scope)
                }
            )

            return try {
                val response: HttpResponse = client.post(baseUrl) {
                    accept(ContentType.Application.Json)
                    body = request
                }
                azureAdTokenMap[scope] = response.receive()
                azureAdTokenMap[scope]!!.access_token
            } catch (e: Exception) {
                LOG.error("Henting av token fra Azure AD feiler med message: ${e.message}")
                throw e
            }
        }
        return azureAdTokenMap[scope]!!.access_token
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AzureADTokenClient::class.java)
    }
}

data class AzureAdResponse(
    var access_token: String,
    var token_type: String,
    var expires_in: Long,
    var ext_expires_in: String,
    var expires_on: Instant?,
    var not_before: String?,
    var resource: String?,
    var issuedOn: Instant? = Instant.now()
)
