package id.neotica.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import id.neotica.domain.model.Role
import id.neotica.domain.model.TokenData
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class TokenService(
    private val secret: String,
    private val issuer: String,
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .build()

    fun issue(userId: String, role: Role): String =
        JWT.create()
            .withSubject(userId)
            .withIssuer(issuer)
            .withClaim("role", role.name)
            .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .sign(algorithm)

    fun decode(token: String): TokenData {
        val decoded = verifier.verify(token)
        val userId = decoded.subject ?: throw RuntimeException("Missing subject")
        val role = Role.valueOf(decoded.getClaim("role").asString() ?: Role.USER.name)
        return TokenData(userId = userId, role = role)
    }
}