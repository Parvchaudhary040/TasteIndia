package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.local.CachedMealDetailDao
import com.parv.tasteindia.data.local.CachedMealDetailEntity
import com.parv.tasteindia.data.remote.MealApi
import com.parv.tasteindia.data.remote.dto.MealDetailDto
import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import com.parv.tasteindia.data.remote.toAppError
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.MealDetail
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Single source of truth for TheMealDB reads.
 *
 * Indian boundary
 * ---------------
 * `filter.php?a=Indian` is the authoritative universe of meal IDs, loaded once and held in
 * memory. Category and ingredient queries return meals from every cuisine, so this class
 * intersects their IDs with the Indian base set before returning. There is no method that
 * hands back un-intersected IDs, so no caller can escape the boundary.
 *
 * Detail fetching (N+1 protection)
 * --------------------------------
 * The list never triggers per-row detail calls (rows only need image + name + favourite), so
 * there is no N+1 to begin with. On top of that, [getMealDetail] is:
 *   - served from an in-memory map, then a Room row, before any network call;
 *   - de-duplicated: concurrent calls for the same id await one shared request;
 *   - bounded: at most [MAX_CONCURRENT_DETAIL_CALLS] detail requests run at once.
 */
class MealRepositoryImpl(
    private val api: MealApi,
    private val cacheDao: CachedMealDetailDao,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : MealRepository {

    private companion object {
        // TheMealDB changed its data: meals that used to carry strArea="Indian" are now tagged
        // strArea="India" (the country name), so filter.php?a=Indian returns {"meals":null} while
        // filter.php?a=India returns the collection. The area *list* endpoint still advertises
        // "Indian", but no meal matches it. Verified against the live API on 2026-09-10.
        const val INDIAN_AREA = "India"
        const val MAX_CONCURRENT_DETAIL_CALLS = 4
    }

    private val indianMealsMutex = Mutex()

    @Volatile
    private var indianMealsCache: List<Meal>? = null

    private val detailMemoryCache = ConcurrentHashMap<String, MealDetail>()
    private val inFlightDetails = ConcurrentHashMap<String, Deferred<DataResult<MealDetail>>>()
    private val detailPermits = Semaphore(MAX_CONCURRENT_DETAIL_CALLS)

    // Resolved (already Indian-intersected) id sets, keyed by the filter value. Re-selecting a
    // filter after "Clear all" is then free, and toggling filters doesn't refetch.
    private val categoryIdsCache = ConcurrentHashMap<String, Set<String>>()
    private val ingredientIdsCache = ConcurrentHashMap<String, Set<String>>()

    override suspend fun getIndianMeals(forceRefresh: Boolean): DataResult<List<Meal>> {
        if (!forceRefresh) indianMealsCache?.let { return DataResult.Success(it) }

        return indianMealsMutex.withLock {
            // Re-check inside the lock: another caller may have populated it while we waited.
            if (!forceRefresh) indianMealsCache?.let { return@withLock DataResult.Success(it) }

            when (val response = safeApiCall { api.filterByArea(INDIAN_AREA) }) {
                is DataResult.Failure -> response
                is DataResult.Success -> {
                    // Partial-data guard: drop rows the UI couldn't render (no id or no name).
                    val meals = response.data.meals.orEmpty()
                        .map { it.toDomain() }
                        .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                    indianMealsCache = meals
                    DataResult.Success(meals)
                }
            }
        }
    }

    override suspend fun getIndianMealIdsForCategory(category: String): DataResult<Set<String>> {
        categoryIdsCache[category]?.let { return DataResult.Success(it) }
        return intersectWithIndianBase { api.filterByCategory(category) }
            .also { if (it is DataResult.Success) categoryIdsCache[category] = it.data }
    }

    override suspend fun getIndianMealIdsForIngredient(ingredient: String): DataResult<Set<String>> {
        ingredientIdsCache[ingredient]?.let { return DataResult.Success(it) }
        return intersectWithIndianBase { api.filterByIngredient(ingredient) }
            .also { if (it is DataResult.Success) ingredientIdsCache[ingredient] = it.data }
    }

    /** Runs [query], then keeps only the IDs that are also in the Indian base set. */
    private suspend fun intersectWithIndianBase(
        query: suspend () -> MealListResponseDto,
    ): DataResult<Set<String>> {
        val baseIds = when (val base = getIndianMeals()) {
            is DataResult.Failure -> return base
            is DataResult.Success -> base.data.mapTo(HashSet()) { it.id }
        }

        return when (val response = safeApiCall(query)) {
            is DataResult.Failure -> response
            is DataResult.Success -> {
                val queried = response.data.meals.orEmpty().mapTo(HashSet()) { it.idMeal }
                DataResult.Success(queried.apply { retainAll(baseIds) })
            }
        }
    }

    override suspend fun getMealDetail(id: String): DataResult<MealDetail> {
        detailMemoryCache[id]?.let { return DataResult.Success(it) }

        readDetailFromRoom(id)?.let { cached ->
            detailMemoryCache[id] = cached
            return DataResult.Success(cached)
        }

        // De-duplicate concurrent callers onto one request; bound total concurrency. The
        // request runs in the repository scope, so a cancelled caller doesn't waste the work
        // or leave a stale in-flight entry (cleared on completion, not on await).
        val request = inFlightDetails.computeIfAbsent(id) {
            scope.async { detailPermits.withPermit { fetchAndCacheDetail(id) } }
                .also { deferred -> deferred.invokeOnCompletion { inFlightDetails.remove(id, deferred) } }
        }
        return request.await()
    }

    private suspend fun fetchAndCacheDetail(id: String): DataResult<MealDetail> =
        when (val response = safeApiCall { api.lookupById(id) }) {
            is DataResult.Failure -> response
            is DataResult.Success -> {
                val dto = response.data.meals?.firstOrNull()
                    ?: return DataResult.Failure(AppError.NotFound)
                val detail = dto.toDomain()
                detailMemoryCache[id] = detail
                persistDetail(id, dto)
                DataResult.Success(detail)
            }
        }

    private suspend fun persistDetail(id: String, dto: MealDetailDto) {
        runCatching {
            cacheDao.upsert(
                CachedMealDetailEntity(
                    idMeal = id,
                    payloadJson = json.encodeToString(dto),
                    cachedAt = nowMillis(),
                )
            )
        }
    }

    private suspend fun readDetailFromRoom(id: String): MealDetail? =
        runCatching {
            cacheDao.getById(id)?.let { entity ->
                json.decodeFromString<MealDetailDto>(entity.payloadJson).toDomain()
            }
        }.getOrNull()

    private suspend fun <T> safeApiCall(block: suspend () -> T): DataResult<T> =
        try {
            DataResult.Success(withContext(ioDispatcher) { block() })
        } catch (t: Throwable) {
            DataResult.Failure(t.toAppError()) // rethrows CancellationException
        }
}
