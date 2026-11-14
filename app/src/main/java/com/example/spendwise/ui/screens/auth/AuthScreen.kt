package com.example.spendwise.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.ui.components.SpendWiseBrandHeader
import com.example.spendwise.ui.theme.*
import com.example.spendwise.viewmodel.SpendWiseViewModel

@Composable
fun AuthScreen(
    viewModel: SpendWiseViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoginTab by remember { mutableStateOf(true) }

    // Login state
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Signup state
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPin by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Brand Logo & Title
        SpendWiseBrandHeader(
            logoSize = 72.dp,
            showTagline = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Toggle Tabs: Log In / Sign Up
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(PillBg)
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.width(260.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (isLoginTab) DarkPine else Color.Transparent)
                        .clickable {
                            isLoginTab = true
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log In",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLoginTab) CardLight else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (!isLoginTab) DarkPine else Color.Transparent)
                        .clickable {
                            isLoginTab = false
                            errorMessage = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign Up",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!isLoginTab) CardLight else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error message banner
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            errorMessage?.let { error ->
                Surface(
                    color = Terracotta.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = "Error",
                            tint = Terracotta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = error,
                            fontFamily = SansFamily,
                            fontSize = 12.sp,
                            color = Terracotta,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Form Fields
        if (isLoginTab) {
            // LOGIN FORM
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = loginEmail,
                    onValueChange = {
                        loginEmail = it
                        errorMessage = null
                    },
                    label = { Text("Email Address", fontFamily = SansFamily, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = {
                        loginPassword = it
                        errorMessage = null
                    },
                    label = { Text("Password or 4-digit PIN", fontFamily = SansFamily, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                            Icon(
                                if (loginPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        val cleanEmail = loginEmail.trim()
                        val cleanPass = loginPassword.trim()
                        if (cleanEmail.isBlank()) {
                            errorMessage = "Please enter your email address"
                            return@KeyboardActions
                        }
                        if (cleanPass.isBlank()) {
                            errorMessage = "Please enter your password or 4-digit PIN"
                            return@KeyboardActions
                        }
                        isLoading = true
                        errorMessage = null
                        viewModel.login(
                            email = cleanEmail,
                            passwordOrPin = cleanPass,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val cleanEmail = loginEmail.trim()
                        val cleanPass = loginPassword.trim()
                        if (cleanEmail.isBlank()) {
                            errorMessage = "Please enter your email address"
                            return@Button
                        }
                        if (cleanPass.isBlank()) {
                            errorMessage = "Please enter your password or 4-digit PIN"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        viewModel.login(
                            email = cleanEmail,
                            passwordOrPin = cleanPass,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPine,
                        disabledContainerColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CardLight, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = "Log In",
                            fontFamily = SansFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CardLight
                        )
                    }
                }

                // Or Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                    Text(
                        text = "OR",
                        fontFamily = SansFamily,
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = CardBorder)
                }

                // 1-Tap Student Demo Login button
                OutlinedButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.quickDemoLogin(onSuccess = onAuthSuccess)
                    },
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = DarkPine,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "1-Tap Demo Sign-In (Maya Chen)",
                        fontFamily = SansFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // SIGN UP FORM
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = signupName,
                    onValueChange = {
                        signupName = it
                        errorMessage = null
                    },
                    label = { Text("Full Name", fontFamily = SansFamily, fontSize = 13.sp) },
                    placeholder = { Text("e.g. Alex Morgan", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = signupEmail,
                    onValueChange = {
                        signupEmail = it
                        errorMessage = null
                    },
                    label = { Text("Email Address", fontFamily = SansFamily, fontSize = 13.sp) },
                    placeholder = { Text("student@university.edu", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = signupPassword,
                    onValueChange = {
                        signupPassword = it
                        errorMessage = null
                    },
                    label = { Text("Account Password", fontFamily = SansFamily, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                            Icon(
                                if (signupPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Toggle",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = signupPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            signupPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("4-digit Quick App Lock PIN", fontFamily = SansFamily, fontSize = 13.sp) },
                    placeholder = { Text("e.g. 2468", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Pin, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        val cleanName = signupName.trim()
                        val cleanEmail = signupEmail.trim()
                        val cleanPass = signupPassword.trim()
                        val cleanPin = signupPin.trim()

                        if (cleanName.isBlank()) {
                            errorMessage = "Please enter your full name"
                            return@KeyboardActions
                        }
                        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                            errorMessage = "Please enter a valid email address"
                            return@KeyboardActions
                        }
                        if (cleanPass.length < 4) {
                            errorMessage = "Password must be at least 4 characters"
                            return@KeyboardActions
                        }
                        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
                            errorMessage = "Security PIN must be exactly 4 digits (e.g. 1234)"
                            return@KeyboardActions
                        }

                        isLoading = true
                        errorMessage = null
                        viewModel.register(
                            name = cleanName,
                            email = cleanEmail,
                            password = cleanPass,
                            pin = cleanPin,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    }),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPine,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "This PIN will be requested every time the app opens on Android to secure your private financial data.",
                    fontFamily = SansFamily,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val cleanName = signupName.trim()
                        val cleanEmail = signupEmail.trim()
                        val cleanPass = signupPassword.trim()
                        val cleanPin = signupPin.trim()

                        if (cleanName.isBlank()) {
                            errorMessage = "Please enter your full name"
                            return@Button
                        }
                        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                            errorMessage = "Please enter a valid email address"
                            return@Button
                        }
                        if (cleanPass.length < 4) {
                            errorMessage = "Password must be at least 4 characters"
                            return@Button
                        }
                        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) {
                            errorMessage = "Security PIN must be exactly 4 digits (e.g. 1234)"
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        viewModel.register(
                            name = cleanName,
                            email = cleanEmail,
                            password = cleanPass,
                            pin = cleanPin,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPine,
                        disabledContainerColor = CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CardLight, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            text = "Create Account & Get Started",
                            fontFamily = SansFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CardLight
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Security note
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = DarkPine,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% on-device private & secure storage",
                fontFamily = SansFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
