package com.walletscholer.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.walletscholer.app.ui.navigation.GlassCapsuleDock
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
            Toast.makeText(this, "Sign-in failed (code ${e.statusCode}): ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            googleAccount = GoogleAuthManager.getLastSignedInAccount(this)
        } catch (_: Exception) {
            googleAccount = null
        }

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

    override fun onResume() {
        super.onResume()
        viewModel.refreshBankingApps()
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

    val settings by viewModel.settings.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

    val biometricAvailable = remember {
        BiometricLockManager.isAvailable(viewModel.getApplication())
    }

    // App Lock enforcement
    val isBiometricEnabled = settings?.biometricLockEnabled == true
    val requiresLock = isBiometricEnabled && !isAppUnlocked

    if (requiresLock) {
        BiometricLockScreen(onUnlocked = { viewModel.setAppUnlocked(true) })
        return
    }

    val transactions by viewModel.transactions.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsState()
    val bankingApps by viewModel.bankingApps.collectAsState()

    val allocations = remember(budget) { viewModel.parseAllocations(budget) }
    val customCategories = remember(budget) { viewModel.parseCustomCategories(budget) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WalletTheme.colors.appBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            GlassCapsuleDock(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it },
                isDarkTheme = isDarkTheme
            )
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
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.98f, animationSpec = tween(180)))
                        .togetherWith(fadeOut(animationSpec = tween(130)) + scaleOut(targetScale = 0.98f, animationSpec = tween(130)))
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(
                        transactions = transactions,
                        budgetEntity = budget,
                        allocations = allocations,
                        goals = goals,
                        settings = settings,
                        bankingApps = bankingApps,
                        selectedMonthKey = selectedMonthKey,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onResetMonth = { viewModel.resetToCurrentMonth() },
                        onAutoCreditSalaryToggle = { viewModel.toggleAutoCreditSalary(it) },
                        onCreditSalaryNow = { viewModel.creditSalaryNow() },
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
                        onToggleAutoCreditSalary = { viewModel.toggleAutoCreditSalary(it) },
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
