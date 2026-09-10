package com.parv.tasteindia.presentation.details

import com.parv.tasteindia.data.remote.dto.MealDetailResponseDto
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lookup = Fixtures.decode<MealDetailResponseDto>("lookup_normal.json")
    private val mealId = lookup.meals!!.single().idMeal

    private fun viewModel(favourites: FakeFavouritesRepository = FakeFavouritesRepository()) =
        DetailsViewModel(
            mealId = mealId,
            mealRepository = MealRepositoryImpl(
                api = FakeMealApi(lookupResponses = mapOf(mealId to lookup)),
                cacheDao = FakeCachedMealDetailDao(),
                json = Fixtures.json,
                ioDispatcher = mainDispatcherRule.dispatcher,
                scope = CoroutineScope(mainDispatcherRule.dispatcher),
            ),
            favouritesRepository = favourites,
        )

    @Test
    fun `loads the meal detail by id`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = viewModel()
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(DetailsUiState.LoadStatus.Success, state.status)
        assertEquals("Chicken Handi", state.detail?.name)
        assertEquals(8, state.detail?.ingredients?.size)
        job.cancel()
    }

    @Test
    fun `favourite state reflects the repository and toggles`() = runTest(mainDispatcherRule.dispatcher) {
        val favourites = FakeFavouritesRepository()
        val vm = viewModel(favourites)
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(!vm.uiState.value.isFavourite)

        vm.toggleFavourite()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isFavourite)
        assertTrue(mealId in favourites.current)

        vm.toggleFavourite()
        advanceUntilIdle()
        assertTrue(!vm.uiState.value.isFavourite)
        job.cancel()
    }

    @Test
    fun `a lookup with no meal surfaces NotFound`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = DetailsViewModel(
            mealId = "does-not-exist",
            mealRepository = MealRepositoryImpl(
                api = FakeMealApi(lookupResponses = mapOf("does-not-exist" to MealDetailResponseDto(meals = null))),
                cacheDao = FakeCachedMealDetailDao(),
                json = Fixtures.json,
                ioDispatcher = mainDispatcherRule.dispatcher,
                scope = CoroutineScope(mainDispatcherRule.dispatcher),
            ),
            favouritesRepository = FakeFavouritesRepository(),
        )
        val job = launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(DetailsUiState.LoadStatus.Error, vm.uiState.value.status)
        job.cancel()
    }
}
