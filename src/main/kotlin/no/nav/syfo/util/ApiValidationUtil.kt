package no.nav.syfo.util

fun validateFnr(fnr: String): Boolean = fnr.isNotEmpty() && fnr.matches("\\d{11}$".toRegex())
