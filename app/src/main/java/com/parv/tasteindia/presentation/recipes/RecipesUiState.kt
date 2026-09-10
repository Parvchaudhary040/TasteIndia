package com.parv.tasteindia.presentation.recipes

import androidx.compose.runtime.Immutable
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.SortOrder

/**
 * Immutable state for the Recipes screen. One data class (rather than a sealed hierarchy) so the
 * filter/search state sits alongside the load status without the two getting out of sync.
 */
@Immutable
data class RecipesUiState(
    val status: LoadStatus = LoadStatus.Loading,
    val meals: List<RecipeListItem> = emptyList(),
    val error: AppError? = null,
    val filters: RecipesFilters = RecipesFilters(),
    /** A category/ingredient filter's ids are still resolving; the list shown is provisional. */
    val isFiltering: Boolean = false,
) {
    enum class LoadStatus { Loading, Success, Error }

    /** Success, but nothing matches. */
    val isEmpty: Boolean get() = status == LoadStatus.Success && meals.isEmpty()

    val resultCount: Int get() = meals.size

    val activeFilterCount: Int get() = filters.activeFilterCount

    val sort: SortOrder get() = filters.sort

    /** Whether "Clear all" has anything to do. */
    val canClearAll: Boolean get() = !filters.isDefault
}

/** What a single recipe row needs — nothing more (no category/area until the details screen). */
@Immutable
data class RecipeListItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val isFavourite: Boolean,
)
