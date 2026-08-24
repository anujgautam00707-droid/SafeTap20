package com.safetap.app.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.ui.components.EmergencyPulseButton
import com.safetap.app.ui.components.QuickActionCard
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyRedDark
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.SafeGreenLight
import com.safetap.app.ui.theme.WarningAmber
import com.safetap.app.ui.theme.WarningAmberContainer

data class ActivityItem(
    val title: String,
    val description: String,
    val timeAgo: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val iconBgColor: Color
)

@Composable
fun HomeScreen(
    onOpenSos: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = SafeTapViewModelFactory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // State for interactive modals
    var showEmergencyCallDialog by remember { mutableStateOf(false) }
    var showFakeCallModal by remember { mutableStateOf(false) }
    var showLiveLocationDialog by remember { mutableStateOf(false) }
    var fakeCallCountdown by remember { mutableStateOf(false) }

    val recentActivities = remember {
        listOf(
            ActivityItem(
                title = "SafeTap Protected",
                description = "Background protection active & ready",
                timeAgo = "Just now",
                icon = Icons.Filled.Shield,
                iconColor = SafeGreen,
                iconBgColor = SafeGreenContainer
            ),
            ActivityItem(
                title = "Location Synchronized",
                description = "GPS lock established with 5m accuracy",
                timeAgo = "12m ago",
                icon = Icons.Outlined.MyLocation,
                iconColor = Color(0xFF1976D2),
                iconBgColor = Color(0xFFE3F2FD)
            ),
            ActivityItem(
                title = "3 Contacts Linked",
                description = "Emergency SMS broadcast ready",
                timeAgo = "1h ago",
                icon = Icons.Filled.People,
                iconColor = Color(0xFF7B1FA2),
                iconBgColor = Color(0xFFF3E5F5)
            )
        )
    }

    // Emergency Call Dialog
    if (showEmergencyCallDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyCallDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Emergency Call",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Do you want to dial emergency dispatch (112) immediately?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyCallDialog = false
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = Uri.parse("tel:112")
                        }
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CALL_PHONE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            context.startActivity(callIntent)
                        } else {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:112")
                            }
                            context.startActivity(dialIntent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Call Emergency (112)", color = EmergencyWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyCallDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Live Location Modal
    if (showLiveLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLiveLocationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Live Location Sharing", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Your current location is continuously monitored for rapid SOS dispatch.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High Precision GPS Active",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLiveLocationDialog = false
                        onOpenSos()
                    }
                ) {
                    Text("Open SOS Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveLocationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Fake Call Simulation Dialog
    if (showFakeCallModal) {
        AlertDialog(
            onDismissRequest = {
                showFakeCallModal = false
                fakeCallCountdown = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.PhoneInTalk,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (fakeCallCountdown) "Incoming Call in 5s..." else "Fake Call Disguise",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (fakeCallCountdown)
                        "Stay calm. Your phone will ring with a simulated emergency escape call shortly."
                    else
                        "Trigger a realistic incoming call to help you safely excuse yourself from uncomfortable situations."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        fakeCallCountdown = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text(if (fakeCallCountdown) "Ringing..." else "Trigger Fake Call", color = EmergencyWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFakeCallModal = false
                    fakeCallCountdown = false
                }) {
                    Text("Dismiss")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Welcome & Profile Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hello, ${uiState.userDisplayName} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You are protected by SafeTap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Security Status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "SafeTap Ready" Status Card
        StatusReadyBanner()

        Spacer(modifier = Modifier.height(24.dp))

        // Main Animated Large SOS Button
        Text(
            text = "EMERGENCY ASSISTANCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        EmergencyPulseButton(
            onClick = onOpenSos,
            title = "SEND SOS",
            subtitle = "TAP FOR EMERGENCY",
            size = 175.dp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Instant Access",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 Quick Action Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Emergency Call",
                subtitle = "Dial 112",
                icon = Icons.Filled.Call,
                iconTint = EmergencyRed,
                iconBackgroundColor = EmergencyRedContainer,
                onClick = { showEmergencyCallDialog = true },
                modifier = Modifier.weight(1f),
                badgeText = "Fast"
            )

            QuickActionCard(
                title = "Trusted Contacts",
                subtitle = "Manage allies",
                icon = Icons.Filled.People,
                iconTint = Color(0xFF7B1FA2),
                iconBackgroundColor = Color(0xFFF3E5F5),
                onClick = onNavigateToContacts,
                modifier = Modifier.weight(1f),
                badgeText = "3 Linked"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Live Location",
                subtitle = "GPS Tracking",
                icon = Icons.Filled.LocationOn,
                iconTint = SafeGreen,
                iconBackgroundColor = SafeGreenContainer,
                onClick = { showLiveLocationDialog = true },
                modifier = Modifier.weight(1f),
                badgeText = "Active"
            )

            QuickActionCard(
                title = "Fake Call",
                subtitle = "Safety disguise",
                icon = Icons.Filled.PhoneInTalk,
                iconTint = WarningAmber,
                iconBackgroundColor = WarningAmberContainer,
                onClick = { showFakeCallModal = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Activity List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                recentActivities.forEachIndexed { index, activity ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(activity.iconBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activity.icon,
                                contentDescription = null,
                                tint = activity.iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activity.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = activity.timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (index < recentActivities.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusReadyBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SafeGreenContainer
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
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp)
            ) {
                // Pulsing outer dot
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .scale(glowScale)
                        .background(SafeGreenLight.copy(alpha = 0.4f), CircleShape)
                )
                // Center solid badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(SafeGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(EmergencyWhite, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SafeTap Ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SafeGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ONLINE",
                            color = SafeGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "GPS High Accuracy • 3 Contacts Synced",
                    style = MaterialTheme.typography.bodySmall,
                    color = SafeGreen.copy(alpha = 0.85f)
                )
            }
        }
    }
}
