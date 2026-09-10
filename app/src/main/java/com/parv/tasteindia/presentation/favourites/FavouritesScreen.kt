package com.parv.tasteindia.presentation.favourites

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parv.tasteindia.presentation.common.PhasePlaceholder
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * Phase 2: navigation target only. Real Room-backed favourites list is built in Phase 7.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    onMealClick: (String) -> Unit,
    onBack: () -> Unit,
) {
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
        PhasePlaceholder(
            label = "Favourites — implemented in Phase 7",
            modifier = Modifier.padding(innerPadding),
        )
    }
}
