package com.parv.tasteindia.testutil

import com.parv.tasteindia.data.remote.MealApi
import com.parv.tasteindia.data.remote.dto.MealDetailResponseDto
import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import kotlinx.coroutines.delay

/**
 * In-memory [MealApi] driven by fixtures. Counts calls so tests can assert caching /
 * de-duplication. Any endpoint can be made to fail by setting the matching `*Error`.
 */
class FakeMealApi(
    private val areaResponse: MealListResponseDto = MealListResponseDto(),
    private val categoryResponses: Map<String, MealListResponseDto> = emptyMap(),
    private val ingredientResponses: Map<String, MealListResponseDto> = emptyMap(),
    private val lookupResponses: Map<String, MealDetailResponseDto> = emptyMap(),
    private val searchResponses: Map<String, MealListResponseDto> = emptyMap(),
) : MealApi {

    var areaError: Throwable? = null
    var lookupError: Throwable? = null

    /** Artificial latency (virtual-time ms) per category / ingredient value, for latest-wins tests. */
    val categoryDelayMs = mutableMapOf<String, Long>()
    val ingredientDelayMs = mutableMapOf<String, Long>()

    var filterByAreaCalls = 0
        private set
    var lookupCallsById = mutableMapOf<String, Int>()
        private set

    override suspend fun filterByArea(area: String): MealListResponseDto {
        filterByAreaCalls++
        areaError?.let { throw it }
        return areaResponse
    }

    override suspend fun filterByCategory(category: String): MealListResponseDto {
        categoryDelayMs[category]?.let { delay(it) }
        return categoryResponses[category] ?: MealListResponseDto(meals = null)
    }

    override suspend fun filterByIngredient(ingredient: String): MealListResponseDto {
        ingredientDelayMs[ingredient]?.let { delay(it) }
        return ingredientResponses[ingredient] ?: MealListResponseDto(meals = null)
    }

    override suspend fun lookupById(id: String): MealDetailResponseDto {
        lookupCallsById[id] = (lookupCallsById[id] ?: 0) + 1
        lookupError?.let { throw it }
        return lookupResponses[id] ?: MealDetailResponseDto(meals = null)
    }

    override suspend fun searchByName(query: String): MealListResponseDto =
        searchResponses[query] ?: MealListResponseDto(meals = null)
}
