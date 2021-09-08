package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "syfobrukertilgang"

const val CALL_NARMESTELEDER_SUCCESS = "call_narmesteleder_success_count"
const val CALL_NARMESTELEDER_FAIL = "call_narmesteleder_fail_count"
val COUNT_CALL_NARMESTELEDER_SUCCESS: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(CALL_NARMESTELEDER_SUCCESS)
    .help("Counts the number of successful calls to narmesteleder")
    .register()
val COUNT_CALL_NARMESTELEDER_FAIL: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(CALL_NARMESTELEDER_FAIL)
    .help("Counts the number of failed calls to narmesteleder")
    .register()
