package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Original abstract geometric "T" prism mark for TAVIQO.
 * Uses pure geometric stage rings and a luminous faceted T without animal imagery.
 */
@Composable
fun TaviqoLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    animated: Boolean = true
) {
    val glowAlpha = if (animated) {
        val transition = rememberInfiniteTransition(label = "taviqo_glow")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
        value
    } else {
        0.75f
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)

        // Subtle luminous stage aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = 0.28f * glowAlpha),
                    RoyalBlueElevated.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = w * 0.52f
            ),
            radius = w * 0.5f,
            center = center
        )

        // Outer orbital stage ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    NeonCyan.copy(alpha = glowAlpha),
                    RoyalBlueBorder,
                    ElectricCyan.copy(alpha = 0.85f),
                    NeonCyan.copy(alpha = glowAlpha)
                )
            ),
            radius = w * 0.43f,
            center = center,
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
        )

        // Stylized faceted geometric "T" crossbar (angled prism wings)
        val topBar = Path().apply {
            moveTo(w * 0.23f, h * 0.30f)
            lineTo(w * 0.77f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.42f)
            lineTo(w * 0.30f, h * 0.42f)
            close()
        }
        drawPath(
            path = topBar,
            brush = Brush.horizontalGradient(
                colors = listOf(RoyalBlueBorder, NeonCyan, ElectricCyan)
            )
        )

        // Stylized "T" vertical beam (tapered prism column)
        val stem = Path().apply {
            moveTo(w * 0.43f, h * 0.38f)
            lineTo(w * 0.57f, h * 0.38f)
            lineTo(w * 0.54f, h * 0.74f)
            lineTo(w * 0.46f, h * 0.80f)
            close()
        }
        drawPath(
            path = stem,
            brush = Brush.verticalGradient(
                colors = listOf(NeonCyan, ElectricCyan, RoyalBlueElevated)
            )
        )
    }
}

@Composable
fun TaviqoBrandLockup(
    modifier: Modifier = Modifier,
    showSlogan: Boolean = false,
    logoSize: Dp = 36.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaviqoLogoMark(size = logoSize)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "TAVIQO",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
            if (showSlogan) {
                Text(
                    text = "El mundo es tu escenario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
