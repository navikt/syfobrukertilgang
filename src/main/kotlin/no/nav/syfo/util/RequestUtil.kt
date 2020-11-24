package no.nav.syfo.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import net.logstash.logback.argument.StructuredArguments
import java.util.*

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID_HEADER].toString()
}
fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

const val NAV_PERSONIDENTER = "Nav-Personidenter"

const val APP_CONSUMER_ID = "syfobrukertilgang"
const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
fun PipelineContext<out Unit, ApplicationCall>.getConsumerId(): String {
    return this.call.request.headers[NAV_CONSUMER_ID_HEADER].toString()
}
fun consumerIdArgument(consumerId: String) = StructuredArguments.keyValue("consumerId", consumerId)!!

fun basicHeader(
    credentialUsername: String,
    credentialPassword: String
) = "Basic " + Base64.getEncoder().encodeToString(java.lang.String.format("%s:%s", credentialUsername, credentialPassword).toByteArray())
