package id.neotica.service

/**
 * Thin business layer in front of the route. Keeps the route class simple and
 * introduces the route → service split without a database.
 */
class GreetingService {

    fun greet(name: String?): String {
        val subject = name?.trim()?.takeIf { it.isNotBlank() } ?: "World"
        return "Hello, $subject!"
    }
}