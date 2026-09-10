package com.parv.tasteindia.data.repository

import com.parv.tasteindia.data.remote.dto.MealListResponseDto
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.testutil.FakeCachedMealDetailDao
import com.parv.tasteindia.testutil.FakeMealApi
import com.parv.tasteindia.testutil.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test requirement #2: category / ingredient filters must intersect their returned meal IDs
 * with the Indian base set, and must never let a query escape that boundary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IndianBoundaryIntersectionTest {

    private val indianBase = Fixtures.decode<MealListResponseDto>("filter_indian.json")
    private val seafood = Fixtures.decode<MealListResponseDto>("filter_category_seafood.json")
    private val chicken = Fixtures.decode<MealListResponseDto>("filter_ingredient_chicken.json")
    private val empty = Fixtures.decode<MealListResponseDto>("filter_empty.json")

    private fun TestScope.repository(api: FakeMealApi): MealRepositoryImpl =
        MealRepositoryImpl(
            api = api,
            cacheDao = FakeCachedMealDetailDao(),
            json = Fixtures.json,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            scope = backgroundScope,
        )

    @Test
    fun `category filter keeps only ids that are also in the Indian base set`() = runTest {
        val api = FakeMealApi(
            areaResponse = indianBase,
            categoryResponses = mapOf("Seafood" to seafood),
        )

        val result = repository(api).getIndianMealIdsForCategory("Seafood")

        // seafood fixture: 52932 + 52957 are Indian; 52815 / 52816 / 52819 are not.
        assertEquals(setOf("52932", "52957"), (result as DataResult.Success).data)
    }

    @Test
    fun `ingredient filter keeps only ids that are also in the Indian base set`() = runTest {
        val api = FakeMealApi(
            areaResponse = indianBase,
            ingredientResponses = mapOf("chicken_breast" to chicken),
        )

        val result = repository(api).getIndianMealIdsForIngredient("chicken_breast")

        // chicken fixture: 52795 + 52930 are Indian; 52840 / 52846 are not.
        assertEquals(setOf("52795", "52930"), (result as DataResult.Success).data)
    }

    @Test
    fun `a category with no Indian overlap yields an empty set, not the raw response`() = runTest {
        val indianIds = indianBase.meals!!.map { it.idMeal }.toSet()
        val onlyForeign = MealListResponseDto(
            meals = seafood.meals!!.filterNot { it.idMeal in indianIds },
        )
        val api = FakeMealApi(
            areaResponse = indianBase,
            categoryResponses = mapOf("Seafood" to onlyForeign),
        )

        val result = repository(api).getIndianMealIdsForCategory("Seafood")

        assertTrue((result as DataResult.Success).data.isEmpty())
    }

    @Test
    fun `an empty API response intersects to an empty set`() = runTest {
        val api = FakeMealApi(
            areaResponse = indianBase,
            categoryResponses = mapOf("Nonexistent" to empty),
        )

        val result = repository(api).getIndianMealIdsForCategory("Nonexistent")

        assertTrue((result as DataResult.Success).data.isEmpty())
    }

    @Test
    fun `partial rows without a usable name are dropped from the base set`() = runTest {
        val partial = Fixtures.decode<MealListResponseDto>("filter_indian_partial.json")
        val api = FakeMealApi(areaResponse = partial)

        val result = repository(api).getIndianMeals()

        // fixture has 4 rows; one has a blank strMeal ("  ") and must not appear.
        val names = (result as DataResult.Success).data.map { it.name }
        assertEquals(listOf("Baingan Bharta", "Bread omelette", "Lamb Biryani"), names.sorted())
    }

    @Test
    fun `the Indian base set is fetched from the network only once`() = runTest {
        val api = FakeMealApi(
            areaResponse = indianBase,
            categoryResponses = mapOf("Seafood" to seafood),
            ingredientResponses = mapOf("chicken_breast" to chicken),
        )
        val repo = repository(api)

        repo.getIndianMeals()
        repo.getIndianMealIdsForCategory("Seafood")
        repo.getIndianMealIdsForIngredient("chicken_breast")

        assertEquals(1, api.filterByAreaCalls)
    }
}
