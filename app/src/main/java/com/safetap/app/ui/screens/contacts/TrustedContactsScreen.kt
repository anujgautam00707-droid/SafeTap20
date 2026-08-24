package com.safetap.app.ui.screens.contacts

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetap.app.data.contacts.TrustedContact
import com.safetap.app.di.SafeTapViewModelFactory
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.WarningAmber

private data class SampleContact(
    val name: String,
    val relationship: String,
    val phone: String
)

private val sampleContacts = listOf(
    SampleContact(
        name = "Sarah Jenkins",
        relationship = "Sister",
        phone = "+15552345678"
    ),
    SampleContact(
        name = "David Miller",
        relationship = "Father",
        phone = "+15558765432"
    ),
    SampleContact(
        name = "Dr. Emily Watson",
        relationship = "Family Doctor",
        phone = "+15553456789"
    )
)

private val contactAvatarColors = listOf(
    Color(0xFFFCE7F3),
    Color(0xFFDBEAFE),
    Color(0xFFDCFCE7),
    Color(0xFFFEF3C7),
    Color(0xFFEDE9FE)
)

@Composable
fun TrustedContactsScreen(
    viewModel: TrustedContactsViewModel = viewModel(
        factory = SafeTapViewModelFactory
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contactsList = uiState.contacts

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember {
        mutableStateOf<TrustedContact?>(null)
    }
    var contactToDelete by remember {
        mutableStateOf<TrustedContact?>(null)
    }

    if (showAddDialog) {
        ContactFormDialog(
            title = "Add Trusted Contact",
            onDismiss = {
                showAddDialog = false
                viewModel.clearError()
            },
            errorMessage = uiState.errorMessage,
            onSave = { name, phone, relationship ->
                val success = viewModel.addContact(name, relationship, phone)
                if (success) showAddDialog = false
                success
            }
        )
    }

    contactToEdit?.let { contact ->
        ContactFormDialog(
            title = "Edit Trusted Contact",
            initialName = contact.name,
            initialPhone = contact.phone,
            initialRelationship = contact.relationship,
            onDismiss = {
                contactToEdit = null
                viewModel.clearError()
            },
            errorMessage = uiState.errorMessage,
            onSave = { name, phone, relationship ->
                val success = viewModel.updateContact(
                    contact.copy(name = name, phone = phone, relationship = relationship)
                )
                if (success) contactToEdit = null
                success
            }
        )
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = {
                contactToDelete = null
            },
            title = {
                Text("Remove Contact?")
            },
            text = {
                Text(
                    "Are you sure you want to remove ${contact.name} " +
                            "from your trusted emergency contacts?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeContact(contact.id)
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed
                    )
                ) {
                    Text(
                        text = "Remove",
                        color = EmergencyWhite
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        contactToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 16.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trusted Contacts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = when (contactsList.size) {
                            0 -> "0 contacts linked"
                            1 -> "1 contact linked"
                            else -> "${contactsList.size} contacts linked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (contactsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                    ) {
                        Text(
                            text = "${contactsList.size} ${if (contactsList.size == 1) "ALLY" else "ALLIES"}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Contacts receive your live location and alert broadcast immediately upon SOS activation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (contactsList.isEmpty()) {
                EmptyContactsState(
                    onAddFirstContact = {
                        viewModel.clearError()
                        showAddDialog = true
                    },
                    onResetDefaults = {
                        sampleContacts.forEach { sampleContact ->
                            viewModel.addContact(
                                name = sampleContact.name,
                                relationship =
                                    sampleContact.relationship,
                                phone = sampleContact.phone
                            )
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = contactsList,
                        key = { contact ->
                            contact.id
                        }
                    ) { contact ->
                        ContactCard(
                            contact = contact,
                            onCall = {
                                val dialablePhone = contact.phone.replace(
                                    "[^0-9+]".toRegex(),
                                    ""
                                )

                                val intent = Intent(
                                    Intent.ACTION_DIAL
                                ).apply {
                                    data = Uri.parse(
                                        "tel:$dialablePhone"
                                    )
                                }

                                context.startActivity(intent)
                            },
                            onEdit = {
                                contactToEdit = contact
                            },
                            onSetPrimary = {
                                viewModel.setPrimaryContact(contact.id)
                            },
                            onDelete = {
                                contactToDelete = contact
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                viewModel.clearError()
                showAddDialog = true
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Add Contact"
                )
            },
            text = {
                Text(
                    text = "Add Contact",
                    fontWeight = FontWeight.Bold
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = EmergencyWhite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
private fun ContactCard(
    contact: TrustedContact,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit
) {
    val avatarBackgroundColor = remember(contact.id) {
        contactAvatarColor(contact.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = remember(contact.name) {
                contactInitials(contact.name)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onSetPrimary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = if (contact.isPrimary) "Primary Contact" else "Set as Primary",
                            tint = if (contact.isPrimary) WarningAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            )
                    ) {
                        Text(
                            text = contact.relationship,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onCall,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SafeGreenContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call ${contact.name}",
                    tint = SafeGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${contact.name}",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.5f
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete ${contact.name}",
                    tint = EmergencyRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyContactsState(
    onAddFirstContact: () -> Unit,
    onResetDefaults: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 40.dp,
                horizontal = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(EmergencyRedContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PeopleAlt,
                contentDescription = null,
                tint = EmergencyRed,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Trusted Contacts Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add friends, family members, or caregivers who will receive instant notifications and live tracking whenever you trigger an SOS.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddFirstContact,
            modifier = Modifier.height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmergencyRed
            )
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Add First Contact",
                color = EmergencyWhite
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onResetDefaults,
            modifier = Modifier.height(42.dp)
        ) {
            Text("Load Sample Contacts")
        }
    }
}

@Composable
private fun ContactFormDialog(
    title: String,
    initialName: String = "",
    initialPhone: String = "",
    initialRelationship: String = "",
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    onSave: (String, String, String) -> Boolean
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var relationship by remember { mutableStateOf(initialRelationship) }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "This contact will receive SOS alerts, live GPS location, and SMS in emergencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { value ->
                        name = value
                        if (value.isNotBlank()) nameError = false
                    },
                    label = { Text("Full Name *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { value ->
                        phone = value
                        if (value.isNotBlank()) phoneError = false
                    },
                    label = { Text("Phone Number *") },
                    isError = phoneError || errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { value -> relationship = value },
                    label = { Text("Relationship (e.g. Mom, Partner, Friend)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = name.isBlank()
                    phoneError = phone.isBlank()
                    if (nameError || phoneError) return@Button
                    onSave(name, phone, relationship)
                }
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun contactInitials(
    contactName: String
): String {
    val nameParts = contactName
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)

    return when {
        nameParts.isEmpty() -> "?"
        nameParts.size == 1 -> nameParts.first().take(2).uppercase()
        else -> buildString {
            append(nameParts.first().first().uppercaseChar())
            append(nameParts[1].first().uppercaseChar())
        }
    }
}

private fun contactAvatarColor(
    contactId: String
): Color {
    val colorIndex =
        (contactId.hashCode() and Int.MAX_VALUE) %
                contactAvatarColors.size

    return contactAvatarColors[colorIndex]
}