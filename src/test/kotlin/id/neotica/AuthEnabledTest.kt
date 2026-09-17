package id.neotica

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

/**
 * Auth-enabled mode tests: AUTH_ENABLED=true activates POST /auth/login.
 *
 * The AUTH_* properties are set immediately before the application boots so the
 * two test classes never leak shared JVM property state into each other.
 */
class AuthEnabledTest {

    private fun testServer(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        System.setProperty("AUTH_ENABLED", "true")
        System.setProperty("JWT_SECRET", "test-secret-that-is-long-enough-for-hmac-256")
        System.setProperty("BASE_URL", "http://localhost:8080")
        application { module() }
        block()
    }

    @Test
    fun `login is mounted when auth is enabled`() = testServer {
        val resp = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"bob","password":"x"}""")
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val token = Json.parseToJsonElement(resp.bodyAsText())
            .jsonObject["token"]?.jsonPrimitive?.content
        assertFalse(token.isNullOrBlank())
    }

    @Test
    fun `hello still works when auth is enabled`() = testServer {
        val resp = client.get("/hello?name=World")
        assertEquals(HttpStatusCode.OK, resp.status)
    }
}