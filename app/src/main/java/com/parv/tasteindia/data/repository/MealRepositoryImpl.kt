package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.local.CachedMealDetailDao
import com.parv.tasteindia.data.remote.MealApi
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.MealDetail
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

/**
 * Phase 2: dependencies are wired, behaviour lands in Phase 3 (error mapping, the Indian base
 * set, category/ingredient intersection, detail caching + de-duplication + bounded concurrency).
 */
class MealRepositoryImpl(
    private val api: MealApi,
    private val cacheDao: CachedMealDetailDao,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MealRepository {

    override suspend fun getIndianMeals(forceRefresh: Boolean): DataResult<List<Meal>> =
        TODO("Phase 3: load filter.php?a=Indian, cache in memory")

    override suspend fun getIndianMealIdsForCategory(category: String): DataResult<Set<String>> =
        TODO("Phase 3: filter.php?c={category} intersected with the Indian base set")

    override suspend fun getIndianMealIdsForIngredient(ingredient: String): DataResult<Set<String>> =
        TODO("Phase 3: filter.php?i={ingredient} intersected with the Indian base set")

    override suspend fun getMealDetail(id: String): DataResult<MealDetail> =
        TODO("Phase 3: lookup.php?i={id} with in-memory + Room cache and request de-duplication")
}
