package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parv.tasteindia.domain.model.SortOrder

/**
 * Contents of the filter bottom sheet: sort, favourites-only, category and main ingredient.
 * Single-select for category/ingredient — tapping the selected chip clears it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheetContent(
    filters: RecipesFilters,
    onSortChange: (SortOrder) -> Unit,
    onFavouritesOnlyChange: (Boolean) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onIngredientSelected: (String?) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClearAll, enabled = !filters.isDefault) { Text("Clear all") }
        }

        Section("Sort") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SortOrder.entries.forEachIndexed { index, order ->
                    SegmentedButton(
                        selected = filters.sort == order,
                        onClick = { onSortChange(order) },
                        shape = SegmentedButtonDefaults.itemShape(index, SortOrder.entries.size),
                    ) {
                        Text(if (order == SortOrder.A_Z) "A – Z" else "Z – A")
                    }
                }
            }
        }

        Section("Favourites") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show favourites only", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = filters.favouritesOnly,
                    onCheckedChange = onFavouritesOnlyChange,
                )
            }
        }

        HorizontalDivider()

        Section("Category") {
            SingleChoiceChips(
                options = FilterOptions.CATEGORIES,
                selected = filters.category,
                onToggle = { onCategorySelected(it) },
            )
        }

        Section("Main ingredient") {
            SingleChoiceChips(
                options = FilterOptions.INGREDIENTS,
                selected = filters.ingredient,
                onToggle = { onIngredientSelected(it) },
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SingleChoiceChips(
    options: List<String>,
    selected: String?,
    onToggle: (String?) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(if (isSelected) null else option) },
                label = { Text(option) },
            )
        }
    }
}
