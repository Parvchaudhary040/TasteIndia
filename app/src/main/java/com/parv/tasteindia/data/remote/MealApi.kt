package com.parv.tasteindia.data.remote

import com.parv.tasteindia.data.remote.dto.MealDetailResponseDto
import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * TheMealDB v1 public API. Only the four required endpoints plus optional name search.
 *
 * `@Query` handles URL encoding, so ingredient/category/query values are safe to pass raw.
 * These are plain `suspend` functions returning the parsed body; HTTP status and IO failures
 * surface as exceptions that the repository translates into [com.parv.tasteindia.domain.model.AppError].
 */
interface MealApi {

    /** filter.php?a=Indian — the authoritative Indian base set (id, name, thumb only). */
    @GET("filter.php")
    suspend fun filterByArea(@Query("a") area: String): MealListResponseDto

    /** filter.php?c={category} — meals in a category, ALL cuisines (must be intersected). */
    @GET("filter.php")
    suspend fun filterByCategory(@Query("c") category: String): MealListResponseDto

    /** filter.php?i={ingredient} — meals by main ingredient, ALL cuisines (must be intersected). */
    @GET("filter.php")
    suspend fun filterByIngredient(@Query("i") ingredient: String): MealListResponseDto

    /** lookup.php?i={id} — full detail for a single meal. */
    @GET("lookup.php")
    suspend fun lookupById(@Query("i") id: String): MealDetailResponseDto

    /** search.php?s={query} — optional name search, ALL cuisines (must be intersected). */
    @GET("search.php")
    suspend fun searchByName(@Query("s") query: String): MealListResponseDto
}
