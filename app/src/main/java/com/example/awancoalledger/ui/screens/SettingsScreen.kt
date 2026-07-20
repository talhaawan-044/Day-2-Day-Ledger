package com.example.awancoalledger.ui.screens

import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.awancoalledger.data.*
import com.example.awancoalledger.ui.components.*
import com.example.awancoalledger.ui.theme.*
import com.example.awancoalledger.viewmodel.features.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import androidx.activity.compose.BackHandler
import com.example.awancoalledger.ui.components.IOSDialogButton
enum class SettingsCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: androidx.compose.ui.graphics.Color, val subtitle: String) {
    BACKUPS_AND_SYNC("Backups & Sync", androidx.compose.material.icons.Icons.Outlined.CloudUpload, androidx.compose.ui.graphics.Color(0xFF007AFF), "Sync & Recovery"),
    BUSINESS_PROFILE("Business Profile", androidx.compose.material.icons.Icons.Outlined.Business, androidx.compose.ui.graphics.Color(0xFF007AFF), "Name, phone, logo"),
    PRIVACY_SECURITY("Privacy & Security", androidx.compose.material.icons.Icons.Outlined.Lock, ErrorRed, "App lock, biometrics"),
    PREFERENCES("Preferences", androidx.compose.material.icons.Icons.Outlined.SettingsSuggest, iOSPurple, "Dark mode, dock")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: com.example.awancoalledger.viewmodel.features.AuthViewModel,

        viewModel: SettingsViewModel,
        onNavigateToShortcut: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val activeRemindersCount = 0

    val bizName by viewModel.businessName.collectAsState(initial = "")
    val ownerName by viewModel.ownerName.collectAsState(initial = "")
    val phone by viewModel.businessPhone.collectAsState(initial = "")
    val countryConfig by viewModel.countryConfig.collectAsState()
    val address by viewModel.businessAddress.collectAsState(initial = "")
    val appLock by viewModel.isAppLockEnabled.collectAsState(initial = false)
    val biometrics by viewModel.isBiometricsEnabled.collectAsState(initial = false)
    val darkMode by viewModel.isDarkMode.collectAsState(initial = false)
    val frostedGlass by viewModel.isFrostedGlassEnabled.collectAsState(initial = true)
    val logoUri by viewModel.companyLogoUri.collectAsState(initial = null)
    val signatureUri by viewModel.signatureUri.collectAsState(initial = null)
    val oilInterval by viewModel.oilChangeInterval.collectAsState()

    val isAnonymous by authViewModel.isAnonymous.collectAsState(initial = true)
    val showConflictDialog by authViewModel.showConflictDialog.collectAsState()

    // Launchers
    val logoPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { viewModel.updateCompanyLogoUri(context, it) }
            }

    val signaturePicker =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let { viewModel.updateSignatureUri(context, it) }
            }

    val user by authViewModel.currentUser.collectAsState(initial = null)
    var showRestoreWarning by remember { mutableStateOf(false) }

    val googleSignInLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task =
                        com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getSignedInAccountFromIntent(result.data)
                try {
                    val account =
                            task.getResult(
                                    com.google.android.gms.common.api.ApiException::class.java
                            )
                    val idToken = account.idToken ?: return@rememberLauncherForActivityResult
                    val credential =
                            com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)

                    if (isAnonymous) {
                        authViewModel.linkAccount(credential) { success, error ->
                            if (success) {
                                Toast.makeText(
                                                context,
                                                "Account Linked Successfully!",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            } else if (error != "COLLISION") {
                                Toast.makeText(
                                                context,
                                                "Linking Failed: $error",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            }
                        }
                    } else {
                        authViewModel.signInWithFirebase(credential) { success, isNewUser ->
                            if (success) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isNewUser) {
                                    authViewModel.confirmLogin(true)
                                    Toast.makeText(
                                                    context,
                                                    "Cloud Sync Enabled!",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                } else {
                                    authViewModel.confirmLogin(false)
                                }
                            } else {
                                Toast.makeText(context, "Sign In Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG)
                            .show()
                }
            }

    val restoreLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri?.let {
                    authViewModel.restoreDatabase(
                            context = context,
                            uri = it,
                            onSuccess = {
                                Toast.makeText(context, "Restore Successful!", Toast.LENGTH_SHORT)
                                        .show()
                            },
                            onError = { error ->
                                Toast.makeText(context, "Restore Failed: $error", Toast.LENGTH_LONG)
                                        .show()
                            }
                    )
                }
            }

    // State for Modals
    var editingField by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showPinKeypad by remember { mutableStateOf(false) }
    var showBackupsModal by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showDockCustomizationModal by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var currentCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val dockItems by viewModel.dockItems.collectAsState()

    BackHandler(enabled = currentCategory != null) {
        currentCategory = null
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = currentCategory,
                label = "SettingsNav",
                transitionSpec = {
                    if (targetState != null && initialState == null) {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
                    } else if (targetState == null && initialState != null) {
                        slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn() togetherWith slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                }
            ) { targetCategory ->
                if (targetCategory == null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val backDispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                        com.example.awancoalledger.ui.components.ScreenHeader(
                            title = "Settings",
                            onBack = { backDispatcher?.onBackPressed() },
                            modifier = Modifier.padding(horizontal = 0.dp) // already padded in parent
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        val companyLogoUri by viewModel.companyLogoUri.collectAsState()
                        val isLogoUploading by viewModel.isLogoUploading.collectAsState()
                        
                        // Profile Header (Premium Card)
                        ProfileHeader(
                            owner = ownerName,
                            biz = bizName,
                            logoUri = companyLogoUri,
                            isUploading = isLogoUploading,
                            onLogoClick = { 
                                logoPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                            }
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Sections
                val allModules = listOf(
                    com.example.awancoalledger.NavTab("Contacts", "parties", androidx.compose.material.icons.Icons.Outlined.People),
                    com.example.awancoalledger.NavTab("Expenses", "expenses", androidx.compose.material.icons.Icons.Outlined.Payments),
                    com.example.awancoalledger.NavTab("Inventory", "inventory", androidx.compose.material.icons.Icons.Outlined.Layers),
                    com.example.awancoalledger.NavTab("Notes", "notes", androidx.compose.material.icons.Icons.Outlined.Description),
                    com.example.awancoalledger.NavTab("Vehicles", "vehicle_tracker", androidx.compose.material.icons.Icons.Outlined.DirectionsCar)
                )
                val unusedModules = allModules.filter { it.route !in dockItems }
                if (unusedModules.isNotEmpty()) {
                    SettingsSection(title = "SHORTCUTS") {
                        unusedModules.forEachIndexed { index, tab ->
                            SettingsRow(icon = tab.icon, title = tab.title, value = "ntss " + tab.title, color = MaterialTheme.colorScheme.primary, isLast = index == unusedModules.size - 1) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 1000) {
                                    lastClickTime = currentTime
                                    onNavigateToShortcut(tab.route)
                                }
                            }
                        }
                    }
                }
                
                SettingsSection(title = "CATEGORIES") {
                    SettingsCategory.values().forEachIndexed { index, category ->
                        SettingsRow(icon = category.icon, title = category.title, value = category.subtitle, color = if (category == SettingsCategory.BACKUPS_AND_SYNC || category == SettingsCategory.BUSINESS_PROFILE) MaterialTheme.colorScheme.primary else category.color, isLast = index == SettingsCategory.values().size - 1) {
                            currentCategory = category
                        }
                    }
                }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                            Surface(onClick = { currentCategory = null }, shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.ArrowBack, contentDescription = "Back")
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(targetCategory.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                        when(targetCategory) {
                    SettingsCategory.BACKUPS_AND_SYNC -> {
            SettingsSection(title = "CLOUD BACKUP") {
                if (user == null || isAnonymous) {
                    PremiumAccountCard(
                            title = "Guest Mode",
                            subtitle = "Back up your data to the cloud",
                            icon = Icons.Outlined.CloudUpload,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { showLoginDialog = true }
                    )
                } else {
                    PremiumAccountCard(
                            title = user?.displayName ?: user?.email ?: "Account Sync Active",
                            subtitle = user?.email ?: "Your data is secured in the cloud",
                            icon = Icons.Outlined.CloudDone,
                            color = MaterialTheme.colorScheme.primary,
                            isLoggedIn = true,
                            onLogout = { showLogoutDialog = true }
                    )
                }
            }

            SettingsSection(title = "LOCAL BACKUPS & DATA") {
                SettingsRow(
                        Icons.Outlined.History,
                        "Auto-Backups",
                        "View Snapshots",
                        color = iOSOrange
                ) { showBackupsModal = true }
                SettingsRow(Icons.Outlined.CloudDownload, "Manual Export", color = MaterialTheme.colorScheme.primary) {
                    authViewModel.shareBackup(context) { error ->
                        Toast.makeText(context, "Export Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }
                SettingsRow(
                        Icons.Outlined.Upload,
                        "Restore Data",
                        color = ErrorRed,
                        textColor = ErrorRed,
                        isLast = true
                ) { restoreLauncher.launch(arrayOf("application/json", "*/*")) }
            }

            if (showLogoutDialog) {
                IOSAlertDialog(
                        title = "Sign Out",
                        message =
                                "Are you sure you want to sign out? Your data will remain safely in the cloud, but you won't be able to sync until you sign back in.",
                        onDismissRequest = { showLogoutDialog = false },
                        buttons = {
                            IOSDialogButton(text = "Cancel", onClick = { showLogoutDialog = false })
                            IOSDialogButton(
                                    text = "Sign Out",
                                    onClick = {
                                        showLogoutDialog = false
                                        authViewModel.signOut(context)
                                    },
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    isLast = true
                            )
                        }
                )
            }


                    }
                    SettingsCategory.BUSINESS_PROFILE -> {
            SettingsSection(title = "BUSINESS PROFILE") {
                SettingsRow(Icons.Outlined.Business, "Business Name", bizName, color = MaterialTheme.colorScheme.primary) {
                    editingField = "Business Name" to bizName
                }
                SettingsRow(Icons.Outlined.Person, "Owner Name", ownerName, color = iOSOrange) {
                    editingField = "Owner Name" to ownerName
                }
                SettingsRow(
                        Icons.Outlined.Public,
                        "Country Code",
                        "${countryConfig.name} (${countryConfig.code})",
                        color = MaterialTheme.colorScheme.primary
                ) {
                    showCountryDialog = true
                }
                SettingsRow(Icons.Outlined.Phone, "Phone", phone, color = SuccessGreen) {
                    editingField = "Phone" to phone
                }
                val isLogoUploading by viewModel.isLogoUploading.collectAsState()
                val isSignatureUploading by viewModel.isSignatureUploading.collectAsState()

                SettingsRow(
                        Icons.Outlined.Image,
                        "Company Logo",
                        color = ErrorRed,
                        control = {
                            if (logoUri != null || isLogoUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                                model = logoUri,
                                                contentDescription = null,
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                )
                                                                .padding(2.dp)
                                                                .clickable {
                                                                    logoPicker.launch(
                                                                            PickVisualMediaRequest(
                                                                                    ActivityResultContracts
                                                                                            .PickVisualMedia
                                                                                            .ImageOnly
                                                                            )
                                                                    )
                                                                },
                                                contentScale = ContentScale.Fit,
                                                alpha = if (isLogoUploading) 0.5f else 1f
                                        )
                                        if (isLogoUploading) {
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Surface(
                                            onClick = {
                                                viewModel.updateCompanyLogoUri(context, null)
                                            },
                                            modifier = Modifier.size(36.dp),
                                            shape = CircleShape,
                                            color = ErrorRed.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "Delete",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                        "Upload",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }
                ) {
                    if (logoUri == null && !isLogoUploading)
                            logoPicker.launch(
                                    PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                            )
                }
                SettingsRow(
                        Icons.Outlined.Edit,
                        "Signature",
                        color = iOSPurple,
                        isLast = true,
                        control = {
                            if (signatureUri != null || isSignatureUploading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(
                                                model = signatureUri,
                                                contentDescription = null,
                                                modifier =
                                                        Modifier.size(40.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                )
                                                                .padding(2.dp)
                                                                .clickable {
                                                                    signaturePicker.launch(
                                                                            PickVisualMediaRequest(
                                                                                    ActivityResultContracts
                                                                                            .PickVisualMedia
                                                                                            .ImageOnly
                                                                            )
                                                                    )
                                                                },
                                                contentScale = ContentScale.Fit,
                                                alpha = if (isSignatureUploading) 0.5f else 1f
                                        )
                                        if (isSignatureUploading) {
                                            CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Surface(
                                            onClick = {
                                                viewModel.updateSignatureUri(context, null)
                                            },
                                            modifier = Modifier.size(36.dp),
                                            shape = CircleShape,
                                            color = ErrorRed.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "Delete",
                                                    tint = ErrorRed,
                                                    modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                        "Upload",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }
                ) {
                    if (signatureUri == null && !isSignatureUploading)
                            signaturePicker.launch(
                                    PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                            )
                }
            }

                    }
                    SettingsCategory.PRIVACY_SECURITY -> {
            SettingsSection(title = "PRIVACY & SECURITY") {
                SettingsToggleRow(Icons.Outlined.Lock, "App Lock", appLock, color = MaterialTheme.colorScheme.primary) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (it) showPinKeypad = true else viewModel.toggleAppLock(false)
                }
                if (appLock) {
                    SettingsToggleRow(
                            Icons.Outlined.Fingerprint,
                            "Touch ID / Face ID",
                            biometrics,
                            color = SuccessGreen
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleBiometrics(it)
                    }
                    SettingsRow(
                            Icons.Outlined.Security,
                            "Change PIN",
                            "****",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            isLast = true
                    ) { showPinKeypad = true }
                }
            }

                    }
                    SettingsCategory.PREFERENCES -> {
            SettingsSection(title = "PREFERENCES") {
                AccentColorRow(viewModel, haptic)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                SettingsRow(
                        androidx.compose.material.icons.Icons.Outlined.ViewCarousel,
                        "Customize Dock",
                        "Rearrange bottom tabs",
                        color = SuccessGreen
                ) {
                    showDockCustomizationModal = true
                }
                SettingsToggleRow(
                        Icons.Outlined.DarkMode,
                        "Dark Mode",
                        darkMode,
                        color = iOSPurple
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleDarkMode(it)
                }
                SettingsToggleRow(
                        androidx.compose.material.icons.Icons.Outlined.BlurOn,
                        "Frosted Glass Dock",
                        frostedGlass,
                        color = MaterialTheme.colorScheme.primary
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFrostedGlass(it)
                }
                SettingsRow(
                        Icons.Outlined.SettingsSuggest,
                        "Oil Change Interval",
                        "${oilInterval} km",
                        color = iOSOrange,
                        isLast = true
                ) { editingField = "Oil Interval" to oilInterval.toString() }
            }

                    }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Modals
        editingField?.let { field ->
            EditFieldDialog(field, onDismiss = { editingField = null }) {
                when (field.first) {
                    "Business Name" -> viewModel.updateBusinessName(it)
                    "Owner Name" -> viewModel.updateOwnerName(it)
                    "Phone" -> viewModel.updateBusinessPhone(it)
                    "Oil Interval" -> viewModel.setOilChangeIntervalKm(it.toIntOrNull() ?: 3000)
                }
                editingField = null
            }
        }

        if (showPinKeypad) {
            PinKeypadModal(onDismiss = { showPinKeypad = false }) { pin ->
                viewModel.setPinAndEnable(pin)
                showPinKeypad = false
            }
        }

        if (showBackupsModal) {
            AutoBackupsModal(viewModel = viewModel, authViewModel = authViewModel, onDismiss = { showBackupsModal = false })
        }

        if (showDockCustomizationModal) {
            DockCustomizationDialog(
                currentItems = dockItems,
                onDismiss = { showDockCustomizationModal = false },
                onSave = { selected -> viewModel.updateDockItems(selected) }
            )
        }

        if (showLoginDialog) {
            LoginDialog(
                    viewModel = authViewModel,
                    onDismiss = { showLoginDialog = false },
                    onGoogleSignIn = {
                        showLoginDialog = false
                        googleSignInLauncher.launch(
                                authViewModel.getGoogleSignInClient(context).signInIntent
                        )
                    },
                    onAuthSuccess = { isNewUser ->
                        if (isNewUser) {
                            authViewModel.confirmLogin(true)
                            Toast.makeText(context, "Cloud Sync Enabled!", Toast.LENGTH_SHORT)
                                    .show()
                        } else {
                            showRestoreWarning = true
                        }
                    }
            )
        }

        if (showRestoreWarning) {
            IOSAlertDialog(
                    onDismissRequest = { showRestoreWarning = false },
                    title = "Restore Cloud Data?",
                    message =
                            "We found existing data on the cloud. This will REPLACE all guest data on this device with your cloud backup. A restart will be required.",
                    buttons = {
                        IOSDialogButton(text = "Cancel", onClick = { showRestoreWarning = false })
                        IOSDialogButton(
                                text = "Restore",
                                fontWeight = FontWeight.Bold,
                                isLast = true,
                                onClick = {
                                    showRestoreWarning = false
                                    authViewModel.confirmLogin(false)
                                    Toast.makeText(
                                                    context,
                                                    "Restoring from Cloud...",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                }
                        )
                    }
            )
        }

        if (showConflictDialog) {
            IOSAlertDialog(
                    onDismissRequest = { authViewModel.handleCollisionCancel() },
                    title = "Account Already Exists",
                    message =
                            "This Google account already has backed-up data. Do you want to Erase Guest Data & Restore Cloud Data or Cancel?",
                    buttons = {
                        IOSDialogButton(
                                text = "Cancel",
                                onClick = { authViewModel.handleCollisionCancel() }
                        )
                        IOSDialogButton(
                                text = "Restore Cloud Data",
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                isLast = true,
                                onClick = {
                                    authViewModel.handleCollisionRestore(context) {
                                        Toast.makeText(
                                                        context,
                                                        "Data Restored from Cloud!",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                    }
                                }
                        )
                    }
            )
        }

            var showCustomCountryDialog by remember { mutableStateOf(false) }
            var customCountryCodeInput by remember { mutableStateOf("") }

            if (showCustomCountryDialog) {
                com.example.awancoalledger.ui.components.IOSAlertDialog(
                    onDismissRequest = { showCustomCountryDialog = false },
                    title = "Custom Country Code",
                    message = "Enter your custom country code (e.g., +33)",
                    content = {
                        androidx.compose.material3.OutlinedTextField(
                            value = customCountryCodeInput,
                            onValueChange = { customCountryCodeInput = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                            singleLine = true
                        )
                    },
                    buttons = {
                        IOSDialogButton(
                            text = "Cancel",
                            onClick = { showCustomCountryDialog = false },
                            color = ErrorRed
                        )
                        IOSDialogButton(
                            text = "Save",
                            onClick = {
                                val code = customCountryCodeInput.trim()
                                if (code.isNotEmpty()) {
                                    val finalCode = if (code.startsWith("+")) code else "+$code"
                                    viewModel.updateCountryCode(finalCode)
                                }
                                showCustomCountryDialog = false
                                showCountryDialog = false
                            },
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            isLast = true
                        )
                    }
                )
            }

            if (showCountryDialog) {
                CountrySelectionModal(
                    onDismiss = { showCountryDialog = false },
                    onCustomRequest = { showCustomCountryDialog = true },
                    onCountrySelected = { config ->
                        viewModel.updateCountryCode(config.code)
                        showCountryDialog = false
                    }
                )
            }
        }
    }

@Composable
fun ProfileHeader(owner: String, biz: String, logoUri: android.net.Uri? = null, isUploading: Boolean = false, onLogoClick: () -> Unit = {}) {
    Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                    modifier =
                            Modifier.size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, iOSPurple))
                                    )
                                    .clickable { onLogoClick() },
                    contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else if (logoUri != null) {
                    coil.compose.AsyncImage(
                        model = logoUri,
                        contentDescription = "Company Logo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                            owner.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                        owner.uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                )
                Text(
                        biz,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        if (title.isNotEmpty()) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                letterSpacing = 0.5.sp
            )
        }
        Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) { Column { content() } }
    }
}

@Composable
fun SettingsRow(
        icon: ImageVector,
        title: String,
        value: String? = null,
        color: Color,
        isLast: Boolean = false,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        control: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClick()
                                }
                                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, color = textColor, fontSize = 17.sp, modifier = Modifier.weight(1f))

            if (control != null) {
                control()
            } else {
                if (value != null) {
                    Text(
                            value,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
        icon: ImageVector,
        title: String,
        checked: Boolean,
        color: Color,
        isLast: Boolean = false,
        onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(color),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
            )
            Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors =
                            SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = SuccessGreen
                            )
            )
        }
        if (!isLast) {
            HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun PinKeypadModal(onDismiss: () -> Unit, onComplete: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                            .clickable { onDismiss() },
            contentAlignment = Alignment.Center
    ) {
        Surface(
                modifier = Modifier.width(320.dp).clickable(enabled = false) {},
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Set New PIN", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { index ->
                        Box(
                                modifier =
                                        Modifier.size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (pin.length > index) MaterialTheme.colorScheme.primary
                                                        else Color.LightGray.copy(alpha = 0.3f)
                                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Keypad
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    keys.chunked(3).forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowKeys.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(64.dp))
                                } else {
                                    Surface(
                                            onClick = {
                                                if (key == "DEL") {
                                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                } else if (pin.length < 4) {
                                                    pin += key
                                                    if (pin.length == 4) onComplete(pin)
                                                }
                                            },
                                            modifier = Modifier.size(64.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "DEL") {
                                                Icon(
                                                        Icons.Outlined.Backspace,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Text(key, fontSize = 24.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBackupsModal(viewModel: SettingsViewModel, authViewModel: com.example.awancoalledger.viewmodel.features.AuthViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val snapshots by viewModel.autoSnapshots.collectAsState()
    var snapshotToRestore by remember { mutableStateOf<File?>(null) }
    var isCreating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshSnapshots(context) }

    ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp)) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-Snapshots", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                IconButton(
                        onClick = {
                            isCreating = true
                            viewModel.createManualSnapshot(context) { success ->
                                isCreating = false
                                if (success)
                                        Toast.makeText(
                                                        context,
                                                        "Snapshot Created",
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                            }
                        },
                        enabled = !isCreating
                ) {
                    if (isCreating)
                            CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                            )
                    else
                            Icon(
                                    Icons.Outlined.AddAPhoto,
                                    contentDescription = "Manual Snapshot",
                                    tint = MaterialTheme.colorScheme.primary
                            )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                    "Full database backups are saved every 6 hours. You can also create one manually.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (snapshots.isEmpty()) {
                Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                ) { Text("No snapshots found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(snapshots) { file ->
                        val dateStr =
                                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                                        .format(Date(file.lastModified()))
                        val sizeStr = "${file.length() / 1024} KB"
                        val isManual = file.name.startsWith("manual")

                        Surface(
                                onClick = { snapshotToRestore = file },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                        modifier =
                                                Modifier.size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                (if (isManual) iOSOrange
                                                                        else MaterialTheme.colorScheme.primary)
                                                                        .copy(alpha = 0.2f)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                            if (isManual) Icons.Outlined.Person
                                            else Icons.Outlined.AutoMode,
                                            null,
                                            tint = if (isManual) iOSOrange else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                            if (isManual) "Manual Snapshot • $sizeStr"
                                            else "Scheduled Snapshot • $sizeStr",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Outlined.Restore, null, tint = SuccessGreen)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                            ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface
                            )
            ) { Text("Done") }
        }

        snapshotToRestore?.let { snapshot ->
            IOSAlertDialog(
                    onDismissRequest = { snapshotToRestore = null },
                    title = "Restore Snapshot?",
                    message =
                            "This will replace all current app data with the data from this snapshot. This action cannot be undone.",
                    buttons = {
                        IOSDialogButton(text = "Cancel", onClick = { snapshotToRestore = null })
                        IOSDialogButton(
                                text = "Restore",
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                isLast = true,
                                onClick = {
                                    authViewModel.restoreSnapshot(
                                            context,
                                            snapshot,
                                            onSuccess = {
                                                Toast.makeText(
                                                                context,
                                                                "Database Restored!",
                                                                Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                onDismiss()
                                            },
                                            onError = { error ->
                                                Toast.makeText(
                                                                context,
                                                                "Restore Failed: $error",
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                            }
                                    )
                                    snapshotToRestore = null
                                }
                        )
                    }
            )
        }
    }
}

@Composable
fun EditFieldDialog(field: Pair<String, String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(field.second) }
    IOSAlertDialog(
            onDismissRequest = onDismiss,
            title = "Edit ${field.first}",
            content = {
                PremiumInput(
                        label = field.first,
                        value = value,
                        onValueChange = { value = it },
                        keyboardType =
                                if (field.first == "Phone" || field.first == "Oil Interval")
                                        androidx.compose.ui.text.input.KeyboardType.Number
                                else androidx.compose.ui.text.input.KeyboardType.Text
                )
            },
            buttons = {
                IOSDialogButton(text = "Cancel", onClick = onDismiss)
                IOSDialogButton(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        isLast = true,
                        onClick = { onSave(value) }
                )
            }
    )
}

@Composable
fun PremiumAccountCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        color: Color,
        isLoggedIn: Boolean = false,
        onClick: () -> Unit = {},
        onLogout: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (isLoggedIn) onLogout() else onClick()
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
            tonalElevation = 2.dp
    ) {
        Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier = Modifier
                            .size(60.dp)
                            .background(color = color.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                        subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelectionModal(
    onDismiss: () -> Unit,
    onCustomRequest: () -> Unit,
    onCountrySelected: (com.example.awancoalledger.data.CountryConfig) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select Country",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            androidx.compose.foundation.lazy.LazyColumn {
                items(com.example.awancoalledger.data.SUPPORTED_COUNTRIES.size) { index ->
                    val country = com.example.awancoalledger.data.SUPPORTED_COUNTRIES[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCountrySelected(country) }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = country.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = country.code,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < com.example.awancoalledger.data.SUPPORTED_COUNTRIES.size - 1) {
                        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    }
                }
                item {
                    androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCustomRequest() }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Other / Custom",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            androidx.compose.material.icons.Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Enter Custom Code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DockCustomizationDialog(
    currentItems: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val allTabs = listOf(
        com.example.awancoalledger.NavTab("Contacts", "parties", androidx.compose.material.icons.Icons.Outlined.People),
        com.example.awancoalledger.NavTab("Expenses", "expenses", androidx.compose.material.icons.Icons.Outlined.Payments),
        com.example.awancoalledger.NavTab("Inventory", "inventory", androidx.compose.material.icons.Icons.Outlined.Layers),
        com.example.awancoalledger.NavTab("Notes", "notes", androidx.compose.material.icons.Icons.Outlined.Description),
        com.example.awancoalledger.NavTab("Vehicles", "vehicle_tracker", androidx.compose.material.icons.Icons.Outlined.DirectionsCar)
    )
    
    var selectedItems by remember { mutableStateOf(currentItems) }

    com.example.awancoalledger.ui.components.IOSAlertDialog(
        onDismissRequest = onDismiss,
        title = "Customize Dock",
        message = "Select exactly 3 shortcuts.",
        content = {
            val selectedTabs = selectedItems.mapNotNull { route -> allTabs.find { it.route == route } }
            val unselectedTabs = allTabs.filter { !selectedItems.contains(it.route) }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // IN DOCK SECTION
                Text(
                    text = "IN DOCK",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    selectedTabs.forEachIndexed { index, tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Remove button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (selectedItems.size > 1) {
                                        selectedItems = selectedItems - tab.route
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.Outlined.RemoveCircle,
                                    contentDescription = "Remove",
                                    tint = com.example.awancoalledger.ui.theme.ErrorRed
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Icon(
                                tab.icon, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                tab.title, 
                                modifier = Modifier.weight(1f), 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            // Up/Down Arrows for reordering
                            if (index > 0) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            val newList = selectedItems.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            selectedItems = newList
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                            if (index < selectedTabs.size - 1) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            val newList = selectedItems.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index + 1]
                                            newList[index + 1] = temp
                                            selectedItems = newList
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(32.dp))
                            }
                        }
                        if (index < selectedTabs.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 56.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // MORE SHORTCUTS SECTION
                Text(
                    text = "MORE SHORTCUTS",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    if (unselectedTabs.isEmpty()) {
                        Text(
                            "All shortcuts are in dock",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    } else {
                        unselectedTabs.forEachIndexed { index, tab ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Add button
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        if (selectedItems.size < 3) {
                                            selectedItems = selectedItems + tab.route
                                        }
                                    },
                                    modifier = Modifier.size(24.dp),
                                    enabled = selectedItems.size < 3
                                ) {
                                    androidx.compose.material3.Icon(
                                        androidx.compose.material.icons.Icons.Outlined.AddCircle,
                                        contentDescription = "Add",
                                        tint = if (selectedItems.size < 3) com.example.awancoalledger.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                androidx.compose.material3.Icon(
                                    tab.icon, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha=0.6f), 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    tab.title, 
                                    modifier = Modifier.weight(1f), 
                                    color = MaterialTheme.colorScheme.onSurface, 
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                            if (index < unselectedTabs.size - 1) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 56.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            IOSDialogButton(
                text = "Cancel",
                onClick = onDismiss,
            )
            IOSDialogButton(
                text = "Save",
                onClick = {
                    if (selectedItems.size == 3) {
                        onSave(selectedItems)
                        onDismiss()
                    }
                },
                color = if (selectedItems.size == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f),
                fontWeight = FontWeight.Bold,
                isLast = true
            )
        }
    )
}

@Composable
fun AccentColorRow(viewModel: SettingsViewModel, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val currentHex by viewModel.accentColorHex.collectAsState()
    
    val accentColors = listOf(
        "#007AFF" to "Blue",
        "#56D25B" to "Green",
        "#FF9500" to "Orange",
        "#FF3B30" to "Red",
        "#5A595E" to "Grey",
        "#e3d714" to "Yellow"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text("Accent Color", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            accentColors.forEach { (hex, _) ->
                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch(e: Exception) { Color.Gray }
                val isSelected = currentHex == hex
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateAccentColorHex(hex)
                        }
                )
            }
        }
    }
}
