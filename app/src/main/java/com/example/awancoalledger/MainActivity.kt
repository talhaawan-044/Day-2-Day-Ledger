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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.example.awancoalledger.viewmodel.LedgerViewModelFactory
import androidx.compose.animation.*

import dev.chrisbanes.haze.*
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
            database,
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
            val settingsViewModel: com.example.awancoalledger.viewmodel.features.SettingsViewModel = viewModel(factory = factory)
            val authViewModel: com.example.awancoalledger.viewmodel.features.AuthViewModel = viewModel(factory = factory)
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val accentColorHex by settingsViewModel.accentColorHex.collectAsState()
            
            AwanCoalLedgerTheme(darkTheme = isDarkMode, accentColorHex = accentColorHex) {
                val isLocked by settingsViewModel.isLocked.collectAsState()
                val ownerName by settingsViewModel.ownerName.collectAsState()
                val biometricsEnabled by settingsViewModel.isBiometricsEnabled.collectAsState()
                
                val needsRestart by authViewModel.needsRestart.collectAsState()
                
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
                        onUnlock = { pin -> settingsViewModel.unlock(pin) },
                        ownerName = ownerName,
                        biometricEnabled = biometricsEnabled,
                        onBiometricTrigger = {
                            showBiometricPrompt {
                                settingsViewModel.unlockWithBiometrics()
                            }
                        }
                    )
                } else {
                    MainAppContainer(factory)
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
fun MainAppContainer(factory: LedgerViewModelFactory) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val hazeState = remember { HazeState() }
    val settingsViewModel: com.example.awancoalledger.viewmodel.features.SettingsViewModel = viewModel(factory = factory)
    val dashboardViewModel: com.example.awancoalledger.viewmodel.features.DashboardViewModel = viewModel(factory = factory)
    val frostedGlass by settingsViewModel.isFrostedGlassEnabled.collectAsState()
    val dockItems by settingsViewModel.dockItems.collectAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on detail screens
    val showBottomBar = currentRoute in listOf("summary", "parties", "expenses", "inventory", "notes", "settings", "vehicle_tracker")

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = if (frostedGlass) Modifier.hazeChild(state = hazeState, style = HazeStyle(backgroundColor = androidx.compose.ui.graphics.Color.Transparent, blurRadius = 15.dp, tint = dev.chrisbanes.haze.HazeTint(androidx.compose.ui.graphics.Color(0x33000000)))) else Modifier,
                    containerColor = if (frostedGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = if (frostedGlass) 0.dp else 8.dp
                ) {
                    val availableTabs = mapOf(
                        "parties" to NavTab("Contacts", "parties", Icons.Default.People),
                        "expenses" to NavTab("Expenses", "expenses", Icons.Default.Payments),
                        "inventory" to NavTab("Inventory", "inventory", Icons.Default.Layers),
                        "notes" to NavTab("Notes", "notes", Icons.Default.Description),
                        "vehicle_tracker" to NavTab("Vehicles", "vehicle_tracker", Icons.Default.LocalShipping)
                    )
                    
                    val items = mutableListOf<NavTab>()
                    items.add(NavTab("Home", "summary", Icons.Default.Home))
                    dockItems.forEach { route ->
                        availableTabs[route]?.let { items.add(it) }
                    }
                    items.add(NavTab("Settings", "settings", Icons.Default.Settings))

                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (item.route == "summary") {
                                        navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { 
                                                saveState = true 
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
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
        Box(modifier = Modifier.fillMaxSize().then(if (frostedGlass) Modifier.haze(hazeState) else Modifier.padding(padding))) {
            NavHost(
                navController = navController, 
                startDestination = "summary",
                enterTransition = { androidx.compose.animation.EnterTransition.None },
                exitTransition = { androidx.compose.animation.ExitTransition.None },
                popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                popExitTransition = { androidx.compose.animation.ExitTransition.None }
            ) {
                composable("summary") {
                    val vm: com.example.awancoalledger.viewmodel.features.DashboardViewModel = viewModel(factory = factory)
                    HomeScreen(
                        viewModel = vm,
                        onNavigateToLedger = { partyId ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
                    val vm: com.example.awancoalledger.viewmodel.features.PartiesViewModel = viewModel(factory = factory)
                    PartiesScreen(vm, onNavigateToLedger = { partyId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("ledger/$partyId")
                    })
                }
                composable("expenses") { val vm: com.example.awancoalledger.viewmodel.features.ExpensesViewModel = viewModel(factory = factory); ExpensesScreen(vm) }
                composable("inventory") { 
                    val vm: com.example.awancoalledger.viewmodel.features.StockViewModel = viewModel(factory = factory)
                    InventoryScreen(vm, onNavigateToStockDetail = { stockId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("stock_detail/$stockId")
                    }) 
                }
                composable("notes") { 
                    val vm: com.example.awancoalledger.viewmodel.features.NotesViewModel = viewModel(factory = factory)
                    FoldersScreen(vm, onNavigateToFolder = { folderId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        navController.navigate("folder_notes/$folderId")
                    }, onNavigateToEditor = { noteId ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        val route = if (noteId == null) "note_editor/new" else "note_editor/$noteId"
                        navController.navigate(route)
                    }, onBack = { navController.navigate("summary") { popUpTo(0) } }) 
                }
                
                composable(
                    "folder_notes/{folderId}",
                    arguments = listOf(navArgument("folderId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val folderIdStr = backStackEntry.arguments?.getString("folderId") ?: "all"
                    val folderId = if (folderIdStr == "all") null else folderIdStr.toIntOrNull()
                    val vm: com.example.awancoalledger.viewmodel.features.NotesViewModel = viewModel(factory = factory)
                    NotesScreen(vm, folderId, onNavigateToEditor = { noteId ->
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
                    val vm: com.example.awancoalledger.viewmodel.features.SettingsViewModel = viewModel(factory = factory)
                    val authVm: com.example.awancoalledger.viewmodel.features.AuthViewModel = viewModel(factory = factory)
                    SettingsScreen(
                        authVm,
                        vm, 
                        onNavigateToShortcut = { route -> navController.navigate(route) }
                    ) 
                }
                composable("vehicle_tracker") { val vm: com.example.awancoalledger.viewmodel.features.VehicleViewModel = viewModel(factory = factory); VehicleTrackerScreen(vm, onNavigateBack = { navController.popBackStack() }) }
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
                    val vm: com.example.awancoalledger.viewmodel.features.NotesViewModel = viewModel(factory = factory)
                    NoteEditorScreen(vm, noteId, folderId, onBack = {
                        navController.popBackStack()
                    })
                }
                
                composable(
                    "stock_detail/{stockId}",
                    arguments = listOf(navArgument("stockId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val stockId = backStackEntry.arguments?.getInt("stockId") ?: 0
                    val vm: com.example.awancoalledger.viewmodel.features.StockViewModel = viewModel(factory = factory)
                    StockDetailScreen(vm, stockId, onBack = {
                        navController.popBackStack()
                    })
                }
                
                composable(
                    "ledger/{partyId}",
                    arguments = listOf(navArgument("partyId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val partyId = backStackEntry.arguments?.getInt("partyId") ?: 0
                    val vm: com.example.awancoalledger.viewmodel.features.LedgerDetailViewModel = viewModel(factory = factory)
                    vm.selectParty(partyId)
                    LedgerDetailScreen(vm, partyId, onBack = {
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