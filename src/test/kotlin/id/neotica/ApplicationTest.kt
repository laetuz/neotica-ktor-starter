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
 * Default-mode tests: AUTH_ENABLED=false, app boots with no env config.
 *
 * The AUTH_* properties are set immediately before the application boots so the
 * two test classes never leak shared JVM property state into each other.
 */
class ApplicationTest {

    private fun testServer(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        System.clearProperty("AUTH_ENABLED")
        System.clearProperty("JWT_SECRET")
        System.clearProperty("BASE_URL")
        application { module() }
        block()
    }

    @Test
    fun `hello with no name returns World`() = testServer {
        val resp = client.get("/hello")
        val message = messageOf(resp)
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("Hello, World!", message)
    }

    @Test
    fun `hello uses the name query param`() = testServer {
        val resp = client.get("/hello?name=Ktor")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("Hello, Ktor!", messageOf(resp))
    }

    @Test
    fun `hello treats blank name as World`() = testServer {
        val resp = client.get("/hello?name=%20%20")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("Hello, World!", messageOf(resp))
    }

    @Test
    fun `unknown route returns structured 404`() = testServer {
        val resp = client.get("/nope")
        assertEquals(HttpStatusCode.NotFound, resp.status)
        assertTrue(resp.bodyAsText().contains("not_found"))
    }

    @Test
    fun `login is not mounted when auth is disabled`() = testServer {
        val resp = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"bob","password":"x"}""")
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test
    fun `notes start empty`() = testServer {
        val resp = client.get("/notes")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("[]", resp.bodyAsText().trim())
    }

    @Test
    fun `note lifecycle - create list update delete`() = testServer {
        val created = client.post("/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"text":"first note"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val noteJson = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val id = noteJson["id"]?.jsonPrimitive?.content ?: error("missing id")
        assertEquals("first note", noteJson["text"]?.jsonPrimitive?.content)

        val listed = client.get("/notes")
        assertEquals(HttpStatusCode.OK, listed.status)
        assertTrue(listed.bodyAsText().contains("first note"))

        val updated = client.put("/notes/$id") {
            contentType(ContentType.Application.Json)
            setBody("""{"text":"updated note"}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updated.bodyAsText().contains("updated note"))

        val deleted = client.delete("/notes/$id")
        assertEquals(HttpStatusCode.NoContent, deleted.status)

        val afterDelete = client.get("/notes")
        assertEquals(HttpStatusCode.OK, afterDelete.status)
        assertFalse(afterDelete.bodyAsText().contains("updated note"))
    }

    @Test
    fun `creating a blank note is rejected`() = testServer {
        val resp = client.post("/notes") {
            contentType(ContentType.Application.Json)
            setBody("""{"text":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
        assertTrue(resp.bodyAsText().contains("invalid_request"))
    }

    @Test
    fun `updating a missing note returns 404`() = testServer {
        val resp = client.put("/notes/missing-id") {
            contentType(ContentType.Application.Json)
            setBody("""{"text":"nope"}""")
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test
    fun `deleting a missing note returns 404`() = testServer {
        val resp = client.delete("/notes/missing-id")
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    private suspend fun messageOf(resp: HttpResponse): String {
        val element = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
        return element["message"]?.jsonPrimitive?.content ?: error("missing message")
    }
}