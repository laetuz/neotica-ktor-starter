package id.neotica

import java.io.File

/**
 * Minimal environment loader.
 * Reads a local `.env` file (key=value) and falls back to system environment
 * variables. Kept deliberately simple for the demo; swap for a library if
 * you need quoting/inline-comment support.
 */
object EnvLoader {
    private val loaded: Map<String, String> by lazy {
        buildMap {
            val envFile = File(".env")
            if (envFile.exists()) {
                envFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
                    .forEach { line ->
                        val key = line.substringBefore('=').trim()
                        val value = line.substringAfter('=').trim()
                        put(key, value)
                    }
            }
        }
    }

    operator fun get(key: String): String? =
        loaded[key] ?: System.getenv(key) ?: System.getProperty(key)
}