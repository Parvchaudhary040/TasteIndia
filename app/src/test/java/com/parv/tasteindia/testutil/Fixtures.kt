package com.parv.tasteindia.testutil

import kotlinx.serialization.json.Json

/**
 * Loads the JSON fixtures under `src/test/resources/fixtures/`. These mirror real TheMealDB
 * responses so tests never touch the public API.
 */
object Fixtures {

    /** Same leniency as the production [com.parv.tasteindia.data.remote.NetworkModule.json]. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun read(name: String): String {
        val stream = Fixtures::class.java.classLoader!!.getResourceAsStream("fixtures/$name")
            ?: error("Fixture not found: fixtures/$name")
        return stream.bufferedReader().use { it.readText() }
    }

    inline fun <reified T> decode(name: String): T = json.decodeFromString(read(name))
}
