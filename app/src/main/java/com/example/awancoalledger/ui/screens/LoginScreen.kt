package com.example.awancoalledger.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.awancoalledger.ui.components.*

import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.features.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginDialog(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onAuthSuccess: (Boolean) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val isAnonymous by viewModel.isAnonymous.collectAsState(initial = true)
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Icon
            Surface(
                modifier = Modifier.size(64.dp).padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0A84FF).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF0A84FF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = if (isSignUp) "Create Account" else "Welcome Back",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Sign in to keep your records in sync.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Form Fields
            if (isSignUp) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0A84FF),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0A84FF),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color(0xFFFF3B30), // iOS Red
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Primary Button
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    if (isAnonymous) {
                        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                        viewModel.linkAccount(credential) { success, msg ->
                            isLoading = false
                            if (success) {
                                onDismiss()
                                onAuthSuccess(true)
                            } else if (msg == "COLLISION") {
                                onDismiss()
                            } else {
                                error = msg
                            }
                        }
                    } else {
                        if (isSignUp) {
                            viewModel.signUpWithEmail(email, password, fullName) { success, isNewUser, msg ->
                                isLoading = false
                                if (success) {
                                    onDismiss()
                                    onAuthSuccess(isNewUser)
                                } else {
                                    if (msg == "VERIFICATION_REQUIRED") {
                                        error = "Account created! Please check your email to verify before logging in."
                                        isSignUp = false
                                        password = ""
                                    } else {
                                        error = msg
                                    }
                                }
                            }
                        } else {
                            viewModel.signInWithEmail(email, password) { success, isNewUser, msg ->
                                isLoading = false
                                if (success) {
                                    onDismiss()
                                    onAuthSuccess(isNewUser)
                                } else {
                                    error = msg
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isSignUp) "Create Account" else "Sign In",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Button
            Text(
                text = if (isSignUp) "Already have an account? Sign In" else "New here? Create Account",
                color = Color(0xFF0A84FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isSignUp = !isSignUp; error = null }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text("OR", modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Button
            OutlinedButton(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}



@Composable
fun LoginConfirmationDialog(
    isNewUser: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color(0xFF1C1C1E),
        icon = {
            Icon(
                if (isNewUser) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = if (isNewUser) Color(0xFF007AFF) else Color(0xFFFF9F0A),
                modifier = Modifier.size(44.dp)
            )
        },
        title = {
            Text(
                if (isNewUser) "Enable Cloud Sync" else "Existing Data Found",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            )
        },
        text = {
            Text(
                if (isNewUser) 
                    "We'll securely link and upload your current offline ledger to the cloud." 
                else 
                    "This will restore your cloud backup. Any guest data on this device will be replaced, and a restart will be required to load the new data.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isNewUser) Color(0xFF007AFF) else Color(0xFFFF9F0A)
                )
            ) {
                Text(
                    if (isNewUser) "Sync Now" else "Restore Data", 
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, 
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel", 
                    color = Color(0xFF007AFF),
                    fontSize = 17.sp
                )
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
