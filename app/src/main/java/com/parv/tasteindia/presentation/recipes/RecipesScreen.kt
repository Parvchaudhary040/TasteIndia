package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parv.tasteindia.presentation.common.EmptyState
import com.parv.tasteindia.presentation.common.ErrorState
import com.parv.tasteindia.presentation.common.LoadingState
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * Indian recipe list. Phase 4: load / success / empty / error+retry states and a LazyColumn
 * keyed by stable meal id. Search and filters arrive in Phase 5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onMealClick: (String) -> Unit,
    onOpenFavourites: () -> Unit,
    viewModel: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TasteIndia") },
                actions = {
                    IconButton(onClick = onOpenFavourites) {
                        Icon(
                            imageVector = TasteIndiaIcons.Favorite,
                            contentDescription = "Favourites",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state.status) {
            RecipesUiState.LoadStatus.Loading ->
                LoadingState(Modifier.padding(innerPadding))

            RecipesUiState.LoadStatus.Error ->
                ErrorState(
                    error = state.error,
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(innerPadding),
                )

            RecipesUiState.LoadStatus.Success ->
                if (state.isEmpty) {
                    EmptyState(
                        title = "No Indian recipes found",
                        body = "We couldn’t find any Indian recipes right now. Pull to refresh or try again later.",
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    RecipeList(
                        items = state.meals,
                        resultCount = state.resultCount,
                        contentPadding = innerPadding,
                        onMealClick = onMealClick,
                        onToggleFavourite = viewModel::toggleFavourite,
                    )
                }
        }
    }
}

@Composable
private fun RecipeList(
    items: List<RecipeListItem>,
    resultCount: Int,
    contentPadding: PaddingValues,
    onMealClick: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "result-count") {
            Text(
                text = pluralRecipes(resultCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .semantics { contentDescription = "$resultCount recipes" },
            )
        }
        items(
            items = items,
            key = { it.id }, // stable idMeal identity
        ) { item ->
            MealRow(
                item = item,
                onClick = { onMealClick(item.id) },
                onToggleFavourite = { onToggleFavourite(item.id) },
            )
        }
    }
}

private fun pluralRecipes(count: Int): String =
    if (count == 1) "1 recipe" else "$count recipes"
