package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

const val LOCAL_ENV_PROPERTIES_PATH = "./src/main/resources/localEnv.json"
const val DEFAULT_LOCAL_ENV_PROPERTIES_PATH = "./src/main/resources/localEnvForTests.json"
private val objectMapper: ObjectMapper = ObjectMapper()

fun getEnvironment(): Environment {
    objectMapper.registerKotlinModule()
    return if (appIsRunningLocally) {
        objectMapper.readValue(
            firstExistingFile(LOCAL_ENV_PROPERTIES_PATH, DEFAULT_LOCAL_ENV_PROPERTIES_PATH),
            Environment::class.java
        )
    } else {
        Environment(
            getEnvVar("APPLICATION_PORT", "8080").toInt(),
            getEnvVar("APPLICATION_THREADS", "1").toInt(),
            getEnvVar("APPLICATION_NAME", "syfobrukertilgang"),
            getEnvVar("NARMESTELEDER_SCOPE"),
            getEnvVar("NARMESTELEDER_URL"),
            getEnvVar("AZURE_APP_CLIENT_ID"),
            getEnvVar("AZURE_APP_CLIENT_SECRET"),
            getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
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
    val narmestelederScope: String,
    val narmestelederUrl: String,
    val aadClientId: String,
    val aadClientSecret: String,
    val aadTokenEndpoint: String,
    val syfobrukertilgangTokenXClientId: String,
    val tokenXWellKnownUrl: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw MissingVariableException(varName)

private fun firstExistingFile(vararg paths: String) = paths
    .map(::File)
    .first(File::exists)

class MissingVariableException(varName: String) : RuntimeException("Missing required variable \"$varName\"")
