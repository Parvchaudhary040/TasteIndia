package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parv.tasteindia.presentation.common.PhasePlaceholder

/**
 * Phase 2: navigation target only. The real list (LazyColumn keyed by idMeal, loading/empty/
 * error states, search + filters) is built in Phases 4 and 5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    onMealClick: (String) -> Unit,
    onOpenFavourites: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("TasteIndia") }) },
    ) { innerPadding ->
        PhasePlaceholder(
            label = "Recipes — implemented in Phase 4",
            modifier = Modifier.padding(innerPadding),
        )
    }
}
