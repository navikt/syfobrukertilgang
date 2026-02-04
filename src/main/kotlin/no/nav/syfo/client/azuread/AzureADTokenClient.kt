package no.nav.syfo.client.azuread

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.syfo.util.ONE_HUNDRED_AND_TWENTY_SECONDS
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class AzureADTokenClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val clientId: String,
    private val clientSecret: String
) {

    private val azureAdTokenMap: ConcurrentHashMap<String, AzureAdResponse> = ConcurrentHashMap()
    private val tokenMutex = Mutex()

    suspend fun accessToken(scope: String): String {
        val omToMinutter = Instant.now().plusSeconds(ONE_HUNDRED_AND_TWENTY_SECONDS)
        val azureAdResponse = azureAdTokenMap[scope]

        if (azureAdResponse == null || shouldRefresh(azureAdResponse, omToMinutter)) {
            tokenMutex.withLock {
                val refreshed = azureAdTokenMap[scope]
                if (refreshed == null || shouldRefresh(refreshed, omToMinutter)) {
                    LOG.info("Henter nytt token fra Azure AD for scope $scope")
                    val response: HttpResponse = httpClient.post(baseUrl) {
                        accept(ContentType.Application.Json)
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("client_id", clientId)
                                    append("client_secret", clientSecret)
                                    append("grant_type", "client_credentials")
                                    append("scope", scope)
                                }
                            )
                        )
                    }
                    azureAdTokenMap[scope] = response.body()
                }
            }
        }
        return azureAdTokenMap[scope]!!.access_token
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AzureADTokenClient::class.java)
    }

    private fun shouldRefresh(response: AzureAdResponse, omToMinutter: Instant): Boolean {
        return response.issuedOn!!.plusSeconds(response.expires_in).isBefore(omToMinutter)
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

class FetchAzureAdTokenException(message: String) : Exception(message)
