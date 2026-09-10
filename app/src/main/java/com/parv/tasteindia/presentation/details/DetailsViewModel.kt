package com.parv.tasteindia.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parv.tasteindia.di.TasteIndiaApp
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.MealDetail
import com.parv.tasteindia.domain.repository.FavouritesRepository
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Loads one meal's full detail by id only — no `Meal`/`MealDetail` is passed through navigation.
 * The repository serves this from its in-memory / Room cache when possible (Phase 3), so
 * returning to a recipe is instant and does no network work.
 */
class DetailsViewModel(
    private val mealId: String,
    private val mealRepository: MealRepository,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    private sealed interface Load {
        data object Loading : Load
        data class Ready(val detail: MealDetail) : Load
        data class Failed(val error: AppError) : Load
    }

    private val load = MutableStateFlow<Load>(Load.Loading)

    val uiState: StateFlow<DetailsUiState> =
        combine(load, favouritesRepository.observeFavouriteIds()) { load, favouriteIds ->
            val isFavourite = mealId in favouriteIds
            when (load) {
                is Load.Loading -> DetailsUiState(DetailsUiState.LoadStatus.Loading, isFavourite = isFavourite)
                is Load.Ready -> DetailsUiState(DetailsUiState.LoadStatus.Success, load.detail, isFavourite)
                is Load.Failed -> DetailsUiState(DetailsUiState.LoadStatus.Error, error = load.error, isFavourite = isFavourite)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DetailsUiState(),
        )

    init {
        reload()
    }

    fun retry() = reload()

    fun toggleFavourite() {
        viewModelScope.launch { favouritesRepository.toggle(mealId) }
    }

    private fun reload() {
        viewModelScope.launch {
            load.value = Load.Loading
            load.value = when (val result = mealRepository.getMealDetail(mealId)) {
                is DataResult.Success -> Load.Ready(result.data)
                is DataResult.Failure -> Load.Failed(result.error)
            }
        }
    }

    companion object {
        fun factory(mealId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TasteIndiaApp).container
                DetailsViewModel(
                    mealId = mealId,
                    mealRepository = container.mealRepository,
                    favouritesRepository = container.favouritesRepository,
                )
            }
        }
    }
}
