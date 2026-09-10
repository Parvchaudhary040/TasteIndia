package com.parv.tasteindia.presentation.details

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
 * Phase 2: navigation target only, receiving nothing but [mealId]. Real detail rendering
 * (hero image, ingredient/measure pairs, tags, links) is built in Phase 6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mealId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TasteIndiaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        PhasePlaceholder(
            label = "Details for meal $mealId — implemented in Phase 6",
            modifier = Modifier.padding(innerPadding),
        )
    }
}
