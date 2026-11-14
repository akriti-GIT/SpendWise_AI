package com.example.spendwise.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.spendwise.ui.components.SpendWiseLogoBadge
import com.example.spendwise.ui.theme.*
import com.example.spendwise.util.BiometricHelper
import com.example.spendwise.viewmodel.SpendWiseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    viewModel: SpendWiseViewModel,
    onUnlocked: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userSettings by viewModel.userSettings.collectAsState()
    val studentName = userSettings?.studentName ?: "Maya Chen"
    val isBiometricEnabled = userSettings?.isBiometricEnabled ?: true

    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        coroutineScope.launch {
            shakeOffset.animateTo(-16f, tween(50))
            shakeOffset.animateTo(16f, tween(50))
            shakeOffset.animateTo(-12f, tween(50))
            shakeOffset.animateTo(12f, tween(50))
            shakeOffset.animateTo(-6f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    // Biometric prompt trigger
    fun requestBiometricAuth() {
        if (context is FragmentActivity && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = context,
                title = "Unlock SpendWise",
                subtitle = "Confirm biometric to access your finances",
                onSuccess = {
                    viewModel.unlockWithBiometric()
                    onUnlocked()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        }
    }

    // Auto-prompt biometrics once on screen load if enabled and available
    LaunchedEffect(Unit) {
        if (isBiometricEnabled && context is FragmentActivity && BiometricHelper.isBiometricAvailable(context)) {
            delay(300)
            requestBiometricAuth()
        }
    }

    // Check PIN whenever it changes to 4 digits
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            delay(100)
            val success = viewModel.unlockWithPin(enteredPin)
            if (success) {
                onUnlocked()
            } else {
                isError = true
                errorMessage = "Incorrect PIN. Please try again."
                triggerShake()
                delay(600)
                enteredPin = ""
                isError = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Avatar & Welcome
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            SpendWiseLogoBadge(size = 64.dp)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome back,",
                fontFamily = SansFamily,
                fontSize = 13.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Text(
                text = studentName,
                fontFamily = SerifFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your 4-digit PIN to access SpendWise",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator (with horizontal shake)
            Row(
                modifier = Modifier
                    .offset(x = shakeOffset.value.dp)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    val dotColor = when {
                        isError -> Terracotta
                        isFilled -> DarkPine
                        else -> CardBorder
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) dotColor else Color.Transparent)
                            .border(2.dp, dotColor, CircleShape)
                    )
                }
            }

            // Error text display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage ?: "",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = Terracotta,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Spacer(modifier = Modifier.height(22.dp))
            }
        }

        // Center / Bottom Section: PIN Keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            for (row in keyRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        when (key) {
                            "BIO" -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            requestBiometricAuth()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Fingerprint,
                                        contentDescription = "Biometric Unlock",
                                        tint = if (isBiometricEnabled) DarkPine else TextMuted,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            "DEL" -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorMessage = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, CardBorder, CircleShape)
                                        .clickable {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                errorMessage = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontFamily = SansFamily,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch account / log out
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .clickable {
                        viewModel.logout(onComplete = onLogout)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Switch Account or Log Out",
                    fontFamily = SansFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
