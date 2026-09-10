package com.parv.tasteindia.presentation.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.parv.tasteindia.di.TasteIndiaApp
import com.parv.tasteindia.domain.model.AppError
import com.parv.tasteindia.domain.model.DataResult
import com.parv.tasteindia.domain.model.Meal
import com.parv.tasteindia.domain.model.SortOrder
import com.parv.tasteindia.domain.repository.FavouritesRepository
import com.parv.tasteindia.domain.repository.MealRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Recipes screen: the Indian base set, plus search / category / ingredient / favourites-only
 * filtering and A–Z / Z–A sorting.
 *
 * Pipeline
 * --------
 *  - `queryInput` is the immediate text-field value; a debounced, de-duplicated copy feeds the
 *    filter combine, so rapid typing neither thrashes the list nor leaves a stale result.
 *  - Category / ingredient selections resolve to Indian-intersected id sets via `flatMapLatest`,
 *    so only the latest selection's result is ever applied.
 *  - Everything is `combine`d and reduced by the pure [applyFilters]; filtering by the resolved
 *    id sets is a second intersection with the Indian boundary.
 *
 * All selections live in [SavedStateHandle], so Phase 7 state restoration is automatic.
 */
// FlowPreview: Flow.debounce (long-standing, widely used, API-stable in practice).
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RecipesViewModel(
    private val handle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val favouritesRepository: FavouritesRepository,
) : ViewModel() {

    private sealed interface BaseLoad {
        data object Loading : BaseLoad
        data class Ready(val meals: List<Meal>) : BaseLoad
        data class Failed(val error: AppError) : BaseLoad
    }

    /** Resolution of one category/ingredient selection into Indian-intersected ids. */
    private sealed interface FilterResolution {
        data object Inactive : FilterResolution
        data object Loading : FilterResolution
        data class Ready(val ids: Set<String>) : FilterResolution
        data class Failed(val error: AppError) : FilterResolution
    }

    private val retryTrigger = MutableStateFlow(0)
    private val baseLoad = MutableStateFlow<BaseLoad>(BaseLoad.Loading)

    // --- selections (persisted) ------------------------------------------------------------
    val queryInput: StateFlow<String> = handle.getStateFlow(KEY_QUERY, "")
    private val category: StateFlow<String?> = handle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val ingredient: StateFlow<String?> = handle.getStateFlow<String?>(KEY_INGREDIENT, null)
    private val favouritesOnly: StateFlow<Boolean> = handle.getStateFlow(KEY_FAV_ONLY, false)
    private val sortName: StateFlow<String> = handle.getStateFlow(KEY_SORT, SortOrder.A_Z.name)

    private val filters: Flow<RecipesFilters> = combine(
        // Debounced + de-duplicated so rapid typing doesn't thrash the list or leave stale results.
        queryInput.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
        category,
        ingredient,
        favouritesOnly,
        sortName,
    ) { query, cat, ing, favOnly, sort ->
        RecipesFilters(
            query = query,
            category = cat,
            ingredient = ing,
            favouritesOnly = favOnly,
            sort = runCatching { SortOrder.valueOf(sort) }.getOrDefault(SortOrder.A_Z),
        )
    }

    private val categoryResolution: Flow<FilterResolution> =
        combine(category, retryTrigger) { cat, _ -> cat }
            .flatMapLatest { cat -> resolve(cat, mealRepository::getIndianMealIdsForCategory) }

    private val ingredientResolution: Flow<FilterResolution> =
        combine(ingredient, retryTrigger) { ing, _ -> ing }
            .flatMapLatest { ing -> resolve(ing, mealRepository::getIndianMealIdsForIngredient) }

    val uiState: StateFlow<RecipesUiState> = combine(
        baseLoad,
        filters,
        favouritesRepository.observeFavouriteIds(),
        categoryResolution,
        ingredientResolution,
    ) { base, activeFilters, favouriteIds, catRes, ingRes ->
        reduce(base, activeFilters, favouriteIds, catRes, ingRes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecipesUiState(),
    )

    init {
        loadIndianMeals(forceRefresh = false)
    }

    // --- intents -------------------------------------------------------------------------------

    fun onSearchQueryChange(value: String) {
        handle[KEY_QUERY] = value
    }

    fun onCategorySelected(value: String?) {
        handle[KEY_CATEGORY] = value
    }

    fun onIngredientSelected(value: String?) {
        handle[KEY_INGREDIENT] = value
    }

    fun onFavouritesOnlyChange(enabled: Boolean) {
        handle[KEY_FAV_ONLY] = enabled
    }

    fun onSortChange(sort: SortOrder) {
        handle[KEY_SORT] = sort.name
    }

    fun clearAllFilters() {
        handle[KEY_QUERY] = ""
        handle[KEY_CATEGORY] = null
        handle[KEY_INGREDIENT] = null
        handle[KEY_FAV_ONLY] = false
        handle[KEY_SORT] = SortOrder.A_Z.name
    }

    fun toggleFavourite(id: String) {
        viewModelScope.launch { favouritesRepository.toggle(id) }
    }

    fun retry() {
        retryTrigger.value += 1
        loadIndianMeals(forceRefresh = true)
    }

    // --- internals ----------------------------------------------------------------------------

    private fun resolve(
        selection: String?,
        query: suspend (String) -> DataResult<Set<String>>,
    ): Flow<FilterResolution> =
        if (selection == null) {
            flowOf(FilterResolution.Inactive)
        } else {
            flow {
                emit(FilterResolution.Loading)
                emit(
                    when (val result = query(selection)) {
                        is DataResult.Success -> FilterResolution.Ready(result.data)
                        is DataResult.Failure -> FilterResolution.Failed(result.error)
                    }
                )
            }
        }

    private fun reduce(
        base: BaseLoad,
        activeFilters: RecipesFilters,
        favouriteIds: Set<String>,
        catRes: FilterResolution,
        ingRes: FilterResolution,
    ): RecipesUiState {
        val baseMeals = when (base) {
            is BaseLoad.Loading -> return RecipesUiState(
                status = RecipesUiState.LoadStatus.Loading,
                filters = activeFilters,
            )
            is BaseLoad.Failed -> return RecipesUiState(
                status = RecipesUiState.LoadStatus.Error,
                error = base.error,
                filters = activeFilters,
            )
            is BaseLoad.Ready -> base.meals
        }

        // A failed filter resolution is surfaced as a screen error with retry.
        (catRes as? FilterResolution.Failed)?.let {
            return RecipesUiState(RecipesUiState.LoadStatus.Error, error = it.error, filters = activeFilters)
        }
        (ingRes as? FilterResolution.Failed)?.let {
            return RecipesUiState(RecipesUiState.LoadStatus.Error, error = it.error, filters = activeFilters)
        }

        val categoryIds = (catRes as? FilterResolution.Ready)?.ids
        val ingredientIds = (ingRes as? FilterResolution.Ready)?.ids
        val stillResolving = catRes is FilterResolution.Loading || ingRes is FilterResolution.Loading

        val filtered = applyFilters(
            base = baseMeals,
            filters = activeFilters,
            favouriteIds = favouriteIds,
            categoryIds = categoryIds,
            ingredientIds = ingredientIds,
        )

        return RecipesUiState(
            status = RecipesUiState.LoadStatus.Success,
            meals = filtered.map { meal ->
                RecipeListItem(
                    id = meal.id,
                    name = meal.name,
                    thumbnailUrl = meal.thumbnailUrl,
                    isFavourite = meal.id in favouriteIds,
                )
            },
            filters = activeFilters,
            isFiltering = stillResolving,
        )
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
        private const val SEARCH_DEBOUNCE_MS = 200L

        private const val KEY_QUERY = "recipes.query"
        private const val KEY_CATEGORY = "recipes.category"
        private const val KEY_INGREDIENT = "recipes.ingredient"
        private const val KEY_FAV_ONLY = "recipes.favouritesOnly"
        private const val KEY_SORT = "recipes.sort"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as TasteIndiaApp).container
                RecipesViewModel(
                    handle = createSavedStateHandle(),
                    mealRepository = container.mealRepository,
                    favouritesRepository = container.favouritesRepository,
                )
            }
        }
    }
}
