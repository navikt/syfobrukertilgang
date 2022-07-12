package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

const val localEnvironmentPropertiesPath = "./src/main/resources/localEnv.json"
const val defaultlocalEnvironmentPropertiesPath = "./src/main/resources/localEnvForTests.json"
private val objectMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    objectMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        objectMapper.readValue(firstExistingFile(localEnvironmentPropertiesPath, defaultlocalEnvironmentPropertiesPath), Environment::class.java)
    } else {
        Environment(
            getEnvVar("APPLICATION_PORT", "8080").toInt(),
            getEnvVar("APPLICATION_THREADS", "1").toInt(),
            getEnvVar("APPLICATION_NAME", "syfobrukertilgang"),
            getEnvVar("LOGINSERVICE_IDPORTEN_DISCOVERY_URL"),
            getEnvVar("LOGINSERVICE_IDPORTEN_AUDIENCE"),
            getEnvVar("NARMESTELEDER_SCOPE"),
            getEnvVar("NARMESTELEDER_URL"),
            getEnvVar("AZURE_APP_CLIENT_ID"),
            getEnvVar("AZURE_APP_CLIENT_SECRET"),
            getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            getEnvVar("SYFOMOTEBEHOV_CLIENT_ID"),
            getEnvVar("TOKEN_X_CLIENT_ID"),
            getEnvVar("TOKEN_X_WELL_KNOWN_URL")
        )
    }
}

val appIsRunningLocally: Boolean = System.getenv("NAIS_CLUSTER_NAME").isNullOrEmpty()

data class Environment(
    val applicationPort: Int,
    val applicationThreads: Int,
    val applicationName: String,
    val aadb2cDiscoveryUrl: String,
    val aadb2cClientId: String,
    val narmestelederScope: String,
    val narmestelederUrl: String,
    val aadClientId: String,
    val aadClientSecret: String,
    val aadTokenEndpoint: String,
    val syfomotebehovClientId: String,
    val syfobrukertilgangTokenXClientId: String,
    val tokenXWellKnownUrl: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
    .map(::File)
    .first(File::exists)
