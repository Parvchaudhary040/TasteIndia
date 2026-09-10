package com.parv.tasteindia.presentation.recipes

import androidx.lifecycle.SavedStateHandle
import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import com.parv.tasteindia.data.remote.dto.MealSummaryDto
import com.parv.tasteindia.data.repository.MealRepositoryImpl
import com.parv.tasteindia.testutil.FakeCachedMealDetailDao
import com.parv.tasteindia.testutil.FakeFavouritesRepository
import com.parv.tasteindia.testutil.FakeMealApi
import com.parv.tasteindia.testutil.Fixtures
import com.parv.tasteindia.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Requirement #3: the latest search / filter selection always wins; a slower earlier one never
 * "arrives late" and overwrites it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LatestFilterStateWinsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun summary(id: String, name: String) =
        MealSummaryDto(idMeal = id, strMeal = name, strMealThumb = null)

    private val indianBase = MealListResponseDto(
        meals = listOf(
            summary("1", "Aloo Gobi"),
            summary("2", "Butter Chicken"),
            summary("3", "Chicken Handi"),
            summary("4", "Dal Makhani"),
            summary("5", "Prawn Curry"),
        ),
    )

    private fun viewModel(api: FakeMealApi, favourites: FakeFavouritesRepository = FakeFavouritesRepository()) =
        RecipesViewModel(
            handle = SavedStateHandle(),
            mealRepository = MealRepositoryImpl(
                api = api,
                cacheDao = FakeCachedMealDetailDao(),
                json = Fixtures.json,
                ioDispatcher = mainDispatcherRule.dispatcher,
                scope = CoroutineScope(mainDispatcherRule.dispatcher),
            ),
            favouritesRepository = favourites,
        )

    @Test
    fun `a slow earlier category never overrides a faster later one`() = runTest(mainDispatcherRule.dispatcher) {
        val api = FakeMealApi(
            areaResponse = indianBase,
            categoryResponses = mapOf(
                "Slow" to MealListResponseDto(meals = listOf(summary("1", "Aloo Gobi"), summary("2", "Butter Chicken"))),
                "Fast" to MealListResponseDto(meals = listOf(summary("4", "Dal Makhani"), summary("5", "Prawn Curry"))),
            ),
        ).apply { categoryDelayMs["Slow"] = 1_000 }

        val vm = viewModel(api)
        val seen = mutableListOf<List<String>>()
        val job = launch { vm.uiState.collect { seen += it.meals.map(RecipeListItem::id) } }
        advanceUntilIdle()

        vm.onCategorySelected("Slow")
        advanceTimeBy(100) // Slow is in flight (needs 1000ms), not applied yet
        vm.onCategorySelected("Fast")
        advanceUntilIdle()

        assertEquals(listOf("4", "5"), vm.uiState.value.meals.map { it.id })
        // The slow selection's result set must never have been shown.
        assertTrue("Slow result leaked into UI: $seen", seen.none { it == listOf("1", "2") })
        job.cancel()
    }

    @Test
    fun `only the final query in a fast burst is applied`() = runTest(mainDispatcherRule.dispatcher) {
        val api = FakeMealApi(areaResponse = indianBase)
        val vm = viewModel(api)

        val seen = mutableListOf<List<String>>()
        val job = launch { vm.uiState.collect { seen += it.meals.map(RecipeListItem::id) } }
        advanceUntilIdle()

        val fullList = listOf("1", "2", "3", "4", "5")
        vm.onSearchQueryChange("a")
        advanceTimeBy(50)
        vm.onSearchQueryChange("chi")
        advanceTimeBy(50)
        vm.onSearchQueryChange("chicken")
        advanceUntilIdle()

        assertEquals(listOf("2", "3"), vm.uiState.value.meals.map { it.id })
        // Every settled emission was either the unfiltered list or the final "chicken" result;
        // "a" / "chi" never got past the debounce.
        assertTrue(
            "Intermediate query leaked: $seen",
            seen.all { it.isEmpty() || it == fullList || it == listOf("2", "3") },
        )
        job.cancel()
    }

    @Test
    fun `favourites-only and category combine, and clearing all restores the full list`() =
        runTest(mainDispatcherRule.dispatcher) {
            val api = FakeMealApi(
                areaResponse = indianBase,
                categoryResponses = mapOf(
                    "Chicken" to MealListResponseDto(
                        meals = listOf(summary("2", "Butter Chicken"), summary("3", "Chicken Handi")),
                    ),
                ),
            )
            val favourites = FakeFavouritesRepository(setOf("3", "4"))
            val vm = viewModel(api, favourites)

            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            vm.onCategorySelected("Chicken")
            vm.onFavouritesOnlyChange(true)
            advanceUntilIdle()
            // category {2,3} AND favourites {3,4} -> {3}
            assertEquals(listOf("3"), vm.uiState.value.meals.map { it.id })

            vm.clearAllFilters()
            advanceUntilIdle()
            assertEquals(listOf("1", "2", "3", "4", "5"), vm.uiState.value.meals.map { it.id })
            job.cancel()
        }

    @Test
    fun `category results are intersected with the Indian base set`() =
        runTest(mainDispatcherRule.dispatcher) {
            val api = FakeMealApi(
                areaResponse = indianBase,
                categoryResponses = mapOf(
                    // "999" is a non-Indian meal returned by the category endpoint.
                    "Seafood" to MealListResponseDto(
                        meals = listOf(summary("5", "Prawn Curry"), summary("999", "Fish and Chips")),
                    ),
                ),
            )
            val vm = viewModel(api)
            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            vm.onCategorySelected("Seafood")
            advanceUntilIdle()

            assertEquals(listOf("5"), vm.uiState.value.meals.map { it.id })
            job.cancel()
        }
}
