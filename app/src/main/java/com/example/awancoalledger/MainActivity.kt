package com.example.awancoalledger

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.app.AlarmManager
import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.awancoalledger.data.LedgerDatabase
import com.example.awancoalledger.data.LedgerRepository
import com.example.awancoalledger.data.SettingsRepository
import com.example.awancoalledger.ui.screens.LedgerDetailScreen
import com.example.awancoalledger.ui.screens.PartiesScreen
import com.example.awancoalledger.ui.screens.HomeScreen
import com.example.awancoalledger.ui.screens.ExpensesScreen
import com.example.awancoalledger.ui.screens.LockScreen
import com.example.awancoalledger.ui.screens.SettingsScreen
import com.example.awancoalledger.ui.screens.RemindersScreen
import com.example.awancoalledger.ui.screens.InventoryScreen
import com.example.awancoalledger.ui.screens.StockDetailScreen
import com.example.awancoalledger.ui.screens.FoldersScreen
import com.example.awancoalledger.ui.screens.NotesScreen
import com.example.awancoalledger.ui.screens.NoteEditorScreen
import com.example.awancoalledger.ui.screens.VehicleTrackerScreen
import com.example.awancoalledger.utils.ReminderScheduler
import com.example.awancoalledger.utils.NotificationHelper
import com.example.awancoalledger.ui.components.IOSAlertDialog
import com.example.awancoalledger.ui.components.IOSDialogButton
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.example.awancoalledger.ui.theme.AwanCoalLedgerTheme
import com.example.awancoalledger.ui.theme.DarkBg
import com.example.awancoalledger.ui.theme.PrimaryBlue
import com.example.awancoalledger.ui.theme.SurfaceColor
import com.example.awancoalledger.viewmodel.LedgerViewModel
import com.example.awancoalledger.viewmodel.LedgerViewModelFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = LedgerDatabase.getDatabase(this)
        val repository = LedgerRepository(database.ledgerDao())
        val settingsRepository = SettingsRepository(this)
        val scheduler = ReminderScheduler(this)
        
        // Firebase & Sync
        val firebaseManager = com.example.awancoalledger.data.FirebaseManager()
        val syncManager = com.example.awancoalledger.data.SyncManager(
            this,
            firebaseManager.firestore,
            repository,
            database.ledgerDao(),
            firebaseManager,
            settingsRepository
        )
        
        val factory = LedgerViewModelFactory(repository, settingsRepository, scheduler, firebaseManager, syncManager)
        
        // Initialize Notifications
        val notificationHelper = NotificationHelper(this)
        notificationHelper.initNotificationChannels()
        
        // Request Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Exact Alarm Permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }

        // Enable Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: LedgerViewModel = viewModel(factory = factory)
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            AwanCoalLedgerTheme(darkTheme = isDarkMode) {
                val isLocked by viewModel.isLocked.collectAsState()
                val ownerName by viewModel.ownerName.collectAsState()
                val biometricsEnabled by viewModel.isBiometricsEnabled.collectAsState()
                
                val needsRestart by viewModel.needsRestart.collectAsState()
                
                if (needsRestart) {
                    IOSAlertDialog(
                        onDismissRequest = { /* Force restart */ },
                        title = "Sync Successful",
                        message = "Your data has been restored from the cloud. A restart is required to load the new database records.",
                        buttons = {
                            IOSDialogButton(
                                text = "Restart Now",
                                color = com.example.awancoalledger.ui.theme.PrimaryBlue,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                isLast = true,
                                onClick = {
                                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            )
                        }
                    )
                }

                if (isLocked) {
                    LockScreen(
                        onUnlock = { pin -> viewModel.unlock(pin) },
                        ownerName = ownerName,
                        biometricEnabled = biometricsEnabled,
                        onBiometricTrigger = {
                            showBiometricPrompt {
                                viewModel.unlockWithBiometrics()
                            }
                        }
                    )
                } else {
                    MainAppContainer(viewModel)
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: LedgerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on detail screens
    val showBottomBar = currentRoute in listOf("summary", "parties", "expenses", "inventory", "notes", "settings")

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        NavTab("Home", "summary", Icons.Default.Home),
                        NavTab("Parties", "parties", Icons.Default.People),
                        NavTab("Expenses", "expenses", Icons.Default.Payments),
                        NavTab("Inventory", "inventory", Icons.Default.Layers),
                        NavTab("Notes", "notes", Icons.Default.Description),
                        NavTab("Settings", "settings", Icons.Default.Settings)
                    )

                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    navController.navigate(item.route) {
                                        popUpTo("summary") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController, 
                startDestination = "summary",
                enterTransition = {
                    fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.92f, animationSpec = tween(300, easing = FastOutLinearInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = 0.92f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) +
                    scaleOut(targetScale = 0.92f, animationSpec = tween(300, easing = FastOutLinearInEasing))
                }
            ) {
                composable("summary") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToLedger = { partyId ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.selectParty(partyId)
                            navController.navigate("ledger/$partyId")
                        },
                        onNavigateToParties = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("parties") {
                                popUpTo("summary") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToStock = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("inventory") {
                                popUpTo("summary") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToExpenses = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("expenses") {
                                popUpTo("summary") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToNotes = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("notes") {
                                popUpTo("summary") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToReminders = {
                            /*
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("reminders") {
                                popUpTo("summary") { saveState = true }
                                launchSingleTop = true
                            }
                            */
                        },
                        onNavigateToVehicleTracker = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            navController.navigate("vehicle_tracker")
                        }
                    )
                }
                composable("parties") {
                    PartiesScreen(viewModel, onNavigateToLedger = { partyId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.selectParty(partyId)
                        navController.navigate("ledger/$partyId")
                    })
                }
                composable("expenses") { ExpensesScreen(viewModel) }
                composable("inventory") { 
                    InventoryScreen(viewModel, onNavigateToStockDetail = { stockId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("stock_detail/$stockId")
                    }) 
                }
                composable("notes") { 
                    FoldersScreen(viewModel, onNavigateToFolder = { folderId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("folder_notes/$folderId")
                    }, onNavigateToEditor = { noteId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val route = if (noteId == null) "note_editor/new" else "note_editor/$noteId"
                        navController.navigate(route)
                    }) 
                }
                
                composable(
                    "folder_notes/{folderId}",
                    arguments = listOf(navArgument("folderId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val folderIdStr = backStackEntry.arguments?.getString("folderId") ?: "all"
                    val folderId = if (folderIdStr == "all") null else folderIdStr.toIntOrNull()
                    NotesScreen(viewModel, folderId, onNavigateToEditor = { noteId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val route = if (noteId == null) {
                            "note_editor/new" + (if (folderId != null) "?folderId=$folderId" else "")
                        } else {
                            "note_editor/$noteId"
                        }
                        navController.navigate(route)
                    }, onBack = { navController.popBackStack() })
                }
                
                composable("settings") { 
                    SettingsScreen(
                        viewModel, 
                        onNavigateToReminders = { navController.navigate("reminders") },
                        onNavigateToVehicleTracker = { navController.navigate("vehicle_tracker") }
                    ) 
                }
                composable("vehicle_tracker") { VehicleTrackerScreen(viewModel, onNavigateBack = { navController.popBackStack() }) }
                // composable("reminders") { RemindersScreen(viewModel, onBack = { navController.popBackStack() }) }
                
                composable(
                    "note_editor/{noteId}?folderId={folderId}",
                    arguments = listOf(
                        navArgument("noteId") { type = NavType.StringType },
                        navArgument("folderId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val noteIdStr = backStackEntry.arguments?.getString("noteId") ?: "new"
                    val noteId = if (noteIdStr == "new") null else noteIdStr.toIntOrNull()
                    val folderId = backStackEntry.arguments?.getString("folderId")?.toIntOrNull()
                    NoteEditorScreen(viewModel, noteId, folderId, onBack = {
                        navController.popBackStack()
                    })
                }
                
                composable(
                    "stock_detail/{stockId}",
                    arguments = listOf(navArgument("stockId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val stockId = backStackEntry.arguments?.getInt("stockId") ?: 0
                    StockDetailScreen(viewModel, stockId, onBack = {
                        navController.popBackStack()
                    })
                }
                
                composable(
                    "ledger/{partyId}",
                    arguments = listOf(navArgument("partyId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val partyId = backStackEntry.arguments?.getInt("partyId") ?: 0
                    LedgerDetailScreen(viewModel, partyId, onBack = {
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}

data class NavTab(val title: String, val route: String, val icon: ImageVector)

@Composable
fun PlaceholderScreen(title: String) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center) {
            Text("$title Screen Coming Soon", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}