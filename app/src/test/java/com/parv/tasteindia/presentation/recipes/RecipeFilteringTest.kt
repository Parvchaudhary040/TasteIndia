package com.parv.tasteindia.presentation.recipes

import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure filter + sort pipeline. Combined-filter behaviour and the Indian-boundary second
 * intersection are exercised here; the network side is covered by
 * [com.parv.tasteindia.data.repository.IndianBoundaryIntersectionTest].
 */
class RecipeFilteringTest {

    private fun meal(id: String, name: String) = Meal(id, name, thumbnailUrl = null)

    private val base = listOf(
        meal("1", "Aloo Gobi"),
        meal("2", "Butter Chicken"),
        meal("3", "Chicken Handi"),
        meal("4", "Dal Makhani"),
        meal("5", "Prawn Curry"),
    )

    private fun filter(
        filters: RecipesFilters,
        favouriteIds: Set<String> = emptySet(),
        categoryIds: Set<String>? = null,
        ingredientIds: Set<String>? = null,
    ) = applyFilters(base, filters, favouriteIds, categoryIds, ingredientIds).map { it.id }

    @Test
    fun `no filters returns everything, A-Z by name`() {
        assertEquals(listOf("1", "2", "3", "4", "5"), filter(RecipesFilters()))
    }

    @Test
    fun `Z-A reverses the name order`() {
        assertEquals(
            listOf("5", "4", "3", "2", "1"),
            filter(RecipesFilters(sort = SortOrder.Z_A)),
        )
    }

    @Test
    fun `query is a trimmed, case-insensitive substring match on the name`() {
        assertEquals(listOf("2", "3"), filter(RecipesFilters(query = "  chicken ")))
        assertEquals(listOf("1"), filter(RecipesFilters(query = "GOBI")))
        assertEquals(emptyList<String>(), filter(RecipesFilters(query = "pizza")))
    }

    @Test
    fun `favouritesOnly keeps only favourited ids`() {
        assertEquals(
            listOf("2", "4"),
            filter(RecipesFilters(favouritesOnly = true), favouriteIds = setOf("2", "4")),
        )
    }

    @Test
    fun `category filter keeps only ids in the resolved set`() {
        assertEquals(
            listOf("2", "3"),
            filter(RecipesFilters(category = "Chicken"), categoryIds = setOf("2", "3", "99")),
        )
    }

    @Test
    fun `an unresolved category filter does not hide anything yet`() {
        // category selected, but ids still null (resolving)
        assertEquals(
            listOf("1", "2", "3", "4", "5"),
            filter(RecipesFilters(category = "Chicken"), categoryIds = null),
        )
    }

    @Test
    fun `filters combine with AND`() {
        val result = filter(
            RecipesFilters(
                query = "chicken",
                category = "Chicken",
                favouritesOnly = true,
                sort = SortOrder.Z_A,
            ),
            favouriteIds = setOf("3"),
            categoryIds = setOf("2", "3"),
        )
        // query -> {2,3}; category -> {2,3}; favourites -> {3}; Z-A of {3} = [3]
        assertEquals(listOf("3"), result)
    }

    @Test
    fun `category id outside the base list can never appear`() {
        // "99" is in the category set but not in the Indian base -> excluded
        assertEquals(
            listOf("2"),
            filter(RecipesFilters(category = "Chicken"), categoryIds = setOf("2", "99")),
        )
    }
}
