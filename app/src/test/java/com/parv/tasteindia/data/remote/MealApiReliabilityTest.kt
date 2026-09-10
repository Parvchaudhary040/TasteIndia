package com.parv.tasteindia.data.remote

import com.parv.tasteindia.data.repository.MealRepositoryImpl
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.testutil.FakeCachedMealDetailDao
import com.parv.tasteindia.testutil.Fixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * End-to-end reliability: a real OkHttp + Retrofit + kotlinx-serialization stack against a
 * MockWebServer, asserting every failure mode maps to the right [AppError] and that an empty
 * payload stays a *success*.
 */
class MealApiReliabilityTest {

    @get:Rule
    val serverRule = MockWebServerRule()

    private val server: MockWebServer get() = serverRule.server

    private var online = true

    private fun repository(): MealRepositoryImpl {
        val client = OkHttpClient.Builder()
            .callTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(ConnectivityInterceptor { online })
            .build()
        val retrofit = NetworkModule.retrofit(client, NetworkModule.json(), server.url("/").toString())
        return MealRepositoryImpl(
            api = NetworkModule.mealApi(retrofit),
            cacheDao = FakeCachedMealDetailDao(),
            json = NetworkModule.json(),
            ioDispatcher = Dispatchers.IO,
            scope = CoroutineScope(Dispatchers.IO),
        )
    }

    private fun enqueue(code: Int = 200, body: String, delaySeconds: Long = 0) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .body(body)
                .apply { if (delaySeconds > 0) bodyDelay(delaySeconds, TimeUnit.SECONDS) }
                .build(),
        )
    }

    @Test
    fun `empty payload is a successful empty list, not an error`() = runBlocking {
        enqueue(body = Fixtures.read("filter_empty.json"))

        val result = repository().getIndianMeals()

        assertEquals(emptyList<Any>(), (result as DataResult.Success).data)
    }

    @Test
    fun `a well-formed payload maps to meals`() = runBlocking {
        enqueue(body = Fixtures.read("filter_indian.json"))

        val result = repository().getIndianMeals()

        assertEquals(6, (result as DataResult.Success).data.size)
    }

    @Test
    fun `http 500 maps to Http(500)`() = runBlocking {
        enqueue(code = 500, body = "{}")

        val result = repository().getIndianMeals()

        assertEquals(AppError.Http(500), (result as DataResult.Failure).error)
    }

    @Test
    fun `malformed json maps to Serialization`() = runBlocking {
        enqueue(body = Fixtures.read("filter_malformed.json"))

        val result = repository().getIndianMeals()

        assertEquals(AppError.Serialization, (result as DataResult.Failure).error)
    }

    @Test
    fun `a response slower than the call timeout maps to Timeout`() = runBlocking {
        enqueue(body = Fixtures.read("filter_indian.json"), delaySeconds = 5)

        val result = repository().getIndianMeals()

        assertEquals(AppError.Timeout, (result as DataResult.Failure).error)
    }

    @Test
    fun `no connectivity fails fast as Network without hitting the wire`() = runBlocking {
        online = false

        val result = repository().getIndianMeals()

        assertEquals(AppError.Network, (result as DataResult.Failure).error)
        assertTrue("request should not have reached the server", server.requestCount == 0)
    }
}
