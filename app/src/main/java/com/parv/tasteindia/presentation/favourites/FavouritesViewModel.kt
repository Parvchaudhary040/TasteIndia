package com.parv.tasteindia.presentation.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parv.tasteindia.di.TasteIndiaApp
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.getOrNull
import com.parv.tasteindia.domain.repository.FavouritesRepository
import com.parv.tasteindia.domain.repository.MealRepository
import com.parv.tasteindia.presentation.recipes.RecipeListItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The separate Favourites destination. Persistence is entirely Room ([FavouritesRepository]);
 * this VM just turns the observed id set into displayable rows, resolving each id from the
 * Indian base set and, failing that, the cached detail — so it still shows something with no
 * network as long as the meal has been seen before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModel(
    private val mealRepository: MealRepository,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<FavouritesUiState> =
        combine(favouritesRepository.observeFavouriteIds(), retryTrigger) { ids, _ -> ids }
            .mapLatest { ids ->
                if (ids.isEmpty()) {
                    FavouritesUiState(status = FavouritesUiState.Status.Empty)
                } else {
                    val resolved = resolve(ids)
                    FavouritesUiState(
                        status = FavouritesUiState.Status.Content,
                        meals = resolved,
                        unresolvedCount = ids.size - resolved.size,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = FavouritesUiState(),
            )

    fun toggleFavourite(id: String) {
        viewModelScope.launch { favouritesRepository.toggle(id) }
    }

    fun retry() {
        retryTrigger.value += 1
    }

    private suspend fun resolve(ids: Set<String>): List<RecipeListItem> {
        val base: Map<String, Meal> =
            mealRepository.getIndianMeals().getOrNull().orEmpty().associateBy { it.id }

        return ids
            .mapNotNull { id ->
                base[id]?.let { meal ->
                    return@mapNotNull RecipeListItem(meal.id, meal.name, meal.thumbnailUrl, isFavourite = true)
                }
                // Not in the base set (offline, or an old favourite): try the cached detail.
                mealRepository.getMealDetail(id).getOrNull()?.let { detail ->
                    RecipeListItem(detail.id, detail.name, detail.thumbnailUrl, isFavourite = true)
                }
            }
            .sortedBy { it.name.lowercase() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TasteIndiaApp).container
                FavouritesViewModel(
                    mealRepository = container.mealRepository,
                    favouritesRepository = container.favouritesRepository,
                )
            }
        }
    }
}
