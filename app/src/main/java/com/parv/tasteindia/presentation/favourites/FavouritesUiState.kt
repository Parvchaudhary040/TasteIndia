package com.parv.tasteindia.presentation.favourites

import androidx.compose.runtime.Immutable
import com.parv.tasteindia.presentation.recipes.RecipeListItem

@Immutable
data class FavouritesUiState(
    val status: Status = Status.Loading,
    val meals: List<RecipeListItem> = emptyList(),
    /** Favourite ids that couldn't be resolved to a meal (e.g. offline and never viewed). */
    val unresolvedCount: Int = 0,
) {
    enum class Status { Loading, Empty, Content }

    val savedCount: Int get() = meals.size + unresolvedCount
}
