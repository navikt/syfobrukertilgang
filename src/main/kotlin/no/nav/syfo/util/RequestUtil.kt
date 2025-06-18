package no.nav.syfo.util

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.RoutingContext
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
fun RoutingContext.getConsumerId(): String {
    return this.call.request.headers[NAV_CONSUMER_ID_HEADER].toString()
}
fun consumerIdArgument(consumerId: String) = StructuredArguments.keyValue("consumerId", consumerId)!!

const val NAV_PERSONIDENT_HEADER = "Nav-Personident"

fun RoutingContext.getPersonIdent(): String? {
    return this.call.request.header(NAV_PERSONIDENT_HEADER)
}

