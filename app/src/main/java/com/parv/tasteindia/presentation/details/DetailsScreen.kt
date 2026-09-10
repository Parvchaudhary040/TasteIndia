package com.parv.tasteindia.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parv.tasteindia.domain.model.Ingredient
import com.parv.tasteindia.domain.model.MealDetail
import com.parv.tasteindia.presentation.common.ErrorState
import com.parv.tasteindia.presentation.common.LoadingState
import com.parv.tasteindia.presentation.common.MealImage
import com.parv.tasteindia.presentation.common.TasteIndiaIcons
import com.parv.tasteindia.presentation.common.asWebUrlOrNull
import com.parv.tasteindia.presentation.common.linkHostOrNull
import com.parv.tasteindia.presentation.common.rememberOpenExternalUrl

/**
 * Full recipe. Reached with only a meal id; every field is rendered only when it has meaningful
 * content (blank API values were already nulled during mapping).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mealId: String,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = viewModel(
        key = "details/$mealId",
        factory = DetailsViewModel.factory(mealId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.detail?.name ?: "Recipe",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TasteIndiaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.status == DetailsUiState.LoadStatus.Success) {
                        FavouriteAction(
                            isFavourite = state.isFavourite,
                            mealName = state.detail?.name.orEmpty(),
                            onToggle = viewModel::toggleFavourite,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state.status) {
            DetailsUiState.LoadStatus.Loading ->
                LoadingState(Modifier.fillMaxSize().padding(innerPadding))

            DetailsUiState.LoadStatus.Error ->
                ErrorState(
                    error = state.error,
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )

            DetailsUiState.LoadStatus.Success ->
                state.detail?.let { detail ->
                    DetailsContent(detail = detail, modifier = Modifier.padding(innerPadding))
                }
        }
    }
}

@Composable
private fun FavouriteAction(isFavourite: Boolean, mealName: String, onToggle: () -> Unit) {
    val label = if (isFavourite) "Remove $mealName from favourites" else "Add $mealName to favourites"
    IconButton(
        onClick = onToggle,
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
    ) {
        Icon(
            imageVector = if (isFavourite) TasteIndiaIcons.Favorite else TasteIndiaIcons.FavoriteBorder,
            contentDescription = null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(detail: MealDetail, modifier: Modifier = Modifier) {
    val openUrl = rememberOpenExternalUrl()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MealImage(
            url = detail.thumbnailUrl,
            contentDescription = "Photo of ${detail.name}",
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(detail.name, style = MaterialTheme.typography.headlineSmall)

            val classifiers = listOfNotNull(detail.category, detail.area)
            if (classifiers.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.category?.let { AssistChip(onClick = {}, enabled = false, label = { Text(it) }) }
                    detail.area?.let { AssistChip(onClick = {}, enabled = false, label = { Text(it) }) }
                }
            }

            if (detail.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.tags.forEach { tag ->
                        SuggestionChip(onClick = {}, enabled = false, label = { Text(tag) })
                    }
                }
            }

            val links = buildList {
                detail.youtubeUrl?.asWebUrlOrNull()?.let { add(LinkButton.Video(detail.youtubeUrl)) }
                detail.sourceUrl?.asWebUrlOrNull()?.let { add(LinkButton.Source(detail.sourceUrl)) }
            }
            if (links.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEach { link ->
                        when (link) {
                            is LinkButton.Video -> ExternalLinkButton(
                                text = "Watch video",
                                accessibilityLabel = "Watch ${detail.name} on ${link.url.linkHostOrNull() ?: "video"}",
                                icon = TasteIndiaIcons.PlayCircle,
                                onClick = { openUrl(link.url) },
                            )
                            is LinkButton.Source -> ExternalLinkButton(
                                text = "View full recipe",
                                accessibilityLabel = "View the full recipe on ${link.url.linkHostOrNull() ?: "the source site"}",
                                icon = TasteIndiaIcons.OpenInNew,
                                onClick = { openUrl(link.url) },
                            )
                        }
                    }
                }
            }

            if (detail.ingredients.isNotEmpty()) {
                HorizontalDivider()
                SectionTitle("Ingredients")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.ingredients.forEach { IngredientRow(it) }
                }
            }

            if (!detail.instructions.isNullOrBlank()) {
                HorizontalDivider()
                SectionTitle("Instructions")
                Text(
                    text = detail.instructions,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private sealed interface LinkButton {
    val url: String
    data class Video(override val url: String) : LinkButton
    data class Source(override val url: String) : LinkButton
}

@Composable
private fun ExternalLinkButton(
    text: String,
    accessibilityLabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.clearAndSetSemantics { contentDescription = accessibilityLabel },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(text)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun IngredientRow(ingredient: Ingredient) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = if (ingredient.measure.isNotEmpty()) {
                    "${ingredient.measure} ${ingredient.name}"
                } else {
                    ingredient.name
                }
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = ingredient.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (ingredient.measure.isNotEmpty()) {
            Text(
                text = ingredient.measure,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
