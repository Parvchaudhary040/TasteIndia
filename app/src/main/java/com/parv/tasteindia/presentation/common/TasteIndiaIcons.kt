package com.parv.tasteindia.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of icons the app needs, as inline [ImageVector]s.
 *
 * `androidx.compose.material:material-icons-core` is frozen (last release 1.7.8) and no longer
 * managed by the Compose BOM, so rather than pin a stale, mismatched artifact we carry the six
 * glyphs we actually use. Path data is from the Material Symbols set (Apache License 2.0).
 */
object TasteIndiaIcons {

    val ArrowBack: ImageVector by lazy {
        materialIcon("ArrowBack") {
            materialPath {
                moveTo(20f, 11f); horizontalLineTo(7.83f); lineToRelative(5.59f, -5.59f)
                lineTo(12f, 4f); lineToRelative(-8f, 8f); lineToRelative(8f, 8f)
                lineToRelative(1.41f, -1.41f); lineTo(7.83f, 13f); horizontalLineTo(20f); close()
            }
        }
    }

    val Favorite: ImageVector by lazy {
        materialIcon("Favorite") {
            materialPath {
                moveTo(12f, 21.35f); lineToRelative(-1.45f, -1.32f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveToRelative(1.74f, 0f, 3.41f, 0.81f, 4.5f, 2.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveToRelative(0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                lineTo(12f, 21.35f); close()
            }
        }
    }

    val FavoriteBorder: ImageVector by lazy {
        materialIcon("FavoriteBorder") {
            materialPath {
                moveTo(16.5f, 3f)
                curveToRelative(-1.74f, 0f, -3.41f, 0.81f, -4.5f, 2.09f)
                curveTo(10.91f, 3.81f, 9.24f, 3f, 7.5f, 3f)
                curveTo(4.42f, 3f, 2f, 5.42f, 2f, 8.5f)
                curveToRelative(0f, 3.78f, 3.4f, 6.86f, 8.55f, 11.54f)
                lineTo(12f, 21.35f); lineToRelative(1.45f, -1.32f)
                curveTo(18.6f, 15.36f, 22f, 12.28f, 22f, 8.5f)
                curveTo(22f, 5.42f, 19.58f, 3f, 16.5f, 3f); close()
                moveTo(12.1f, 18.55f); lineToRelative(-0.1f, 0.1f); lineToRelative(-0.1f, -0.1f)
                curveTo(7.14f, 14.24f, 4f, 11.39f, 4f, 8.5f)
                curveTo(4f, 6.5f, 5.5f, 5f, 7.5f, 5f)
                curveToRelative(1.54f, 0f, 3.04f, 0.99f, 3.57f, 2.36f)
                horizontalLineToRelative(1.87f)
                curveTo(13.46f, 5.99f, 14.96f, 5f, 16.5f, 5f)
                curveToRelative(2f, 0f, 3.5f, 1.5f, 3.5f, 3.5f)
                curveToRelative(0f, 2.89f, -3.14f, 5.74f, -7.9f, 10.05f); close()
            }
        }
    }

    val Search: ImageVector by lazy {
        materialIcon("Search") {
            materialPath {
                moveTo(15.5f, 14f); horizontalLineToRelative(-0.79f); lineToRelative(-0.28f, -0.27f)
                curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
                curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
                reflectiveCurveTo(3f, 5.91f, 3f, 9.5f)
                reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
                curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.23f, -1.57f)
                lineToRelative(0.27f, 0.28f); verticalLineToRelative(0.79f); lineToRelative(5f, 4.99f)
                lineTo(20.49f, 19f); lineToRelative(-4.99f, -5f); close()
                moveTo(9.5f, 14f)
                curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
                reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
                reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
                reflectiveCurveTo(11.99f, 14f, 9.5f, 14f); close()
            }
        }
    }

    val Close: ImageVector by lazy {
        materialIcon("Close") {
            materialPath {
                moveTo(19f, 6.41f); lineTo(17.59f, 5f); lineTo(12f, 10.59f); lineTo(6.41f, 5f)
                lineTo(5f, 6.41f); lineTo(10.59f, 12f); lineTo(5f, 17.59f); lineTo(6.41f, 19f)
                lineTo(12f, 13.41f); lineTo(17.59f, 19f); lineTo(19f, 17.59f); lineTo(13.41f, 12f)
                close()
            }
        }
    }

    val FilterList: ImageVector by lazy {
        materialIcon("FilterList") {
            materialPath {
                moveTo(10f, 18f); horizontalLineToRelative(4f); verticalLineToRelative(-2f)
                horizontalLineToRelative(-4f); verticalLineToRelative(2f); close()
                moveTo(3f, 6f); verticalLineToRelative(2f); horizontalLineToRelative(18f)
                verticalLineTo(6f); horizontalLineTo(3f); close()
                moveTo(6f, 13f); horizontalLineToRelative(12f); verticalLineToRelative(-2f)
                horizontalLineTo(6f); verticalLineToRelative(2f); close()
            }
        }
    }

    val OpenInNew: ImageVector by lazy {
        materialIcon("OpenInNew") {
            materialPath {
                moveTo(19f, 19f); horizontalLineTo(5f); verticalLineTo(5f)
                horizontalLineToRelative(7f); verticalLineTo(3f); horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f); verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f); horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f); verticalLineToRelative(-7f)
                horizontalLineToRelative(-2f); verticalLineToRelative(7f); close()
                moveTo(14f, 3f); verticalLineToRelative(2f); horizontalLineToRelative(3.59f)
                lineToRelative(-9.83f, 9.83f); lineToRelative(1.41f, 1.41f)
                lineTo(19f, 6.41f); verticalLineTo(10f); horizontalLineToRelative(2f)
                verticalLineTo(3f); horizontalLineToRelative(-7f); close()
            }
        }
    }

    val BrokenImage: ImageVector by lazy {
        materialIcon("BrokenImage") {
            materialPath {
                moveTo(21f, 5f); verticalLineToRelative(6.59f); lineToRelative(-3f, -3.01f)
                lineToRelative(-4f, 4.01f); lineToRelative(-4f, -4f); lineToRelative(-4f, 4f)
                lineToRelative(-3f, -3.01f); verticalLineTo(5f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f); horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f); close()
                moveTo(18f, 11.42f); lineToRelative(3f, 3.01f); verticalLineTo(19f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f); horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f); verticalLineToRelative(-6.58f)
                lineToRelative(3f, 2.99f); lineToRelative(4f, -4f); lineToRelative(4f, 4f)
                lineToRelative(4f, -3.99f); close()
            }
        }
    }

    val PlayCircle: ImageVector by lazy {
        materialIcon("PlayCircle") {
            materialPath {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
                moveTo(10f, 16.5f); verticalLineToRelative(-9f); lineToRelative(6f, 4.5f)
                lineToRelative(-6f, 4.5f); close()
            }
        }
    }
}

private inline fun materialIcon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = "TasteIndiaIcons.$name",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()

private inline fun ImageVector.Builder.materialPath(
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) = path(
    fill = SolidColor(Color.Black),
    pathFillType = PathFillType.NonZero,
    pathBuilder = pathBuilder,
)
