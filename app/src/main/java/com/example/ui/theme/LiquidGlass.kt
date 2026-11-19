package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Derives a proportional inner corner radius from the user's unified bar corner radius.
 */
fun unifiedInnerRadius(unifiedRadius: Dp): Dp = (unifiedRadius - 4.dp).coerceAtLeast(4.dp)

/**
 * Reusable Liquid Glass surface composable.
 * [glassOpacity] controls the vertical specular sheen intensity (`0.05f..0.45f`) paired
 * with a frosted dark tint so foreground icons & URL text stay legible over bright webpages.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    shadowElevation: Dp = 18.dp,
    glassOpacity: Float = 0.16f,
    isHighlighted: Boolean = false,
    accentColor: Color = IceCyan,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val darkMode = isSystemInDarkTheme()
    val clampedSheen = glassOpacity.coerceIn(0.05f, 0.45f)
    val baseAlpha = (0.58f + clampedSheen * 0.72f).coerceIn(0.60f, 0.92f)

    val baseTint = remember(darkMode, baseAlpha) {
        if (darkMode) {
            Color(0xFF0A101C).copy(alpha = baseAlpha)
        } else {
            Color(0xFF101827).copy(alpha = baseAlpha)
        }
    }

    val topRimColorState = animateColorAsState(
        targetValue = if (isHighlighted) {
            accentColor.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = (0.28f + clampedSheen * 0.85f).coerceAtMost(0.75f))
        },
        animationSpec = tween(durationMillis = 220),
        label = "liquid_glass_top_rim",
    )

    val midRimColorState = animateColorAsState(
        targetValue = if (isHighlighted) {
            accentColor.copy(alpha = 0.34f)
        } else {
            Color.White.copy(alpha = (0.10f + clampedSheen * 0.35f).coerceAtMost(0.32f))
        },
        animationSpec = tween(durationMillis = 220),
        label = "liquid_glass_mid_rim",
    )

    val specularBrush = remember(clampedSheen) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = clampedSheen),
                0.48f to Color.White.copy(alpha = clampedSheen * 0.48f),
                1.0f to Color.White.copy(alpha = clampedSheen * 0.26f),
            )
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        // Dedicated glass surface layer kept separate from foreground icons.
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.50f),
                    spotColor = Color.Black.copy(alpha = 0.65f),
                )
                .clip(shape)
                .background(baseTint)
                .background(specularBrush)
                .drawWithCache {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val borderStroke = Stroke(width = 0.5.dp.toPx())
                    val bottomRim = Color.White.copy(alpha = 0.06f)
                    onDrawWithContent {
                        drawContent()
                        val rimBrush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to topRimColorState.value,
                                0.45f to midRimColorState.value,
                                1.0f to bottomRim,
                            ),
                            startY = 0f,
                            endY = size.height,
                        )
                        drawOutline(
                            outline = outline,
                            brush = rimBrush,
                            style = borderStroke,
                        )
                    }
                }
        )

        // Razor-sharp foreground layer.
        content()
    }
}

/**
 * 360-degree rounded-rectangle perimeter loading indicator using [drawWithCache] and
 * lambda-based state reads ([progressProvider], [alphaProvider]).
 * Rebuilds the rounded-rect perimeter [Path] & [PathMeasure] ONLY when layout size or
 * [cornerRadius] changes, eliminating allocation & recomposition overhead on progress ticks.
 */
@Composable
fun PerimeterLoadingRing(
    cornerRadius: Dp,
    progressProvider: () -> Float,
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawWithCache {
            val strokePx = 2.dp.toPx()
            val inset = strokePx / 2f
            val left = inset
            val top = inset
            val right = (size.width - inset).coerceAtLeast(left + 1f)
            val bottom = (size.height - inset).coerceAtLeast(top + 1f)
            val w = right - left
            val h = bottom - top
            val r = minOf(cornerRadius.toPx() - inset, w / 2f, h / 2f).coerceAtLeast(0f)
            val d = r * 2f

            val fullPath = Path().apply {
                moveTo(left + w / 2f, top)
                lineTo(right - r, top)
                if (r > 0f) {
                    arcTo(Rect(right - d, top, right, top + d), -90f, 90f, false)
                }
                lineTo(right, bottom - r)
                if (r > 0f) {
                    arcTo(Rect(right - d, bottom - d, right, bottom), 0f, 90f, false)
                }
                lineTo(left + r, bottom)
                if (r > 0f) {
                    arcTo(Rect(left, bottom - d, left + d, bottom), 90f, 90f, false)
                }
                lineTo(left, top + r)
                if (r > 0f) {
                    arcTo(Rect(left, top, left + d, top + d), 180f, 90f, false)
                }
                lineTo(left + w / 2f, top)
            }

            val pathMeasure = PathMeasure().apply {
                setPath(fullPath, forceClosed = false)
            }
            val totalLength = pathMeasure.length
            val segmentPath = Path()
            val sweepBrush = Brush.sweepGradient(
                colors = listOf(
                    IceCyan.copy(alpha = 0.75f),
                    IceCyan,
                    PureFrost,
                    IceCyan,
                ),
                center = Offset(size.width / 2f, size.height / 2f),
            )
            val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)

            onDrawBehind {
                val ringAlpha = alphaProvider().coerceIn(0f, 1f)
                if (ringAlpha <= 0.01f || totalLength <= 0f) return@onDrawBehind

                val fraction = progressProvider().coerceIn(0f, 1f)
                val stopDistance = (totalLength * fraction).coerceIn(0f, totalLength)
                if (stopDistance > 0f) {
                    segmentPath.reset()
                    pathMeasure.getSegment(0f, stopDistance, segmentPath, startWithMoveTo = true)
                    drawPath(
                        path = segmentPath,
                        brush = sweepBrush,
                        alpha = ringAlpha,
                        style = strokeStyle,
                    )
                }
            }
        }
    )
}
