package com.parv.tasteindia.presentation.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parv.tasteindia.presentation.common.EmptyState
import com.parv.tasteindia.presentation.common.LoadingState
import com.parv.tasteindia.presentation.common.TasteIndiaIcons
import com.parv.tasteindia.presentation.recipes.MealRow

/**
 * Separate Favourites destination. Opens the same ID-based Details screen and works offline for
 * any recipe that has been seen before.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onMealClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FavouritesViewModel = viewModel(factory = FavouritesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favourites") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TasteIndiaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state.status) {
            FavouritesUiState.Status.Loading ->
                LoadingState(Modifier.fillMaxSize().padding(innerPadding))

            FavouritesUiState.Status.Empty ->
                EmptyState(
                    title = "No favourites yet",
                    body = "Tap the heart on any recipe to save it here.",
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )

            FavouritesUiState.Status.Content ->
                Column(Modifier.fillMaxSize().padding(innerPadding)) {
                    SavedCount(state.savedCount)
                    if (state.meals.isEmpty()) {
                        EmptyState(
                            title = "Can’t show your favourites",
                            body = "You have ${state.savedCount} saved, but they can’t be loaded " +
                                "right now. Reconnect and try again.",
                            modifier = Modifier.fillMaxSize(),
                            action = { Button(onClick = viewModel::retry) { Text("Try again") } },
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = state.meals, key = { it.id }) { item ->
                                MealRow(
                                    item = item,
                                    onClick = { onMealClick(item.id) },
                                    onToggleFavourite = { viewModel.toggleFavourite(item.id) },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SavedCount(count: Int) {
    Text(
        text = if (count == 1) "1 saved recipe" else "$count saved recipes",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$count saved recipes"
            },
    )
}
