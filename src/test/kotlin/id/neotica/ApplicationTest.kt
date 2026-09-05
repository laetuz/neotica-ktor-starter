package id.neotica

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ApplicationTest {

    init {
        System.setProperty("JWT_SECRET", "test-secret-that-is-long-enough-for-hmac-256")
        System.setProperty("BASE_URL", "http://localhost:8080")
    }

    private fun testServer(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { module() }
        block()
    }

    private suspend fun io.ktor.client.HttpClient.login(user: String): String {
        val resp = post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$user","password":"x"}""")
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        return json["token"]?.jsonPrimitive?.content
            ?: error("login did not return a token")
    }

    @Test
    fun `health check returns ok`() = testServer {
        val resp = client.get("/healthz")
        assertEquals(HttpStatusCode.OK, resp.status)
    }

    @Test
    fun `tasks require authentication`() = testServer {
        val resp = client.get("/tasks")
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun `login returns a token then tasks work`() = testServer {
        val token = client.login("bob")
        val tasks = client.get("/tasks") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, tasks.status)
    }

    @Test
    fun `creating a blank title is rejected`() = testServer {
        val token = client.login("bob")
        val resp = client.post("/tasks") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"  ","description":null}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun `delete requires admin role`() = testServer {
        val userToken = client.login("bob")
        val resp = client.delete("/tasks/some-id") {
            bearerAuth(userToken)
        }
        assertEquals(HttpStatusCode.Forbidden, resp.status)
    }

    @Test
    fun `admin can delete a task`() = testServer {
        val adminToken = client.login("admin")
        val resp = client.delete("/tasks/does-not-exist") {
            bearerAuth(adminToken)
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }
}