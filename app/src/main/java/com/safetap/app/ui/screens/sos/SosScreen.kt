package com.safetap.app.ui.screens.sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.domain.sos.model.LocationTrackingState
import com.safetap.app.domain.sos.services.SmsDeliveryState
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyRedDark
import com.safetap.app.ui.theme.EmergencyRedLight
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.WarningAmber

@Composable
fun SosScreen(
    viewModel: SosViewModel = viewModel(factory = SafeTapViewModelFactory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batteryPercentage by viewModel.batteryPercentage.collectAsStateWithLifecycle()
    val smsStatuses by viewModel.smsDeliveryStatuses.collectAsStateWithLifecycle()

    val trackingState by (viewModel.liveTrackingState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(LocationTrackingState.Idle) })

    var showPermissionRationale by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.CALL_PHONE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val smsGranted = results[Manifest.permission.SEND_SMS] == true
        val callGranted = results[Manifest.permission.CALL_PHONE] == true

        if ((fineLocationGranted || coarseLocationGranted) && smsGranted && callGranted) {
            viewModel.startSos()
        } else {
            showPermissionRationale = true
        }
    }

    fun hasAllRequiredPermissions(): Boolean {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCall = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return hasLocation && hasSms && hasCall
    }

    fun handleSosInitiation(immediate: Boolean = false) {
        if (hasAllRequiredPermissions()) {
            if (immediate) {
                viewModel.triggerImmediately()
            } else {
                viewModel.startSos()
            }
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Emergency Permissions Required",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SafeTap needs the following permissions to protect you during an SOS:")
                    Text("• Location: To broadcast your live GPS tracking to emergency contacts.")
                    Text("• SMS: To dispatch the immediate SOS alert with your live map link.")
                    Text("• Phone: To auto-dial your designated primary trusted contact.")
                    Text("• Notifications: To run the background live location tracking service.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    }
                ) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isCheckingPermissions = uiState == SosUiState.CheckingPermissions
    val isCountdown = uiState is SosUiState.Countdown
    val isEmergencyActive = uiState == SosUiState.CollectingEmergencyData ||
            uiState is SosUiState.ReadyToSend ||
            uiState is SosUiState.Active

    val usesCountdownVisuals = isCheckingPermissions || isCountdown
    val isSosInProgress = usesCountdownVisuals || isEmergencyActive
    val countdownSeconds = (uiState as? SosUiState.Countdown)?.secondsRemaining ?: 5

    val infiniteTransition = rememberInfiniteTransition(label = "SosEmergencyPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SosAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = when {
                        isEmergencyActive -> listOf(
                            Color(0xFF3B0000),
                            Color(0xFF1E0505),
                            MaterialTheme.colorScheme.background
                        )
                        usesCountdownVisuals -> listOf(
                            Color(0xFF2E1005),
                            Color(0xFF1E0A05),
                            MaterialTheme.colorScheme.background
                        )
                        else -> listOf(
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
        // Header
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
                    text = when (val state = uiState) {
                        SosUiState.Idle -> "Tap button to start 5-second countdown"
                        SosUiState.CheckingPermissions -> "Checking emergency permissions"
                        is SosUiState.Countdown -> "Dispatching SOS in ${state.secondsRemaining}s"
                        SosUiState.CollectingEmergencyData -> "Acquiring GPS & starting live stream"
                        is SosUiState.ReadyToSend -> "Emergency data ready"
                        is SosUiState.Active -> "EMERGENCY BROADCAST ACTIVE"
                        SosUiState.Cancelled -> "SOS alert cancelled"
                        is SosUiState.Error -> state.message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (uiState) {
                        SosUiState.CheckingPermissions,
                        is SosUiState.Countdown -> WarningAmber
                        SosUiState.CollectingEmergencyData,
                        is SosUiState.ReadyToSend,
                        is SosUiState.Active,
                        is SosUiState.Error -> EmergencyRedLight
                        SosUiState.Idle,
                        SosUiState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (uiState) {
                            SosUiState.CheckingPermissions,
                            is SosUiState.Countdown -> WarningAmber
                            SosUiState.CollectingEmergencyData,
                            is SosUiState.ReadyToSend,
                            is SosUiState.Active,
                            is SosUiState.Error -> EmergencyRed
                            SosUiState.Idle,
                            SosUiState.Cancelled -> SafeGreen
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = when (uiState) {
                        SosUiState.Idle -> "ARMED"
                        SosUiState.CheckingPermissions -> "CHECKING"
                        is SosUiState.Countdown -> "COUNTING"
                        SosUiState.CollectingEmergencyData -> "PREPARING"
                        is SosUiState.ReadyToSend -> "READY"
                        is SosUiState.Active -> "LIVE SOS"
                        SosUiState.Cancelled -> "CANCELLED"
                        is SosUiState.Error -> "ERROR"
                    },
                    color = EmergencyWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SOS Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            if (isSosInProgress) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(pulseScale)
                        .background(
                            color = (if (isEmergencyActive) EmergencyRed else WarningAmber).copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
            }

            if (usesCountdownVisuals) {
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
            } else if (isEmergencyActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(220.dp),
                    color = EmergencyRedLight,
                    strokeWidth = 8.dp,
                    trackColor = EmergencyRedDark
                )
            }

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
                            colors = when {
                                isEmergencyActive -> listOf(
                                    EmergencyRedLight,
                                    EmergencyRed,
                                    EmergencyRedDark
                                )
                                usesCountdownVisuals -> listOf(
                                    Color(0xFFFFB74D),
                                    WarningAmber,
                                    Color(0xFFE65100)
                                )
                                else -> listOf(
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
                            when (uiState) {
                                SosUiState.Idle -> handleSosInitiation(immediate = false)
                                is SosUiState.Countdown -> handleSosInitiation(immediate = true)
                                else -> Unit
                            }
                        }
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when {
                        usesCountdownVisuals -> {
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
                        isEmergencyActive -> {
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
                        else -> {
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

        // Action Buttons
        if (uiState != SosUiState.Idle) {
            Button(
                onClick = {
                    when (uiState) {
                        SosUiState.Cancelled,
                        is SosUiState.Error -> viewModel.resetSos()
                        else -> viewModel.cancelSos()
                    }
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
                    text = when (uiState) {
                        SosUiState.Cancelled -> "Reset SOS"
                        is SosUiState.Error -> "Dismiss Error"
                        else -> "Cancel Alert / I Am Safe"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            OutlinedButton(
                onClick = { handleSosInitiation(immediate = false) },
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

        // Status Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Location Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isEmergencyActive && trackingState is LocationTrackingState.Tracking -> EmergencyRedContainer
                        isEmergencyActive -> EmergencyRedContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surface
                    }
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
                                when {
                                    isEmergencyActive && trackingState is LocationTrackingState.Tracking -> EmergencyRed
                                    isEmergencyActive -> WarningAmber
                                    else -> SafeGreenContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isEmergencyActive -> Icons.Filled.Sensors
                                else -> Icons.Filled.GpsFixed
                            },
                            contentDescription = null,
                            tint = when {
                                isEmergencyActive -> EmergencyWhite
                                else -> SafeGreen
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    isEmergencyActive && trackingState is LocationTrackingState.Tracking -> "🔴 Live Location ON"
                                    isEmergencyActive && trackingState is LocationTrackingState.LocationUnavailable -> "⚠️ Live Location Searching"
                                    isEmergencyActive -> "Connecting Live GPS..."
                                    else -> "GPS Location Standby"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isEmergencyActive -> EmergencyRed
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )

                            if (isEmergencyActive && trackingState is LocationTrackingState.Tracking) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmergencyRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE SYNC",
                                        color = EmergencyWhite,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = when (val tracking = trackingState) {
                                is LocationTrackingState.Tracking -> {
                                    "%.5f°, %.5f° • Accuracy ±%.1fm • Update #${tracking.updatesCount}".format(
                                        tracking.latitude,
                                        tracking.longitude,
                                        tracking.accuracy
                                    )
                                }
                                is LocationTrackingState.LocationUnavailable -> {
                                    tracking.reason
                                }
                                is LocationTrackingState.Error -> {
                                    tracking.message
                                }
                                LocationTrackingState.Initializing -> {
                                    "Starting high-accuracy foreground location tracking service..."
                                }
                                else -> {
                                    if (isEmergencyActive) {
                                        val data = (uiState as? SosUiState.Active)?.emergencyData
                                        if (data != null && data.isLocationAvailable) {
                                            "%.5f°, %.5f° • Accuracy ±%.1fm".format(data.latitude, data.longitude, data.locationAccuracy)
                                        } else {
                                            "Acquiring high accuracy location lock..."
                                        }
                                    } else {
                                        "High precision GPS coordinates will be continuously streamed"
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Emergency SMS Broadcast Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmergencyActive) {
                        EmergencyRedContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
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
                                if (isEmergencyActive) {
                                    EmergencyRed
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEmergencyActive) {
                                Icons.Filled.NotificationsActive
                            } else {
                                Icons.Outlined.Shield
                            },
                            contentDescription = null,
                            tint = if (isEmergencyActive) {
                                EmergencyWhite
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEmergencyActive) {
                                "Emergency Broadcast Active"
                            } else {
                                "Emergency Dispatch Standby"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEmergencyActive) {
                                EmergencyRed
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val totalSms = smsStatuses.size
                        val sentSms = smsStatuses.count { it.state == SmsDeliveryState.SENT || it.state == SmsDeliveryState.DELIVERED }
                        val failedSms = smsStatuses.count { it.state == SmsDeliveryState.FAILED }

                        Text(
                            text = if (isEmergencyActive) {
                                if (totalSms > 0) {
                                    if (failedSms > 0) {
                                        "SMS: $sentSms/$totalSms sent ($failedSms failed) • Live web link attached"
                                    } else {
                                        "SMS: $sentSms/$totalSms contacts alerted • Live web link attached"
                                    }
                                } else {
                                    "Dispatching SMS with live location link to all contacts..."
                                }
                            } else {
                                "Contacts will be alerted immediately with a live tracking link"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Battery Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            text = batteryPercentage?.let { percentage ->
                                "Battery Status: $percentage%"
                            } ?: "Battery Status: Reading...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Synchronized in emergency broadcast payload",
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