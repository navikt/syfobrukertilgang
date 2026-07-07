package no.nav.syfo.client.azuread

import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import no.nav.syfo.client.httpClientProxy
import no.nav.syfo.util.ONE_HUNDRED_AND_TWENTY_SECONDS
import org.slf4j.LoggerFactory
import java.time.Instant

class AzureADTokenClient(
    private val baseUrl: String,
    private val clientId: String,
    private val clientSecret: String,
) {
    private var azureAdTokenMap: HashMap<String, AzureAdResponse> = HashMap()

    suspend fun accessToken(scope: String): String {
        val omToMinutter = Instant.now().plusSeconds(ONE_HUNDRED_AND_TWENTY_SECONDS)
        val azureAdResponse = azureAdTokenMap[scope]

        if (azureAdResponse == null ||
            azureAdResponse.issuedOn!!
                .plusSeconds(azureAdResponse.expires_in)
                .isBefore(omToMinutter)
        ) {
            LOG.info("Henter nytt token fra Azure AD for scope $scope")

            return try {
                val response: HttpResponse =
                    httpClientProxy().post(baseUrl) {
                        accept(ContentType.Application.Json)
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    append("client_id", clientId)
                                    append("client_secret", clientSecret)
                                    append("grant_type", "client_credentials")
                                    append("scope", scope)
                                },
                            ),
                        )
                    }
                azureAdTokenMap[scope] = response.body()
                azureAdTokenMap[scope]!!.access_token
            } catch (e: FetchAzureAdTokenException) {
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
    var issuedOn: Instant? = Instant.now(),
)

class FetchAzureAdTokenException(
    message: String,
) : Exception(message)
