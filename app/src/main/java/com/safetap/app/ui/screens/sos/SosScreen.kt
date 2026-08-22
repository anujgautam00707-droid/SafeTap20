package com.safetap.app.ui.screens.sos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.ui.theme.DarkBackground
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyRedDark
import com.safetap.app.ui.theme.EmergencyRedLight
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.WarningAmber
import com.safetap.app.ui.theme.WarningAmberContainer
import kotlinx.coroutines.delay

enum class SosState {
    IDLE,
    COUNTDOWN,
    TRIGGERED
}

@Composable
fun SosScreen(
    viewModel: SosViewModel = viewModel(factory = SafeTapViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var sosState by remember { mutableStateOf(SosState.IDLE) }
    var countdownSeconds by remember { mutableIntStateOf(5) }

    // 5-second countdown timer animation
    LaunchedEffect(sosState) {
        if (sosState == SosState.COUNTDOWN) {
            countdownSeconds = 5
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds -= 1
            }
            sosState = SosState.TRIGGERED
            viewModel.onSosPressed()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SosEmergencyPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = when (sosState) {
                        SosState.TRIGGERED -> listOf(
                            Color(0xFF3B0000),
                            Color(0xFF1E0505),
                            MaterialTheme.colorScheme.background
                        )
                        SosState.COUNTDOWN -> listOf(
                            Color(0xFF2E1005),
                            Color(0xFF1E0A05),
                            MaterialTheme.colorScheme.background
                        )
                        SosState.IDLE -> listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Title & Status Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Emergency SOS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when (sosState) {
                        SosState.TRIGGERED -> "EMERGENCY BROADCAST ACTIVE"
                        SosState.COUNTDOWN -> "Dispatching SOS in ${countdownSeconds}s"
                        SosState.IDLE -> "Tap button to start 5-second countdown"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (sosState) {
                        SosState.TRIGGERED -> EmergencyRedLight
                        SosState.COUNTDOWN -> WarningAmber
                        SosState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (sosState) {
                            SosState.TRIGGERED -> EmergencyRed
                            SosState.COUNTDOWN -> WarningAmber
                            SosState.IDLE -> SafeGreen
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = when (sosState) {
                        SosState.TRIGGERED -> "ALERTING"
                        SosState.COUNTDOWN -> "COUNTING"
                        SosState.IDLE -> "ARMED"
                    },
                    color = EmergencyWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Centerpiece SOS Button with 5s Countdown Visual Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            if (sosState != SosState.IDLE) {
                // Pulsing outer warning wave
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(pulseScale)
                        .background(
                            color = (if (sosState == SosState.TRIGGERED) EmergencyRed else WarningAmber).copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
            }

            // Countdown / Progress Ring
            if (sosState == SosState.COUNTDOWN) {
                val animatedProgress by animateFloatAsState(
                    targetValue = countdownSeconds / 5f,
                    animationSpec = tween(durationMillis = 900, easing = LinearEasing),
                    label = "CountdownArc"
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(220.dp),
                    color = WarningAmber,
                    strokeWidth = 8.dp,
                    trackColor = WarningAmber.copy(alpha = 0.2f),
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
                )
            } else if (sosState == SosState.TRIGGERED) {
                CircularProgressIndicator(
                    modifier = Modifier.size(220.dp),
                    color = EmergencyRedLight,
                    strokeWidth = 8.dp,
                    trackColor = EmergencyRedDark
                )
            }

            // Central Touch Target Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(175.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = EmergencyRedDark,
                        spotColor = EmergencyRed
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when (sosState) {
                                SosState.TRIGGERED -> listOf(
                                    EmergencyRedLight,
                                    EmergencyRed,
                                    EmergencyRedDark
                                )
                                SosState.COUNTDOWN -> listOf(
                                    Color(0xFFFFB74D),
                                    WarningAmber,
                                    Color(0xFFE65100)
                                )
                                SosState.IDLE -> listOf(
                                    EmergencyRedLight,
                                    EmergencyRed,
                                    EmergencyRedDark
                                )
                            }
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = EmergencyWhite),
                        onClick = {
                            if (sosState == SosState.IDLE) {
                                sosState = SosState.COUNTDOWN
                            } else if (sosState == SosState.COUNTDOWN) {
                                sosState = SosState.TRIGGERED
                                viewModel.onSosPressed()
                            }
                        }
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (sosState) {
                        SosState.COUNTDOWN -> {
                            Text(
                                text = "${countdownSeconds}s",
                                color = EmergencyWhite,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "TAP TO TRIGGER NOW",
                                color = EmergencyWhite.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        SosState.TRIGGERED -> {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = EmergencyWhite,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ACTIVE",
                                color = EmergencyWhite,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                        SosState.IDLE -> {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = EmergencyWhite,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SOS",
                                color = EmergencyWhite,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "START 5S TIMER",
                                color = EmergencyWhite.copy(alpha = 0.85f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cancel Button (Always Visible & Prominent)
        if (sosState != SosState.IDLE) {
            Button(
                onClick = {
                    sosState = SosState.IDLE
                    countdownSeconds = 5
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel Alert / I Am Safe",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            OutlinedButton(
                onClick = {
                    sosState = SosState.COUNTDOWN
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Test 5-Second SOS Countdown",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Cards Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emergency Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (sosState == SosState.TRIGGERED)
                        EmergencyRedContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (sosState == SosState.TRIGGERED) EmergencyRed else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (sosState == SosState.TRIGGERED) Icons.Filled.NotificationsActive else Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (sosState == SosState.TRIGGERED) EmergencyWhite else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (sosState == SosState.TRIGGERED) "Emergency Broadcast Active" else "Emergency Dispatch Standby",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (sosState == SosState.TRIGGERED) EmergencyRed else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (sosState == SosState.TRIGGERED)
                                "Alerting 3 trusted contacts with live audio & location"
                            else
                                "3 contacts will be alerted immediately on trigger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // GPS Status Indicator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SafeGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GPS Location Locked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SafeGreenContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HIGH PRECISION",
                                    color = SafeGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "37.7749° N, 122.4194° W • Accuracy ±3.5m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Battery Percentage Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Status: 88%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Power Optimized • 14h standby remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
