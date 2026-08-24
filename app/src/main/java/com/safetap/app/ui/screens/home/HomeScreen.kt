package com.safetap.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.R
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.ui.components.EmergencyPulseButton
import com.safetap.app.ui.components.QuickActionCard
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.SafeGreenLight
import com.safetap.app.ui.theme.WarningAmber
import com.safetap.app.ui.theme.WarningAmberContainer

data class ActivityItem(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val timeAgoRes: Int,
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
                titleRes = R.string.home_act_safetap_protected,
                descriptionRes = R.string.home_act_safetap_protected_desc,
                timeAgoRes = R.string.home_act_just_now,
                icon = Icons.Filled.Shield,
                iconColor = SafeGreen,
                iconBgColor = SafeGreenContainer
            ),
            ActivityItem(
                titleRes = R.string.home_act_location_synced,
                descriptionRes = R.string.home_act_location_synced_desc,
                timeAgoRes = R.string.home_act_12m_ago,
                icon = Icons.Outlined.MyLocation,
                iconColor = Color(0xFF1976D2),
                iconBgColor = Color(0xFFE3F2FD)
            ),
            ActivityItem(
                titleRes = R.string.home_act_contacts_linked,
                descriptionRes = R.string.home_act_contacts_linked_desc,
                timeAgoRes = R.string.home_act_1h_ago,
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
                    text = stringResource(R.string.home_dialog_emergency_call_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.home_dialog_emergency_call_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyCallDialog = false
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:911")
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text(stringResource(R.string.home_dialog_call_911), color = EmergencyWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyCallDialog = false }) {
                    Text(stringResource(R.string.cancel))
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
                Text(stringResource(R.string.home_dialog_live_location_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(stringResource(R.string.home_dialog_live_location_desc))
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
                                text = stringResource(R.string.home_dialog_gps_active),
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
                    Text(stringResource(R.string.home_dialog_open_sos))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveLocationDialog = false }) {
                    Text(stringResource(R.string.close))
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
                    text = stringResource(
                        if (fakeCallCountdown) R.string.home_dialog_fake_call_countdown_title
                        else R.string.home_dialog_fake_call_title
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(
                        if (fakeCallCountdown) R.string.home_dialog_fake_call_countdown_desc
                        else R.string.home_dialog_fake_call_desc
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        fakeCallCountdown = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                ) {
                    Text(
                        stringResource(
                            if (fakeCallCountdown) R.string.home_dialog_ringing
                            else R.string.home_dialog_trigger_fake_call
                        ),
                        color = EmergencyWhite
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFakeCallModal = false
                    fakeCallCountdown = false
                }) {
                    Text(stringResource(R.string.dismiss))
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
                    text = stringResource(R.string.home_greeting, uiState.userDisplayName),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_protected_subtitle),
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
            text = stringResource(R.string.home_emergency_assistance),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        EmergencyPulseButton(
            onClick = onOpenSos,
            title = stringResource(R.string.home_send_sos),
            subtitle = stringResource(R.string.home_tap_for_emergency),
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
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.home_instant_access),
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
                title = stringResource(R.string.home_qa_emergency_call),
                subtitle = stringResource(R.string.home_qa_emergency_call_sub),
                icon = Icons.Filled.Call,
                iconTint = EmergencyRed,
                iconBackgroundColor = EmergencyRedContainer,
                onClick = { showEmergencyCallDialog = true },
                modifier = Modifier.weight(1f),
                badgeText = stringResource(R.string.home_qa_badge_fast)
            )

            QuickActionCard(
                title = stringResource(R.string.home_qa_trusted_contacts),
                subtitle = stringResource(R.string.home_qa_trusted_contacts_sub),
                icon = Icons.Filled.People,
                iconTint = Color(0xFF7B1FA2),
                iconBackgroundColor = Color(0xFFF3E5F5),
                onClick = onNavigateToContacts,
                modifier = Modifier.weight(1f),
                badgeText = stringResource(R.string.home_qa_badge_linked)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = stringResource(R.string.home_qa_live_location),
                subtitle = stringResource(R.string.home_qa_live_location_sub),
                icon = Icons.Filled.LocationOn,
                iconTint = SafeGreen,
                iconBackgroundColor = SafeGreenContainer,
                onClick = { showLiveLocationDialog = true },
                modifier = Modifier.weight(1f),
                badgeText = stringResource(R.string.home_qa_badge_active)
            )

            QuickActionCard(
                title = stringResource(R.string.home_qa_fake_call),
                subtitle = stringResource(R.string.home_qa_fake_call_sub),
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
                    text = stringResource(R.string.home_recent_activity),
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
                                text = stringResource(activity.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(activity.descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(activity.timeAgoRes),
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
                        text = stringResource(R.string.home_status_ready),
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
                            text = stringResource(R.string.home_status_online),
                            color = SafeGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_status_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = SafeGreen.copy(alpha = 0.85f)
                )
            }
        }
    }
}
