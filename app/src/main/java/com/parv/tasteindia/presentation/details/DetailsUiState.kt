package com.parv.tasteindia.presentation.details

import androidx.compose.runtime.Immutable
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.MealDetail

@Immutable
data class DetailsUiState(
    val status: LoadStatus = LoadStatus.Loading,
    val detail: MealDetail? = null,
    val isFavourite: Boolean = false,
    val error: AppError? = null,
) {
    enum class LoadStatus { Loading, Success, Error }
}
