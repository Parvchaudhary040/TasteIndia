package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parv.tasteindia.domain.model.SortOrder
import com.parv.tasteindia.presentation.common.EmptyState
import com.parv.tasteindia.presentation.common.ErrorState
import com.parv.tasteindia.presentation.common.LoadingState
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * Indian recipe list with search, filters (category / ingredient / favourites-only), A–Z / Z–A
 * sort, active-filter chips, a live result count, empty + error states and Clear all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onMealClick: (String) -> Unit,
    onOpenFavourites: () -> Unit,
    viewModel: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.queryInput.collectAsStateWithLifecycle()

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TasteIndia") },
                actions = {
                    IconButton(onClick = onOpenFavourites) {
                        Icon(TasteIndiaIcons.Favorite, contentDescription = "Favourites")
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = TasteIndiaIcons.FilterList,
                            contentDescription = filterActionLabel(state.activeFilterCount),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            SearchField(
                query = query,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = { viewModel.onSearchQueryChange("") },
            )

            ActiveFilterChips(
                filters = state.filters,
                onClearQuery = { viewModel.onSearchQueryChange("") },
                onClearCategory = { viewModel.onCategorySelected(null) },
                onClearIngredient = { viewModel.onIngredientSelected(null) },
                onClearFavouritesOnly = { viewModel.onFavouritesOnlyChange(false) },
                onResetSort = { viewModel.onSortChange(SortOrder.A_Z) },
                onClearAll = viewModel::clearAllFilters,
                modifier = Modifier.padding(top = 4.dp),
            )

            when (state.status) {
                RecipesUiState.LoadStatus.Loading ->
                    LoadingState(Modifier.fillMaxSize())

                RecipesUiState.LoadStatus.Error ->
                    ErrorState(
                        error = state.error,
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )

                RecipesUiState.LoadStatus.Success -> {
                    if (state.isFiltering) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    ResultCount(
                        count = state.resultCount,
                        filtered = state.activeFilterCount > 0,
                    )
                    if (state.isEmpty) {
                        EmptyState(
                            title = "No recipes match",
                            body = emptyBody(state),
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        RecipeList(
                            items = state.meals,
                            onMealClick = onMealClick,
                            onToggleFavourite = viewModel::toggleFavourite,
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
        ) {
            FiltersSheetContent(
                filters = state.filters,
                onSortChange = viewModel::onSortChange,
                onFavouritesOnlyChange = viewModel::onFavouritesOnlyChange,
                onCategorySelected = viewModel::onCategorySelected,
                onIngredientSelected = viewModel::onIngredientSelected,
                onClearAll = viewModel::clearAllFilters,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        label = { Text("Search recipes") },
        leadingIcon = { Icon(TasteIndiaIcons.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(TasteIndiaIcons.Close, contentDescription = "Clear search text")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun ResultCount(count: Int, filtered: Boolean) {
    val text = when {
        count == 1 -> "1 recipe"
        else -> "$count recipes"
    } + if (filtered) " match your filters" else ""
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$count recipes" + if (filtered) ", filtered" else ""
            },
    )
}

@Composable
private fun RecipeList(
    items: List<RecipeListItem>,
    onMealClick: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = items, key = { it.id }) { item ->
            MealRow(
                item = item,
                onClick = { onMealClick(item.id) },
                onToggleFavourite = { onToggleFavourite(item.id) },
            )
        }
    }
}

private fun filterActionLabel(activeCount: Int): String =
    if (activeCount == 0) "Filters" else "Filters, $activeCount active"

private fun emptyBody(state: RecipesUiState): String {
    val f = state.filters
    return when {
        f.favouritesOnly && f.activeFilterCount == 1 ->
            "You haven’t added any favourites yet. Tap the heart on a recipe to save it."
        f.activeFilterCount > 0 ->
            "No Indian recipes match this combination of filters. Try removing one, or Clear all."
        else ->
            "We couldn’t find any Indian recipes right now. Pull to refresh or try again later."
    }
}
