package com.safetap.app.ui.screens.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.safetap.app.ui.theme.EmergencyRed
import com.safetap.app.ui.theme.EmergencyRedContainer
import com.safetap.app.ui.theme.EmergencyWhite
import com.safetap.app.ui.theme.SafeGreen
import com.safetap.app.ui.theme.SafeGreenContainer
import com.safetap.app.ui.theme.WarningAmber
import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String,
    val phone: String,
    val isPrimary: Boolean = false,
    val avatarBgColor: Color = Color(0xFFE0E7FF)
)

private val InitialDummyContacts = listOf(
    Contact(
        name = "Sarah Jenkins",
        relationship = "Sister",
        phone = "+1 (555) 234-5678",
        isPrimary = true,
        avatarBgColor = Color(0xFFFCE7F3)
    ),
    Contact(
        name = "David Miller",
        relationship = "Father",
        phone = "+1 (555) 876-5432",
        isPrimary = true,
        avatarBgColor = Color(0xFFDBEAFE)
    ),
    Contact(
        name = "Dr. Emily Watson",
        relationship = "Family Doctor",
        phone = "+1 (555) 345-6789",
        isPrimary = false,
        avatarBgColor = Color(0xFFDCFCE7)
    )
)

@Composable
fun TrustedContactsScreen(
    viewModel: TrustedContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val contactsList = remember { mutableStateListOf<Contact>().apply { addAll(InitialDummyContacts) } }

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // Dialog form state
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRelationship by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    // Add Contact Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                nameError = false
                phoneError = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Add Trusted Contact",
                    fontWeight = FontWeight.Bold
                )
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
                        value = newName,
                        onValueChange = {
                            newName = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("Full Name *") },
                        isError = nameError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = {
                            newPhone = it
                            if (it.isNotBlank()) phoneError = false
                        },
                        label = { Text("Phone Number *") },
                        isError = phoneError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newRelationship,
                        onValueChange = { newRelationship = it },
                        label = { Text("Relationship (e.g. Mom, Partner, Friend)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        if (newPhone.isBlank()) {
                            phoneError = true
                            return@Button
                        }

                        val avatarColors = listOf(
                            Color(0xFFFCE7F3),
                            Color(0xFFDBEAFE),
                            Color(0xFFDCFCE7),
                            Color(0xFFFEF3C7),
                            Color(0xFFEDE9FE)
                        )

                        contactsList.add(
                            Contact(
                                name = newName.trim(),
                                relationship = newRelationship.trim().ifBlank { "Emergency Contact" },
                                phone = newPhone.trim(),
                                isPrimary = contactsList.isEmpty(),
                                avatarBgColor = avatarColors[contactsList.size % avatarColors.size]
                            )
                        )

                        // Reset form
                        newName = ""
                        newPhone = ""
                        newRelationship = ""
                        showAddDialog = false
                        viewModel.onAddContactClicked()
                    }
                ) {
                    Text("Save Contact")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        nameError = false
                        phoneError = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Remove Contact?") },
            text = {
                Text("Are you sure you want to remove ${contact.name} from your trusted emergency contacts?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        contactsList.remove(contact)
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Remove", color = EmergencyWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Section
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
                        text = if (contactsList.isEmpty())
                            "No emergency contacts linked"
                        else
                            "${contactsList.size} contacts will receive instant SOS alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (contactsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${contactsList.size} ALLIES",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

            // Main Content: List or Empty State
            if (contactsList.isEmpty()) {
                EmptyContactsState(
                    onAddFirstContact = { showAddDialog = true },
                    onResetDefaults = { contactsList.addAll(InitialDummyContacts) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = contactsList,
                        key = { it.id }
                    ) { contact ->
                        ContactCard(
                            contact = contact,
                            onCall = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${contact.phone.replace("[^0-9+]".toRegex(), "")}")
                                }
                                context.startActivity(intent)
                            },
                            onDelete = { contactToDelete = contact }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Filled.PersonAdd, contentDescription = "Add Contact") },
            text = { Text("Add Contact", fontWeight = FontWeight.Bold) },
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
    contact: Contact,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
            // Avatar Initials
            val initials = remember(contact.name) {
                val parts = contact.name.trim().split(" ")
                if (parts.size >= 2) {
                    "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
                } else {
                    contact.name.take(2).uppercase()
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contact.avatarBgColor),
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

            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (contact.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Primary Contact",
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = contact.relationship,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Call Button
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

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
            .padding(vertical = 40.dp, horizontal = 16.dp),
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
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Contact", color = EmergencyWhite)
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
