package com.parv.tasteindia.presentation.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parv.tasteindia.presentation.common.TasteIndiaIcons

/**
 * One-screen welcome / landing page. Sets expectations for what the app does, then leads into
 * the recipe list. Deliberately calm — one primary action.
 */
@Composable
fun WelcomeScreen(onExplore: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TasteIndiaIcons.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.size(24.dp))

            Text(
                text = "Welcome to TasteIndia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(12.dp))

            Text(
                text = "Browse a hand-picked collection of Indian recipes — search, filter, and " +
                    "keep your favourites for later.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 360.dp),
            )

            Spacer(Modifier.size(36.dp))

            Column(
                modifier = Modifier.widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureRow(
                    icon = TasteIndiaIcons.Search,
                    title = "Search instantly",
                    body = "Find any recipe by name as you type.",
                )
                FeatureRow(
                    icon = TasteIndiaIcons.FilterList,
                    title = "Filter & sort",
                    body = "Narrow by category or main ingredient, sort A–Z or Z–A.",
                )
                FeatureRow(
                    icon = TasteIndiaIcons.Favorite,
                    title = "Save favourites",
                    body = "Tap the heart — your favourites stay saved, even offline.",
                )
            }

            Spacer(Modifier.size(40.dp))

            Button(
                onClick = onExplore,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text("Explore recipes", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
