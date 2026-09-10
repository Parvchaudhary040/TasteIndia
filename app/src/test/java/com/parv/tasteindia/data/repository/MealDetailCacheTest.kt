package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.local.CachedMealDetailEntity
import com.parv.tasteindia.data.remote.dto.MealDetailDto
import com.parv.tasteindia.data.remote.dto.MealDetailResponseDto
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.testutil.FakeCachedMealDetailDao
import com.parv.tasteindia.testutil.FakeMealApi
import com.parv.tasteindia.testutil.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down the N+1 protection in [MealRepositoryImpl.getMealDetail]: memory cache, Room cache,
 * and de-duplication of concurrent requests for the same id.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailCacheTest {

    private val normalLookup = Fixtures.decode<MealDetailResponseDto>("lookup_normal.json")
    private val normalDto: MealDetailDto = normalLookup.meals!!.single()
    private val mealId = normalDto.idMeal

    /** Build a repository bound to this test's virtual-time scheduler. */
    private fun TestScope.repo(
        api: FakeMealApi,
        dao: FakeCachedMealDetailDao = FakeCachedMealDetailDao(),
    ): Pair<MealRepositoryImpl, FakeCachedMealDetailDao> =
        MealRepositoryImpl(
            api = api,
            cacheDao = dao,
            json = Fixtures.json,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = backgroundScope,
            nowMillis = { 0L },
        ) to dao

    @Test
    fun `second lookup is served from memory without another network call`() = runTest {
        val api = FakeMealApi(lookupResponses = mapOf(mealId to normalLookup))
        val (repository, _) = repo(api)

        val first = repository.getMealDetail(mealId)
        val second = repository.getMealDetail(mealId)

        assertTrue(first is DataResult.Success)
        assertEquals((first as DataResult.Success).data, (second as DataResult.Success).data)
        assertEquals(1, api.lookupCallsById[mealId])
    }

    @Test
    fun `a lookup writes through to the Room cache`() = runTest {
        val api = FakeMealApi(lookupResponses = mapOf(mealId to normalLookup))
        val (repository, dao) = repo(api)

        repository.getMealDetail(mealId)

        assertTrue(dao.store.containsKey(mealId))
    }

    @Test
    fun `a pre-populated Room cache is used with zero network calls`() = runTest {
        val dao = FakeCachedMealDetailDao().apply {
            store[mealId] = CachedMealDetailEntity(
                idMeal = mealId,
                payloadJson = Fixtures.json.encodeToString(normalDto),
                cachedAt = 0L,
            )
        }
        val api = FakeMealApi(lookupResponses = mapOf(mealId to normalLookup))
        val (repository, _) = repo(api, dao)

        val result = repository.getMealDetail(mealId)

        assertTrue(result is DataResult.Success)
        assertEquals("Chicken Handi", (result as DataResult.Success).data.name)
        assertEquals(null, api.lookupCallsById[mealId])
    }

    @Test
    fun `concurrent lookups for the same id share one network request`() = runTest {
        val api = FakeMealApi(lookupResponses = mapOf(mealId to normalLookup))
        val (repository, _) = repo(api)

        val a = async { repository.getMealDetail(mealId) }
        val b = async { repository.getMealDetail(mealId) }
        val c = async { repository.getMealDetail(mealId) }

        assertTrue(a.await() is DataResult.Success)
        assertEquals(a.await(), b.await())
        assertEquals(a.await(), c.await())
        assertEquals(1, api.lookupCallsById[mealId])
    }

    @Test
    fun `a lookup that returns no meal is NotFound`() = runTest {
        val api = FakeMealApi(lookupResponses = mapOf(mealId to MealDetailResponseDto(meals = null)))
        val (repository, _) = repo(api)

        val result = repository.getMealDetail(mealId)

        assertEquals(AppError.NotFound, (result as DataResult.Failure).error)
    }
}
