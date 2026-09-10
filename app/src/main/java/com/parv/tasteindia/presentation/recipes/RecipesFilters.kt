package com.parv.tasteindia.presentation.recipes

import androidx.compose.runtime.Immutable
import com.parv.tasteindia.domain.model.SortOrder

/**
 * The full, immutable filter/search/sort selection for the Recipes screen. Every field is a
 * plain value so it round-trips through `SavedStateHandle` (Phase 7 state restoration).
 */
@Immutable
data class RecipesFilters(
    val query: String = "",
    val category: String? = null,
    val ingredient: String? = null,
    val favouritesOnly: Boolean = false,
    val sort: SortOrder = SortOrder.A_Z,
) {
    /** Number of narrowing filters in effect. Sort is not a "filter" — it never hides a meal. */
    val activeFilterCount: Int
        get() = listOf(
            query.trim().isNotEmpty(),
            category != null,
            ingredient != null,
            favouritesOnly,
        ).count { it }

    /** Whether "Clear all" should do anything (also resets a non-default sort). */
    val isDefault: Boolean
        get() = activeFilterCount == 0 && sort == SortOrder.A_Z

    companion object {
        val DEFAULT = RecipesFilters()
    }
}
