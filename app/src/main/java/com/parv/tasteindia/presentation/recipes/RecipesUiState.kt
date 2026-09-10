package com.parv.tasteindia.presentation.recipes

import androidx.compose.runtime.Immutable
import com.parv.tasteindia.domain.model.AppError

/**
 * Immutable state for the Recipes screen. One data class (rather than a sealed hierarchy) so
 * that the filter/search state added in Phase 5 can sit alongside the load status without the
 * two getting out of sync.
 */
@Immutable
data class RecipesUiState(
    val status: LoadStatus = LoadStatus.Loading,
    val meals: List<RecipeListItem> = emptyList(),
    val error: AppError? = null,
) {
    enum class LoadStatus { Loading, Success, Error }

    /** Success, but nothing to show. */
    val isEmpty: Boolean get() = status == LoadStatus.Success && meals.isEmpty()

    val resultCount: Int get() = meals.size
}

/** What a single recipe row needs — nothing more (no category/area until the details screen). */
@Immutable
data class RecipeListItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val isFavourite: Boolean,
)
