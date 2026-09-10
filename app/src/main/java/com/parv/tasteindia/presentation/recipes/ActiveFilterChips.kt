package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.parv.tasteindia.domain.model.SortOrder
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * Row of chips for the filters currently in effect, each removable, plus "Clear all". Nothing is
 * shown when the selection is at its default. A non-default sort shows as a (removable) chip too.
 */
@Composable
fun ActiveFilterChips(
    filters: RecipesFilters,
    onClearQuery: () -> Unit,
    onClearCategory: () -> Unit,
    onClearIngredient: () -> Unit,
    onClearFavouritesOnly: () -> Unit,
    onResetSort: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filters.isDefault) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val query = filters.query.trim()
        if (query.isNotEmpty()) {
            RemovableChip("Search: “$query”", "Clear search filter", onClearQuery)
        }
        filters.category?.let {
            RemovableChip("Category: $it", "Clear category filter", onClearCategory)
        }
        filters.ingredient?.let {
            RemovableChip("Ingredient: $it", "Clear ingredient filter", onClearIngredient)
        }
        if (filters.favouritesOnly) {
            RemovableChip("Favourites only", "Clear favourites-only filter", onClearFavouritesOnly)
        }
        if (filters.sort != SortOrder.A_Z) {
            RemovableChip("Sorted Z–A", "Reset sort to A–Z", onResetSort)
        }
        TextButton(onClick = onClearAll) { Text("Clear all") }
    }
}

@Composable
private fun RemovableChip(
    label: String,
    removeActionLabel: String,
    onRemove: () -> Unit,
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = TasteIndiaIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.AvatarSize),
            )
        },
        modifier = Modifier.clearAndSetSemantics { contentDescription = removeActionLabel },
    )
}
