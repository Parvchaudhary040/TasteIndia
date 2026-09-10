package com.parv.tasteindia.presentation.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.parv.tasteindia.presentation.common.MealImage
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * One recipe row: thumbnail, name, favourite toggle. Stable identity is the caller's job
 * (`key = { it.id }` in the list). No category/area here — that's only known on the details
 * screen.
 */
@Composable
fun MealRow(
    item: RecipeListItem,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.semantics {
            // The row is one tap target for TalkBack; the button below stays separately focusable.
            role = Role.Button
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MealImage(
                url = item.thumbnailUrl,
                contentDescription = null, // name is right next to it; avoid double announcement
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            FavouriteToggle(
                isFavourite = item.isFavourite,
                mealName = item.name,
                onToggle = onToggleFavourite,
            )
        }
    }
}

@Composable
private fun FavouriteToggle(
    isFavourite: Boolean,
    mealName: String,
    onToggle: () -> Unit,
) {
    val label = if (isFavourite) {
        "Remove $mealName from favourites"
    } else {
        "Add $mealName to favourites"
    }
    IconButton(
        onClick = onToggle,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
        },
    ) {
        Icon(
            imageVector = if (isFavourite) TasteIndiaIcons.Favorite else TasteIndiaIcons.FavoriteBorder,
            contentDescription = null,
            tint = if (isFavourite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
