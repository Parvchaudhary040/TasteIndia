package com.parv.tasteindia.domain.model

/**
 * A meal as it appears in the recipe list.
 *
 * This is deliberately minimal: `filter.php?a=Indian` only returns id, name and thumbnail,
 * so the list row must not promise category/area until it is legitimately available via
 * detail enrichment ([MealDetail]).
 */
data class Meal(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
)
