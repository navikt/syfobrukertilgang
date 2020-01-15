package no.nav.syfo.testutil

object UserConstants {

    private const val MOCK_AKTORID_PREFIX = "10"

    const val ARBEIDSTAKER_FNR = "12345678912"
    val ARBEIDSTAKER_AKTORID = mockAktorId(ARBEIDSTAKER_FNR)

    fun mockAktorId(fnr: String): String {
        return MOCK_AKTORID_PREFIX + fnr
    }
}
