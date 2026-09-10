package com.parv.tasteindia.presentation.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Opens external links (source / video) safely:
 *  - only `http(s)` URLs are ever acted on ([asWebUrlOrNull] also gates the UI so a bad link
 *    simply shows no button);
 *  - `Uri.parse` never throws, and the `startActivity` is wrapped, so a malformed or
 *    unhandleable link shows a toast instead of crashing.
 */
@Composable
fun rememberOpenExternalUrl(): (String) -> Unit {
    val context = LocalContext.current
    return { url -> openExternalUrl(context, url) }
}

fun openExternalUrl(context: Context, url: String) {
    val uri = url.asWebUrlOrNull()
    if (uri == null) {
        Toast.makeText(context, "That link isn’t valid", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}

/** Trimmed `http`/`https` [Uri] with a non-blank host, or `null`. */
fun String.asWebUrlOrNull(): Uri? {
    val candidate = trim().takeIf { it.isNotEmpty() } ?: return null
    val uri = runCatching { candidate.toUri() }.getOrNull() ?: return null
    return uri.takeIf {
        it.scheme?.lowercase() in setOf("http", "https") && !it.host.isNullOrBlank()
    }
}

/** Human-readable host for a link label, e.g. "youtube.com" — never the full raw URL. */
fun String.linkHostOrNull(): String? =
    asWebUrlOrNull()?.host?.removePrefix("www.")
