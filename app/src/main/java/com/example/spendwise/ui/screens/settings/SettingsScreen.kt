package com.example.spendwise.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.ui.components.ConfirmationDialog
import com.example.spendwise.ui.theme.*
import com.example.spendwise.viewmodel.SpendWiseViewModel

@Composable
fun SettingsScreen(
    viewModel: SpendWiseViewModel,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showResetSampleConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf("") }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }

    val isCloudAi by viewModel.isCloudAiEnabled.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initAiConfig(context)
    }

    val studentName = userSettings?.studentName ?: "Maya Chen"
    val studentId = userSettings?.collegeId ?: "MCA-2026-087"
    val userEmail = userSettings?.email ?: "maya.chen@campus.edu"
    val isAppLockEnabled = userSettings?.isAppLockEnabled ?: true
    val isBiometricEnabled = userSettings?.isBiometricEnabled ?: true
    val isDark = userSettings?.darkMode ?: false

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "PREFERENCES",
                    fontFamily = SansFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.0.sp,
                    color = Terracotta,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Settings",
                    fontFamily = SerifFamily,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Profile Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                    .clickable {
                        editName = studentName
                        editId = studentId
                        showProfileDialog = true
                    }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(PaleGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = studentName.firstOrNull()?.uppercase() ?: "S",
                            fontFamily = SerifFamily,
                            fontSize = 24.sp,
                            color = DarkPine,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName,
                            fontFamily = SerifFamily,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Student ID: $studentId",
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Edit Profile",
                        tint = TextMuted
                    )
                }
            }
        }

        // Section: Security & Authentication
        item {
            SectionHeader("ACCOUNT & SECURITY")
        }

        item {
            CardContainer {
                SettingRow(
                    icon = Icons.Outlined.Email,
                    title = "Account Email",
                    detail = userEmail
                )

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // App Lock Toggle (Require PIN every time app opens)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = DarkPine,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Lock on Launch",
                                fontFamily = SansFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Require 4-digit PIN every time app opens",
                                fontFamily = SansFamily,
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CardLight, checkedTrackColor = DarkPine)
                    )
                }

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Biometric Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = DarkPine,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Biometric Unlock",
                                fontFamily = SansFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fingerprint / Face recognition prompt",
                                fontFamily = SansFamily,
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CardLight, checkedTrackColor = DarkPine)
                    )
                }

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Change PIN
                SettingActionRow(
                    icon = Icons.Outlined.Pin,
                    title = "Change Security PIN",
                    onClick = {
                        oldPinInput = ""
                        newPinInput = ""
                        pinError = null
                        showChangePinDialog = true
                    }
                )

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Log out
                SettingActionRow(
                    icon = Icons.Outlined.Logout,
                    title = "Log Out of Account",
                    textColor = Terracotta,
                    onClick = { showLogoutConfirm = true }
                )
            }
        }

        // Section: AI & Smart Receipt Detection
        item {
            SectionHeader("AI & SMART DETECTION")
        }

        item {
            CardContainer {
                // Cloud AI Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = DarkPine,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Gemini Vision AI",
                                fontFamily = SansFamily,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCloudAi) "Active (multimodal cloud vision)" else "Disabled (local OCR only)",
                                fontFamily = SansFamily,
                                fontSize = 11.sp,
                                color = if (isCloudAi) SageGreen else TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = isCloudAi,
                        onCheckedChange = { viewModel.setCloudAiEnabled(context, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CardLight, checkedTrackColor = DarkPine)
                    )
                }

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Gemini API Key
                SettingActionRow(
                    icon = Icons.Outlined.Key,
                    title = "Gemini API Key",
                    detail = if (geminiKey.isNotBlank()) "Configured (••••${geminiKey.takeLast(4)})" else "Default / Custom Key",
                    onClick = {
                        apiKeyInput = geminiKey
                        showApiKeyDialog = true
                    }
                )

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Local Fallback Status
                SettingRow(
                    icon = Icons.Outlined.CloudDone,
                    title = "Offline Local Fallback",
                    detail = "Active (ML Kit Devanagari)"
                )
            }
        }

        // Section: Preferences
        item {
            SectionHeader("APP PREFERENCES")
        }

        item {
            CardContainer {
                // Currency
                SettingRow(
                    icon = Icons.Outlined.CurrencyRupee,
                    title = "Currency",
                    detail = "Indian Rupee (₹)"
                )
                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = DarkPine,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dark Mode",
                            fontFamily = SansFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CardLight, checkedTrackColor = DarkPine)
                    )
                }

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Notifications
                SettingRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Budget Alerts",
                    detail = "Enabled"
                )
            }
        }

        // Section: Data Management
        item {
            SectionHeader("DATA MANAGEMENT")
        }

        item {
            CardContainer {
                // Export Data
                SettingActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Export Expense Report",
                    onClick = {
                        val report = viewModel.exportDataSummary()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SpendWise Report", report))
                        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Reset Sample Data
                SettingActionRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reset to Sample Data",
                    onClick = { showResetSampleConfirm = true }
                )

                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)

                // Clear All Data
                SettingActionRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Clear All Transactions",
                    textColor = ErrorRed,
                    onClick = { showClearDataConfirm = true }
                )
            }
        }

        // Section: About
        item {
            SectionHeader("ABOUT")
        }

        item {
            CardContainer {
                SettingRow(icon = Icons.Outlined.Info, title = "Application", detail = "SpendWise v1.0")
                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)
                SettingRow(icon = Icons.Outlined.School, title = "Course Project", detail = "MCA MAD Activity 1")
                HorizontalDivider(color = CardBorder, thickness = 0.8.dp)
                SettingRow(icon = Icons.Outlined.Layers, title = "Architecture", detail = "Jetpack Compose + Room")
            }
        }
    }

    // Profile Edit Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Edit Student Profile", fontFamily = SerifFamily, fontSize = 20.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Student Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editId,
                        onValueChange = { editId = it },
                        label = { Text("College / Roll ID") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateProfile(editName, editId)
                            showProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPine)
                ) {
                    Text("Save", fontFamily = SansFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Cancel", fontFamily = SansFamily, color = TextSecondary)
                }
            }
        )
    }

    // Reset Confirmation
    if (showResetSampleConfirm) {
        ConfirmationDialog(
            title = "Load Sample Data?",
            message = "This will load sample income and expense transactions into your account for testing. Other accounts are not affected.",
            confirmText = "Load Samples",
            onConfirm = {
                showResetSampleConfirm = false
                viewModel.resetToSampleData {
                    Toast.makeText(context, "Sample data added to your account!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showResetSampleConfirm = false }
        )
    }

    // Clear Confirmation
    if (showClearDataConfirm) {
        ConfirmationDialog(
            title = "Clear Your Data?",
            message = "This will permanently remove your transactions and budgets. Other user accounts will not be affected. Are you sure?",
            confirmText = "Clear My Data",
            onConfirm = {
                showClearDataConfirm = false
                viewModel.clearAllData {
                    Toast.makeText(context, "Your data has been cleared.", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showClearDataConfirm = false }
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Change Security PIN", fontFamily = SerifFamily, fontSize = 20.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your current 4-digit PIN and select a new one.",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                oldPinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("Current PIN") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                newPinInput = it
                                pinError = null
                            }
                        },
                        label = { Text("New 4-digit PIN") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )

                    pinError?.let { err ->
                        Text(
                            text = err,
                            fontFamily = SansFamily,
                            fontSize = 11.5.sp,
                            color = Terracotta
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePin(
                            oldPin = oldPinInput,
                            newPin = newPinInput,
                            onSuccess = {
                                showChangePinDialog = false
                                Toast.makeText(context, "PIN updated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                pinError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPine)
                ) {
                    Text("Update PIN", fontFamily = SansFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", fontFamily = SansFamily, color = TextSecondary)
                }
            }
        )
    }

    // Logout Confirmation
    if (showLogoutConfirm) {
        ConfirmationDialog(
            title = "Log Out?",
            message = "You will be returned to the login screen. You will need your password or PIN to sign back in.",
            confirmText = "Log Out",
            onConfirm = {
                showLogoutConfirm = false
                viewModel.logout(onComplete = onLogout)
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    // Gemini API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "Gemini Vision API Key",
                    fontFamily = SerifFamily,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "SpendWise-AI uses Google Gemini 1.5 Flash Vision to parse complex receipts, split bills, and bus fares with 100% accuracy. Get a free API key at aistudio.google.com",
                        fontFamily = SansFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Leave empty to use built-in / offline mode.",
                        fontFamily = SansFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setGeminiApiKey(context, apiKeyInput.trim())
                        showApiKeyDialog = false
                        Toast.makeText(context, "API Key saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPine)
                ) {
                    Text("Save Key", fontFamily = SansFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", fontFamily = SansFamily, color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = SansFamily,
        fontSize = 9.5.sp,
        letterSpacing = 1.8.sp,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun CardContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DarkPine, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontFamily = SansFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = detail,
            fontFamily = SansFamily,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (textColor != Color.Unspecified) textColor else DarkPine, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontFamily = SansFamily,
                fontSize = 14.sp,
                color = if (textColor != Color.Unspecified) textColor else MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
