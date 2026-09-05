package id.neotica

/**
 * Runtime configuration, sourced purely from environment (`.env`).
 * Never hardcode secrets or hostnames in application code.
 */
object AppConfig {
    val jwtSecret: String = requireNotNull(EnvLoader["JWT_SECRET"]) {
        "JWT_SECRET is not set. Copy .env.example to .env and fill it in."
    }

    val baseUrl: String = requireNotNull(EnvLoader["BASE_URL"]) {
        "BASE_URL is not set. Copy .env.example to .env and fill it in."
    }
}