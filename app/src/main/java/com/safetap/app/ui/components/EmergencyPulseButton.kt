package com.safetap.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedDark
import com.safetap.app.ui.theme.EmergencyRedLight
import com.safetap.app.ui.theme.EmergencyWhite

@Composable
fun EmergencyPulseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    title: String = "SEND SOS",
    subtitle: String = "TAP FOR HELP",
    icon: ImageVector = Icons.Filled.Warning,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.45f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.35f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha2"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 1.35f)
    ) {
        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale1)
                .background(
                    color = EmergencyRedLight.copy(alpha = pulseAlpha1),
                    shape = CircleShape
                )
        )

        // Middle pulsing ring 2
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale2)
                .background(
                    color = EmergencyRed.copy(alpha = pulseAlpha2),
                    shape = CircleShape
                )
        )

        // Main tactile button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = EmergencyRedDark.copy(alpha = 0.5f),
                    spotColor = EmergencyRed.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            EmergencyRedLight,
                            EmergencyRed,
                            EmergencyRedDark
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = EmergencyWhite),
                    onClick = onClick
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = EmergencyWhite,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = EmergencyWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 1.2.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = EmergencyWhite.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
