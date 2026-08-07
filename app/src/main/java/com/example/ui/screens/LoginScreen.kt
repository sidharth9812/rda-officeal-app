package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import com.example.model.UserRole
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.CyberButton
import com.example.ui.components.DeveloperCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.*

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var securityCode by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var resetError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.collectAsState()
    val developerInfo by authViewModel.repository.developerInfo.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        IconButton(
            onClick = { ThemeManager.toggleTheme(context) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(42.dp)
                .clip(CircleShape)
                .background(CyberSurface)
                .border(1.dp, CyberBorder, CircleShape)
        ) {
            Icon(
                imageVector = if (ThemeManager.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = if (ThemeManager.isDarkMode) Color(0xFFFDE047) else BentoNavy,
                modifier = Modifier.size(20.dp)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, BentoNavy.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = R.drawable.rda_academy_logo_1785683224405,
                        contentDescription = "Raghukul Defence Academy Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "RDA PHYSICAL ACADEMY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Firebase Authentication Portal",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Mode Toggle Bar (LOGIN vs REGISTER)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp))
                        .background(CyberSurfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (!isRegisterMode) BentoNavy else Color.Transparent)
                            .clickable { isRegisterMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOGIN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isRegisterMode) Color.White else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isRegisterMode) BentoNavy else Color.Transparent)
                            .clickable { isRegisterMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "REGISTER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRegisterMode) Color.White else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (authState is AuthState.Error) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoCoralCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = BentoCoralOn, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = BentoCoralOn,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isRegisterMode) {
                    // Profile Photo Selection Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, CyberBorder, RoundedCornerShape(18.dp))
                            .background(CyberSurfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(
                                    photoUrl = photoUrl.ifBlank { if (selectedRole == UserRole.GROUP_LEADER) "preset_leader" else "preset_runner" },
                                    size = 48.dp,
                                    iconSize = 26.dp,
                                    showEditOverlay = true,
                                    onEditClick = { showAvatarPicker = true }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Profile Picture", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (photoUrl.isBlank()) "Tap to select photo / avatar" else "Photo set successfully",
                                        fontSize = 10.sp,
                                        color = if (photoUrl.isBlank()) TextSecondary else BentoMintOn
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showAvatarPicker = true },
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoNavy),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BentoNavy) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoNavy,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email Address Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BentoNavy) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoNavy,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BentoPurpleOn) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        if (isRegisterMode) {
                            authViewModel.registerWithEmail(fullName, email, password, selectedRole, securityCode, photoUrl)
                        } else {
                            authViewModel.loginWithEmail(email, password)
                        }
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoPurpleOn,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (!isRegisterMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                resetEmail = email.trim()
                                resetMessage = null
                                resetError = null
                                showForgotPasswordDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoPurpleOn
                            )
                        }
                    }
                }

                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SELECT ACADEMY ROLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.STUDENT,
                            onClick = { selectedRole = UserRole.STUDENT },
                            label = { Text("STUDENT", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoMintCard,
                                selectedLabelColor = BentoMintOn,
                                containerColor = CyberSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN },
                            label = { Text("ADMIN", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoCoralCard,
                                selectedLabelColor = BentoCoralOn,
                                containerColor = CyberSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedRole == UserRole.ADMIN) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = securityCode,
                            onValueChange = { securityCode = it },
                            label = { Text("ADMIN Security Passcode", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = BentoCoralOn) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "🔒 Security Passcode required for account creation",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier
                                .padding(top = 4.dp, start = 4.dp)
                                .align(Alignment.Start)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = BentoNavy, modifier = Modifier.size(36.dp))
                } else {
                    CyberButton(
                        text = if (isRegisterMode) "CREATE ACCOUNT" else "LOGIN TO ACADEMY",
                        onClick = {
                            keyboardController?.hide()
                            if (isRegisterMode) {
                                authViewModel.registerWithEmail(fullName, email, password, selectedRole, securityCode, photoUrl)
                            } else {
                                authViewModel.loginWithEmail(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.Login,
                        containerColor = BentoNavy,
                        contentColor = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // DEVELOPER SECTION
                DeveloperCard(developerInfo = developerInfo)
            }
        }

        if (showAvatarPicker) {
            AvatarPickerDialog(
                currentPhotoUrl = photoUrl,
                onAvatarSelected = { photoUrl = it },
                onDismiss = { showAvatarPicker = false }
            )
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSendingReset) showForgotPasswordDialog = false
                },
                containerColor = CyberSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BentoPurpleCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = BentoPurpleOn, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Reset Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Enter your registered email address. A password reset link will be sent to your inbox via Firebase Authentication.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Registered Email", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BentoNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (!isSendingReset && resetEmail.isNotBlank()) {
                                    isSendingReset = true
                                    resetMessage = null
                                    resetError = null
                                    authViewModel.sendPasswordResetEmail(resetEmail) { success, msg ->
                                        isSendingReset = false
                                        if (success) {
                                            resetMessage = msg
                                            resetError = null
                                        } else {
                                            resetError = msg
                                            resetMessage = null
                                        }
                                    }
                                }
                            }),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoNavy,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (resetMessage != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BentoMintCard)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoMintOn, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resetMessage ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoMintOn
                                    )
                                }
                            }
                        }

                        if (resetError != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = resetError ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSendingReset = true
                            resetMessage = null
                            resetError = null
                            authViewModel.sendPasswordResetEmail(resetEmail) { success, msg ->
                                isSendingReset = false
                                if (success) {
                                    resetMessage = msg
                                    resetError = null
                                } else {
                                    resetError = msg
                                    resetMessage = null
                                }
                            }
                        },
                        enabled = !isSendingReset && resetEmail.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        if (isSendingReset) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("SEND RESET LINK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showForgotPasswordDialog = false },
                        enabled = !isSendingReset
                    ) {
                        Text("CLOSE", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


