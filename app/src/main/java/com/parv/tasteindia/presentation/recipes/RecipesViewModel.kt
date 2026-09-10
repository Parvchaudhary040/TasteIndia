package com.parv.tasteindia.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parv.tasteindia.di.TasteIndiaApp
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.repository.FavouritesRepository
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Holds the Indian meal list and favourite state for the Recipes screen.
 *
 * Search / filtering / sorting land in Phase 5; the shape here (a base-load flow combined with
 * the favourites flow) is what those extra inputs will be `combine`d into.
 */
class RecipesViewModel(
    private val mealRepository: MealRepository,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    /** Result of the last Indian base-set load. */
    private sealed interface BaseLoad {
        data object Loading : BaseLoad
        data class Ready(val meals: List<Meal>) : BaseLoad
        data class Failed(val error: com.parv.tasteindia.domain.model.AppError) : BaseLoad
    }

    private val baseLoad = MutableStateFlow<BaseLoad>(BaseLoad.Loading)

    val uiState: StateFlow<RecipesUiState> =
        combine(baseLoad, favouritesRepository.observeFavouriteIds()) { load, favouriteIds ->
            when (load) {
                is BaseLoad.Loading -> RecipesUiState(status = RecipesUiState.LoadStatus.Loading)
                is BaseLoad.Failed -> RecipesUiState(
                    status = RecipesUiState.LoadStatus.Error,
                    error = load.error,
                )
                is BaseLoad.Ready -> RecipesUiState(
                    status = RecipesUiState.LoadStatus.Success,
                    meals = load.meals.map { meal ->
                        RecipeListItem(
                            id = meal.id,
                            name = meal.name,
                            thumbnailUrl = meal.thumbnailUrl,
                            isFavourite = meal.id in favouriteIds,
                        )
                    },
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = RecipesUiState(),
        )

    init {
        loadIndianMeals(forceRefresh = false)
    }

    fun retry() = loadIndianMeals(forceRefresh = true)

    fun toggleFavourite(id: String) {
        viewModelScope.launch { favouritesRepository.toggle(id) }
    }

    private fun loadIndianMeals(forceRefresh: Boolean) {
        viewModelScope.launch {
            baseLoad.value = BaseLoad.Loading
            baseLoad.value = when (val result = mealRepository.getIndianMeals(forceRefresh)) {
                is DataResult.Success -> BaseLoad.Ready(result.data)
                is DataResult.Failure -> BaseLoad.Failed(result.error)
            }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TasteIndiaApp).container
                RecipesViewModel(
                    mealRepository = container.mealRepository,
                    favouritesRepository = container.favouritesRepository,
                )
            }
        }
    }
}
