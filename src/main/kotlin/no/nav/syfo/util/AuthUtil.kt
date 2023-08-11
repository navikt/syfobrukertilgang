package no.nav.syfo.util

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import java.net.URL
import java.util.concurrent.TimeUnit

private const val JWK_CACHE_SIZE = 10L
private const val JWK_CACHE_EXPIRES_IN = 24L
private const val JWK_BUCKET_SIZE = 10L
private const val JWK_REFILL_RATE = 1L

fun jwkProvider(wellKnownUri: String): JwkProvider =
    JwkProviderBuilder(URL(wellKnownUri)).cached(
        JWK_CACHE_SIZE,
        JWK_CACHE_EXPIRES_IN,
        TimeUnit.HOURS
    )
        .rateLimited(JWK_BUCKET_SIZE, JWK_REFILL_RATE, TimeUnit.MINUTES).build()
