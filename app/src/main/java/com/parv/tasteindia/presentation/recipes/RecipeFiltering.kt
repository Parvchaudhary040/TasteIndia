package com.parv.tasteindia.presentation.recipes

import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.SortOrder

/**
 * Pure filter + sort pipeline, applied to the in-memory Indian base set.
 *
 * @param categoryIds   allowed ids for the active category filter, already intersected with the
 *                       Indian base set by the repository. `null` = category filter inactive OR
 *                       its ids are still resolving (in which case we don't hide anything yet).
 * @param ingredientIds  same, for the main-ingredient filter.
 *
 * Because [base] is itself the Indian base set, filtering by membership in [categoryIds] /
 * [ingredientIds] is a second intersection with the Indian boundary — a category/ingredient meal
 * that isn't Indian can never appear here.
 */
fun applyFilters(
    base: List<Meal>,
    filters: RecipesFilters,
    favouriteIds: Set<String>,
    categoryIds: Set<String>?,
    ingredientIds: Set<String>?,
): List<Meal> {
    var meals = base.asSequence()

    if (filters.favouritesOnly) {
        meals = meals.filter { it.id in favouriteIds }
    }
    if (filters.category != null && categoryIds != null) {
        meals = meals.filter { it.id in categoryIds }
    }
    if (filters.ingredient != null && ingredientIds != null) {
        meals = meals.filter { it.id in ingredientIds }
    }
    val query = filters.query.trim()
    if (query.isNotEmpty()) {
        meals = meals.filter { it.name.contains(query, ignoreCase = true) }
    }

    val sorted = meals.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    return when (filters.sort) {
        SortOrder.A_Z -> sorted.toList()
        SortOrder.Z_A -> sorted.toList().asReversed()
    }
}
