package com.parv.tasteindia.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Meal thumbnail with explicit loading and failure states.
 *
 * The caller always constrains the size (via [modifier]), and every branch fills that same box,
 * so the row layout never collapses when an image is slow or fails — including when [url] is
 * null/blank (Coil routes that straight to the error branch).
 */
@Composable
fun MealImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url?.takeIf { it.isNotBlank() })
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { ImageStatusBox(icon = null) },
        error = { ImageStatusBox(icon = TasteIndiaIcons.BrokenImage) },
    )
}

@Composable
private fun ImageStatusBox(icon: ImageVector?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative; the image itself carries the description
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
