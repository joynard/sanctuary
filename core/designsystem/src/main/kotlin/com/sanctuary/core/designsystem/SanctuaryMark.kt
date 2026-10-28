package com.sanctuary.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Three nested doorways: a refuge, not a progress gauge or a signal icon. Kept as
 * line art so it stays legible at launcher and empty-state sizes.
 */
@Composable
fun SanctuaryMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
) {
    Canvas(modifier = modifier.size(size)) {
        drawCircle(color = backgroundColor, radius = this.size.minDimension / 2f)

        val width = this.size.width
        val height = this.size.height
        val stroke = this.size.minDimension * 0.032f
        val centerX = width * 0.5f
        val bottom = height * 0.80f

        repeat(3) { index ->
            val insetX = width * (0.22f + index * 0.09f)
            val top = height * (0.26f + index * 0.055f)
            val radius = width * 0.16f

            val arch = Path().apply {
                moveTo(insetX, bottom)
                lineTo(insetX, top + radius)
                cubicTo(
                    insetX,
                    top,
                    centerX - radius * 0.65f,
                    top,
                    centerX,
                    top,
                )
                cubicTo(
                    centerX + radius * 0.65f,
                    top,
                    width - insetX,
                    top,
                    width - insetX,
                    top + radius,
                )
                lineTo(width - insetX, bottom)
            }

            drawPath(
                path = arch,
                color = color.copy(alpha = 1f - index * 0.24f),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(width * 0.28f, height * 0.80f),
            end = Offset(width * 0.72f, height * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
