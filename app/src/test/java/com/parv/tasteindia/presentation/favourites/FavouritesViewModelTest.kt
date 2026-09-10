package com.parv.tasteindia.presentation.favourites

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun summary(id: String, name: String) =
        MealSummaryDto(idMeal = id, strMeal = name, strMealThumb = null)

    private val indianBase = MealListResponseDto(
        meals = listOf(
            summary("1", "Aloo Gobi"),
            summary("2", "Butter Chicken"),
            summary("3", "Chicken Handi"),
        ),
    )

    private fun viewModel(favourites: FakeFavouritesRepository) = FavouritesViewModel(
        mealRepository = MealRepositoryImpl(
            api = FakeMealApi(areaResponse = indianBase),
            cacheDao = FakeCachedMealDetailDao(),
            json = Fixtures.json,
            ioDispatcher = mainDispatcherRule.dispatcher,
            scope = CoroutineScope(mainDispatcherRule.dispatcher),
        ),
        favouritesRepository = favourites,
    )

    @Test
    fun `empty when there are no favourites`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = viewModel(FakeFavouritesRepository())
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(FavouritesUiState.Status.Empty, vm.uiState.value.status)
        job.cancel()
    }

    @Test
    fun `resolves favourite ids against the Indian base set, sorted by name`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel(FakeFavouritesRepository(setOf("3", "1")))
            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(FavouritesUiState.Status.Content, state.status)
            assertEquals(listOf("Aloo Gobi", "Chicken Handi"), state.meals.map { it.name })
            assertEquals(2, state.savedCount)
            job.cancel()
        }

    @Test
    fun `un-favouriting a meal removes it from the list live`() =
        runTest(mainDispatcherRule.dispatcher) {
            val favourites = FakeFavouritesRepository(setOf("1", "2"))
            val vm = viewModel(favourites)
            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(2, vm.uiState.value.meals.size)

            vm.toggleFavourite("1")
            advanceUntilIdle()

            assertEquals(listOf("2"), vm.uiState.value.meals.map { it.id })
            job.cancel()
        }

    @Test
    fun `a favourite id not in the base set is counted as unresolved`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = viewModel(FakeFavouritesRepository(setOf("1", "999")))
            val job = launch { vm.uiState.collect {} }
            advanceUntilIdle()

            val state = vm.uiState.value
            assertEquals(listOf("1"), state.meals.map { it.id })
            assertEquals(1, state.unresolvedCount)
            assertEquals(2, state.savedCount)
            job.cancel()
        }
}
