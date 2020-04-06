package no.nav.syfo.util

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import net.logstash.logback.argument.StructuredArguments

const val MDC_CALL_ID = "callId"
const val NAV_CALL_ID = "Nav-Call-Id"
const val NAV_CALL_ID_HEADER = "X-Nav-CallId"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID_HEADER].toString()
}

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!
