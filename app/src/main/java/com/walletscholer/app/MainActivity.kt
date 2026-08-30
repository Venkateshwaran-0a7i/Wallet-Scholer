package com.walletscholer.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.walletscholer.app.data.auth.BiometricLockManager
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.remote.GoogleAuthManager
import com.walletscholer.app.ui.navigation.NAV_ITEMS
import com.walletscholer.app.ui.navigation.Screen
import com.walletscholer.app.ui.screens.AccountSyncSheet
import com.walletscholer.app.ui.screens.AddTransactionSheet
import com.walletscholer.app.ui.screens.BiometricLockScreen
import com.walletscholer.app.ui.screens.BudgetScreen
import com.walletscholer.app.ui.screens.CalculatorScreen
import com.walletscholer.app.ui.screens.GeminiAdvisorSheet
import com.walletscholer.app.ui.screens.HomeScreen
import com.walletscholer.app.ui.screens.MoreScreen
import com.walletscholer.app.ui.screens.WalletScreen
import com.walletscholer.app.ui.theme.WalletScholarTheme
import com.walletscholer.app.ui.theme.WalletTheme
import com.walletscholer.app.ui.viewmodel.WalletViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: WalletViewModel by viewModels()

    // Source of truth for the real Google identity — backed by Play Services, survives
    // process death independently of Room (Room's userName/userEmail are just a display
    // cache kept in sync via viewModel.loginUser()).
    private var googleAccount by mutableStateOf<GoogleSignInAccount?>(null)

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            googleAccount = account
            viewModel.loginUser(
                name = account.displayName ?: account.email ?: "Google User",
                email = account.email ?: ""
            )
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            // Common causes: OAuth consent screen not set up, SHA-1/package name not
            // registered for this build, or the account isn't added as a test user yet.
            Toast.makeText(this, "Sign-in failed (code ${e.statusCode}): ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore a previous real sign-in session if one exists.
        googleAccount = GoogleAuthManager.getLastSignedInAccount(this)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            WalletScholarTheme(darkTheme = isDarkTheme) {
                MainAppContent(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    googleAccount = googleAccount,
                    onStartGoogleSignIn = {
                        signInLauncher.launch(GoogleAuthManager.getSignInIntent(this))
                    },
                    onGoogleSignOut = {
                        GoogleAuthManager.signOut(this) {
                            googleAccount = null
                            viewModel.logoutUser()
                            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: WalletViewModel,
    isDarkTheme: Boolean,
    googleAccount: GoogleSignInAccount?,
    onStartGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showAddTxSheet by remember { mutableStateOf(false) }
    var showAiAdvisorSheet by remember { mutableStateOf(false) }
    var showAccountSyncSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }

    // ── Biometric lock state ──────────────────────────────────────────────────
    val settings by viewModel.settings.collectAsState()
    val biometricAvailable = remember {
        BiometricLockManager.isAvailable(viewModel.getApplication())
    }
    // Start locked if biometric lock is enabled and hardware is available.
    // We use null as "undecided" so we don't flash the lock screen before
    // settings are loaded from Room.
    var isLocked by remember { mutableStateOf<Boolean?>(null) }
    val biometricEnabled = settings?.biometricLockEnabled == true && biometricAvailable

    // Once settings load, decide initial lock state
    androidx.compose.runtime.LaunchedEffect(biometricEnabled) {
        if (isLocked == null) {
            isLocked = biometricEnabled
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    val transactions by viewModel.transactions.collectAsState()
    // Re-collect settings since it's now declared above
    val goals by viewModel.goals.collectAsState()
    val budget by viewModel.budget.collectAsState()

    val allocations = remember(budget) { viewModel.parseAllocations(budget) }
    val customCategories = remember(budget) { viewModel.parseCustomCategories(budget) }

    // Show biometric lock screen when app is locked
    if (isLocked == true) {
        BiometricLockScreen(onUnlocked = { isLocked = false })
        return
    }
    // While settings are still null (first frame), show nothing to avoid flicker
    if (isLocked == null && biometricEnabled) return

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WalletTheme.colors.appBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = WalletTheme.colors.navBg,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NAV_ITEMS.forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WalletTheme.colors.accent,
                            selectedTextColor = WalletTheme.colors.accent,
                            unselectedIconColor = WalletTheme.colors.faint,
                            unselectedTextColor = WalletTheme.colors.faint,
                            indicatorColor = WalletTheme.colors.accentSoft
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.route}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == Screen.Home || currentScreen == Screen.Wallet) {
                FloatingActionButton(
                    onClick = {
                        editingTx = null
                        showAddTxSheet = true
                    },
                    shape = CircleShape,
                    containerColor = WalletTheme.colors.accent,
                    contentColor = WalletTheme.colors.accentText,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_crossfade"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        transactions = transactions,
                        budgetEntity = budget,
                        allocations = allocations,
                        goals = goals,
                        settings = settings,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
                        onOpenAddTransaction = {
                            editingTx = null
                            showAddTxSheet = true
                        },
                        onTransactionClick = { tx ->
                            editingTx = tx
                            showAddTxSheet = true
                        },
                        onOpenAiAdvisor = {
                            showAiAdvisorSheet = true
                        },
                        onOpenAccountSync = {
                            showAccountSyncSheet = true
                        }
                    )
                    Screen.Wallet -> WalletScreen(
                        transactions = transactions,
                        onTransactionClick = { tx ->
                            editingTx = tx
                            showAddTxSheet = true
                        }
                    )
                    Screen.Calculator -> CalculatorScreen()
                    Screen.Budget -> BudgetScreen(
                        budgetEntity = budget,
                        allocations = allocations,
                        customCategories = customCategories,
                        transactions = transactions,
                        onUpdateAllocations = { viewModel.updateBudgetAllocations(it) },
                        onAddCustomCategory = { name, amount, iconKey ->
                            viewModel.addCustomCategory(name, amount, iconKey)
                        },
                        onUpdateIncome = { viewModel.updateBudgetIncome(it) }
                    )
                    Screen.More -> MoreScreen(
                        goals = goals,
                        settings = settings,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
                        onSaveGoal = { id, name, target, current, date ->
                            viewModel.saveGoal(id, name, target, current, date)
                        },
                        onDeleteGoal = { id ->
                            viewModel.deleteGoal(id)
                        },
                        onUpdateSalaryCycle = { amount, date ->
                            viewModel.updateSalaryCycle(amount, date)
                        },
                        onToggleNotifMaster = { viewModel.toggleNotifMaster(it) },
                        onToggleNotifThreshold = { key, enabled ->
                            viewModel.toggleNotifThreshold(key, enabled)
                        },
                        onToggleSync = { viewModel.toggleSync(it) },
                        onToggleBiometric = { viewModel.toggleBiometricLock(it) },
                        isBiometricAvailable = biometricAvailable,
                        isGoogleSignedIn = googleAccount != null,
                        onLoginClick = onStartGoogleSignIn,
                        onLogoutClick = onGoogleSignOut
                    )
                }
            }
        }
    }

    if (showAddTxSheet) {
        AddTransactionSheet(
            editingTx = editingTx,
            customCategories = customCategories,
            onDismiss = {
                showAddTxSheet = false
                editingTx = null
            },
            onSave = { id, type, categoryId, amount, date, description ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    categoryId = categoryId,
                    amount = amount,
                    date = date,
                    description = description
                )
                showAddTxSheet = false
                editingTx = null
            },
            onVoid = { tx ->
                viewModel.voidTransaction(tx)
                showAddTxSheet = false
                editingTx = null
            }
        )
    }

    if (showAiAdvisorSheet) {
        GeminiAdvisorSheet(
            transactions = transactions,
            allocations = allocations,
            goals = goals,
            onDismiss = { showAiAdvisorSheet = false }
        )
    }

    if (showAccountSyncSheet) {
        AccountSyncSheet(
            settings = settings,
            transactions = transactions,
            goals = goals,
            allocations = allocations,
            googleAccount = googleAccount,
            onDismiss = { showAccountSyncSheet = false },
            onLoginClick = onStartGoogleSignIn,
            onLogout = onGoogleSignOut,
            onSaveSheetId = { id -> viewModel.updateSheetId(id) },
            onSyncResult = { success -> viewModel.setSyncResult(success) },
            onFreshStart = { preset -> viewModel.freshStart(preset) }
        )
    }
}

