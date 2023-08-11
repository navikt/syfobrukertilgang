package no.nav.syfo.testutil

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import no.nav.syfo.testutil.UserConstants.LEDER_FNR
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun generateTokenXToken(
    audience: String,
    issuer: String,
    expiry: LocalDateTime? = LocalDateTime.now().plusHours(1),
    fnr: String = LEDER_FNR,
): String? {
    val now = Date()
    val key = getDefaultRSAKey()
    val alg = Algorithm.RSA256(key.toRSAPublicKey(), key.toRSAPrivateKey())

    return JWT.create()
        .withKeyId(key.keyID)
        .withIssuer(issuer)
        .withAudience(audience)
        .withJWTId(UUID.randomUUID().toString())
        .withClaim("pid", fnr)
        .withClaim("ver", "1.0")
        .withClaim("nonce", "myNonce")
        .withClaim("auth_time", now)
        .withClaim("nbf", now)
        .withClaim("iat", now)
        .withClaim("exp", Date.from(expiry?.atZone(ZoneId.systemDefault())?.toInstant()))
        .withClaim("acr", "idporten-loa-high")
        .sign(alg)
}

private fun getDefaultRSAKey(): RSAKey {
    return getJWKSet().keys.first() as RSAKey
}

private fun getJWKSet(): JWKSet {
    return JWKSet.parse(getFileAsString("src/test/resources/jwkset.json"))
}

fun getFileAsString(filePath: String) = String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8)
