package id.neotica

/**
 * Runtime configuration, sourced purely from environment (`.env`).
 * Never hardcode secrets or hostnames in application code.
 *
 * The project boots with NO env configuration: `AUTH_ENABLED` defaults to
 * `false`, and only when it is `true` are `JWT_SECRET` / `BASE_URL` required.
 *
 * Values are re-read on each access so tests (and redeploys) can flip the
 * flag without a JVM restart.
 */
object AppConfig {
    val authEnabled: Boolean
        get() = EnvLoader["AUTH_ENABLED"]?.toBoolean() ?: false

    val jwtSecret: String
        get() = requireNotNull(EnvLoader["JWT_SECRET"]) {
            "JWT_SECRET is not set. Enable AUTH_ENABLED in .env and fill it in."
        }

    val baseUrl: String
        get() = requireNotNull(EnvLoader["BASE_URL"]) {
            "BASE_URL is not set. Enable AUTH_ENABLED in .env and fill it in."
        }
}