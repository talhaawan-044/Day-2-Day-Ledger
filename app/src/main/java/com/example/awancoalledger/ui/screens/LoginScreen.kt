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
import com.example.awancoalledger.viewmodel.LedgerViewModel

@Composable
fun LoginDialog(
    viewModel: LedgerViewModel,
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
            // Header Section (Solid Blue/Dark Gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFF001F3F))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Custom Close Button (Top Left - iOS style)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF0A84FF),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // App Identity
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CloudQueue,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = if (isSignUp) "Join Awan Ledger" else "Welcome Back",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (isSignUp) "Secure your data across all your devices." else "Sign in to keep your records in sync.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSignUp) {
                        PremiumInput(
                            label = "Full Name",
                            value = fullName,
                            onValueChange = { fullName = it }
                        )
                    }
                    PremiumInput(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it },
                        keyboardType = KeyboardType.Email
                    )
                    PremiumInput(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }


                error?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFFF453A),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary Action Button
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
                                        error = msg
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isSignUp) "Create Account" else "Sign In",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Secondary Action
                Text(
                    text = if (isSignUp) "Already have an account? Sign In" else "New here? Create Account",
                    color = Color(0xFF007AFF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { 
                        isSignUp = !isSignUp
                        error = null
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Social Dividers
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                    Text(
                        "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Google Button (Refined iOS style)
                Surface(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continue with Google", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(60.dp))
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
