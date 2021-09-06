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
import org.slf4j.LoggerFactory
import java.net.ProxySelector
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap

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

    private var azureAdTokenMap: HashMap<String, AzureAdResponse> = HashMap<String, AzureAdResponse>()

    suspend fun accessToken(scope: String): AzureAdResponse? {
        val omToMinutter = Instant.now().plusSeconds(120L)
        val azureAdResponse = azureAdTokenMap[scope]

        if (azureAdResponse == null || azureAdResponse.issuedOn!!.plusSeconds(azureAdResponse.expires_in).isBefore(omToMinutter)) {
            LOG.info(">>>>Henter nytt token fra Azure AD for scope {}", scope)

            LOG.info(">>>>clientId {}", clientId)
            LOG.info(">>>>scope {}", scope)
            LOG.info(">>>>clientSecret {}", clientSecret)

            val formParameters = Parameters.build {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("grant_type", "client_credentials")
                append("scope", scope)
            }

            return try {
                val response: HttpResponse = client.post(baseUrl) {
                    accept(ContentType.Application.Json)
                    body = FormDataContent(formParameters)
                }
                azureAdTokenMap[scope] = response.receive()
                LOG.info(">>>>response AAD {}", response)
                azureAdTokenMap[scope]
            } catch (e: java.lang.IllegalStateException) {
                LOG.error(">>>>Exception when accessing aad msg ${e.message}")
                return null
            }

            /*     val response: HttpResponse = client.post(baseUrl) {
          accept(ContentType.Application.Json)
          body = FormDataContent(Parameters.build {
              append("client_id", clientId)
              append("scope", scope)
              append("grant_type", "client_credentials")
              append("client_secret", clientSecret)
          })
      }*/
/*            LOG.info(">>>>Response from AD endpoint: $response")
            return when (response.status) {
                HttpStatusCode.OK -> {
                    azureAdTokenMap[scope] = response.receive()
                    azureAdTokenMap[scope]
                }
                else -> {
                    LOG.error(">>>>NOT OK Response from AD endpoint: $response")
                    throw IllegalStateException("Henting av token fra Azure AD feiler med HTTPstatus: ${response.status.value}")
                }
            }*/
        }
        return Objects.requireNonNull<AzureAdResponse>(azureAdTokenMap[scope])
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
