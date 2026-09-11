package com.parv.tasteindia.domain.repository

import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.MealDetail

/**
 * Owns the Indian meal collection and all reads against TheMealDB.
 *
 * The Indian base set (`filter.php?a=Indian`) is the authoritative universe of meal IDs.
 * Category and ingredient queries return meals from every cuisine, so this interface only
 * ever hands back IDs that have already been intersected with the Indian base set — callers
 * cannot escape the Indian boundary because there is no API surface that lets them.
 */
interface MealRepository {

    /**
     * The authoritative Indian base set. Cached in memory after the first successful load;
     * pass [forceRefresh] to bypass the cache (e.g. pull-to-refresh / retry).
     */
    suspend fun getIndianMeals(forceRefresh: Boolean = false): DataResult<List<Meal>>

    /**
     * IDs of Indian meals in [category], i.e. `filter.php?c={category}` intersected with the
     * Indian base set. Requires the base set to be loadable (network on first call).
     */
    suspend fun getIndianMealIdsForCategory(category: String): DataResult<Set<String>>

    /**
     * IDs of Indian meals whose main ingredient is [ingredient], i.e. `filter.php?i={ingredient}`
     * intersected with the Indian base set.
     */
    suspend fun getIndianMealIdsForIngredient(ingredient: String): DataResult<Set<String>>

    /**
     * Full detail for one meal (`lookup.php?i={id}`). Served from an in-memory + Room cache
     * when available; concurrent calls for the same id are de-duplicated.
     */
    suspend fun getMealDetail(id: String): DataResult<MealDetail>

    /**
     * [id]'s detail if this app has already fetched it (in-memory or Room cache), with no
     * network fallback. A cache hit here is never proof of Indian membership by itself — every
     * detail was cached by a prior [getMealDetail] call whose id came from an already
     * Indian-gated source (the base set, or a category/ingredient intersection). Callers that
     * need to resolve an id of unknown provenance (e.g. a saved favourite that has fallen out of
     * the current base set) must use this instead of [getMealDetail], which would otherwise treat
     * a fresh, unrestricted `lookup.php` response as if it proved the id belongs in TasteIndia.
     */
    suspend fun getCachedMealDetail(id: String): MealDetail?
}
