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
                getEnvVar("AAD_B2C_DISCOVERY_URL"),
                getEnvVar("AAD_B2C_CLIENT_ID"),
                getEnvVar("AKTORREGISTER_V1_URL"),
                getEnvVar("SECURITY_TOKEN_SERVICE_REST_URL")
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
        val aktoerregisterV1Url: String,
        val stsRestUrl: String
)

data class VaultSecrets(
        val serviceuserUsername: String,
        val serviceuserPassword: String
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
        System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun firstExistingFile(vararg paths: String) = paths
        .map(::File)
        .first(File::exists)
