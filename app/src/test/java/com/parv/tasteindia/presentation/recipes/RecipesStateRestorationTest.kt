package com.parv.tasteindia.presentation.recipes

import androidx.lifecycle.SavedStateHandle
import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import com.parv.tasteindia.data.remote.dto.MealSummaryDto
import com.parv.tasteindia.data.repository.MealRepositoryImpl
import com.parv.tasteindia.domain.model.SortOrder
import com.parv.tasteindia.testutil.FakeCachedMealDetailDao
import com.parv.tasteindia.testutil.FakeFavouritesRepository
import com.parv.tasteindia.testutil.FakeMealApi
import com.parv.tasteindia.testutil.Fixtures
import com.parv.tasteindia.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Test requirement #4 (the navigation-restoration half): the Recipes screen's search / filter /
 * sort selection lives in [SavedStateHandle], so it is rebuilt after process death or a
 * back-stack restore. Recreating the ViewModel with a populated handle must reproduce the exact
 * filtered + sorted list, with no user re-entry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipesStateRestorationTest {

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
        ),
    )

    private fun viewModel(handle: SavedStateHandle, favourites: FakeFavouritesRepository) =
        RecipesViewModel(
            handle = handle,
            mealRepository = MealRepositoryImpl(
                api = FakeMealApi(
                    areaResponse = indianBase,
                    categoryResponses = mapOf(
                        "Chicken" to MealListResponseDto(
                            meals = listOf(summary("2", "Butter Chicken"), summary("3", "Chicken Handi")),
                        ),
                    ),
                ),
                cacheDao = FakeCachedMealDetailDao(),
                json = Fixtures.json,
                ioDispatcher = mainDispatcherRule.dispatcher,
                scope = CoroutineScope(mainDispatcherRule.dispatcher),
            ),
            favouritesRepository = favourites,
        )

    @Test
    fun `a populated SavedStateHandle restores search, category, favourites-only and sort`() =
        runTest(mainDispatcherRule.dispatcher) {
            val handle = SavedStateHandle(
                mapOf(
                    "recipes.query" to "chicken",
                    "recipes.category" to "Chicken",
                    "recipes.favouritesOnly" to false,
                    "recipes.sort" to SortOrder.Z_A.name,
                ),
            )
            val vm = viewModel(handle, FakeFavouritesRepository())

            // The text field binds to this immediately (no debounce wait needed).
            assertEquals("chicken", vm.queryInput.value)

            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals("chicken", state.filters.query)
            assertEquals("Chicken", state.filters.category)
            assertEquals(SortOrder.Z_A, state.filters.sort)
            // query "chicken" ∩ category {2,3} ∩ base, sorted Z-A
            assertEquals(listOf("Chicken Handi", "Butter Chicken"), state.meals.map { it.name })
            assertEquals(2, state.activeFilterCount)
            job.cancel()
        }

    @Test
    fun `favourites-only survives restoration`() = runTest(mainDispatcherRule.dispatcher) {
        val handle = SavedStateHandle(mapOf("recipes.favouritesOnly" to true))
        val vm = viewModel(handle, FakeFavouritesRepository(setOf("4")))
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.filters.favouritesOnly)
        assertEquals(listOf("Dal Makhani"), vm.uiState.value.meals.map { it.name })
        job.cancel()
    }

    @Test
    fun `clearAllFilters writes defaults back into the handle`() =
        runTest(mainDispatcherRule.dispatcher) {
            val handle = SavedStateHandle(
                mapOf(
                    "recipes.query" to "chicken",
                    "recipes.category" to "Chicken",
                    "recipes.sort" to SortOrder.Z_A.name,
                ),
            )
            val vm = viewModel(handle, FakeFavouritesRepository())
            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            vm.clearAllFilters()
            advanceUntilIdle()

            assertEquals("", handle["recipes.query"])
            assertEquals(null, handle.get<String?>("recipes.category"))
            assertEquals(SortOrder.A_Z.name, handle["recipes.sort"])
            assertEquals(4, vm.uiState.value.meals.size) // full list back
            job.cancel()
        }
}
