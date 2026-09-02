package com.walletscholer.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.R
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.components.EmptyStateView
import com.walletscholer.app.ui.components.GhostAppButton
import com.walletscholer.app.ui.components.PillBadge
import com.walletscholer.app.ui.components.PrimaryAppButton
import com.walletscholer.app.ui.components.ProgressRing
import com.walletscholer.app.ui.components.SectionHeader
import com.walletscholer.app.ui.theme.WalletTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun MoreScreen(
    goals: List<GoalEntity>,
    settings: UserSettingsEntity?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSaveGoal: (id: String?, name: String, target: Double, current: Double, date: String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onUpdateSalaryCycle: (amount: Double, date: Int) -> Unit,
    onToggleAutoCreditSalary: (Boolean) -> Unit,
    onToggleNotifMaster: (Boolean) -> Unit,
    onToggleNotifThreshold: (String, Boolean) -> Unit,
    onToggleSync: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    isBiometricAvailable: Boolean,
    isGoogleSignedIn: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddGoalSheet by remember { mutableStateOf(false) }
    val signedIn = isGoogleSignedIn
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currentSettings = settings ?: UserSettingsEntity()

    var salaryAmountInput by remember(currentSettings.salaryAmount) {
        mutableStateOf(if (currentSettings.salaryAmount > 0) currentSettings.salaryAmount.toInt().toString() else "85000")
    }
    var salaryDateInput by remember(currentSettings.salaryDate) {
        mutableStateOf(currentSettings.salaryDate.toString())
    }

    // Thresholds map
    val notifMap = remember(currentSettings.notifThresholdsJson) {
        try {
            val json = JSONObject(currentSettings.notifThresholdsJson)
            mapOf(
                "75" to json.optBoolean("75", true),
                "90" to json.optBoolean("90", true),
                "100" to json.optBoolean("100", true),
                "Over budget" to json.optBoolean("Over budget", true)
            )
        } catch (_: Exception) {
            mapOf("75" to true, "90" to true, "100" to true, "Over budget" to true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 96.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = "Wallet Scholar Logo",
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "More",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile / Google Account Card
        AppCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WalletTheme.colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = WalletTheme.colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (signedIn) currentSettings.userName else "Local Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WalletTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (signedIn) currentSettings.userEmail else "Not signed in with Google",
                        fontSize = 12.sp,
                        color = WalletTheme.colors.subtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!signedIn) {
                PrimaryAppButton(
                    text = "Continue with Google",
                    leadingIcon = Icons.Default.Login,
                    onClick = onLoginClick
                )
            } else {
                GhostAppButton(
                    text = "Sign out",
                    onClick = {
                        onToggleSync(false)
                        onLogoutClick()
                    }
                )
            }
        }

        // Savings Goals Section
        SectionHeader(
            title = "Savings Goals",
            actionText = "+ New goal",
            onActionClick = { showAddGoalSheet = true }
        )

        if (goals.isEmpty()) {
            AppCard {
                EmptyStateView(
                    icon = Icons.Default.Savings,
                    title = "No goals yet",
                    subtitle = "Create a savings goal to track progress."
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.forEach { goal ->
                    val pct = FinanceEngine.clamp((goal.currentAmount / goal.targetAmount) * 100.0, 0.0, 100.0)
                    AppCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ProgressRing(
                                pct = pct,
                                size = 48.dp,
                                strokeWidth = 5.dp,
                                color = WalletTheme.colors.success
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WalletTheme.colors.text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${FinanceEngine.fmtMoney(goal.currentAmount)} / ${FinanceEngine.fmtMoney(goal.targetAmount)}${if (goal.targetDate.isNotBlank()) " · by ${goal.targetDate}" else ""}",
                                    fontSize = 12.sp,
                                    color = WalletTheme.colors.subtext
                                )
                            }
                            IconButton(
                                onClick = { onDeleteGoal(goal.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Goal",
                                    tint = WalletTheme.colors.faint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Salary Cycle Section
        SectionHeader(title = "Salary Cycle")
        AppCard {
            Text(
                text = "MONTHLY INCOME (₹)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = salaryAmountInput,
                onValueChange = { input ->
                    salaryAmountInput = input
                    val amt = input.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onUpdateSalaryCycle(amt, salaryDateInput.toIntOrNull() ?: 1)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SALARY DATE (DAY OF MONTH)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = salaryDateInput,
                onValueChange = { input ->
                    salaryDateInput = input
                    val day = input.toIntOrNull()
                    if (day != null && day in 1..31) {
                        onUpdateSalaryCycle(salaryAmountInput.toDoubleOrNull() ?: 85000.0, day)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-credit monthly salary",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = WalletTheme.colors.text
                    )
                    Text(
                        text = "Automatically add salary transaction each month",
                        fontSize = 12.sp,
                        color = WalletTheme.colors.subtext
                    )
                }
                Switch(
                    checked = currentSettings.autoCreditSalary,
                    onCheckedChange = { onToggleAutoCreditSalary(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WalletTheme.colors.accent,
                        uncheckedTrackColor = WalletTheme.colors.borderSoft
                    ),
                    modifier = Modifier.testTag("auto_credit_salary_switch")
                )
            }
        }

        // Notification Preferences
        SectionHeader(title = "Notifications")
        AppCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget alerts",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = WalletTheme.colors.text
                )
                Switch(
                    checked = currentSettings.notifMaster,
                    onCheckedChange = { onToggleNotifMaster(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WalletTheme.colors.accent,
                        uncheckedTrackColor = WalletTheme.colors.borderSoft
                    )
                )
            }

            listOf("75", "90", "100", "Over budget").forEach { th ->
                val label = if (th == "Over budget") th else "$th% used"
                val isChecked = notifMap[th] != false
                val isEnabled = currentSettings.notifMaster

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isEnabled) WalletTheme.colors.text else WalletTheme.colors.faint
                    )
                    Switch(
                        checked = isChecked,
                        enabled = isEnabled,
                        onCheckedChange = { onToggleNotifThreshold(th, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WalletTheme.colors.accent,
                            uncheckedTrackColor = WalletTheme.colors.borderSoft
                        )
                    )
                }
            }
        }

        // Backup & Sync Section
        SectionHeader(title = "Backup & Sync")
        AppCard {
            if (!signedIn) {
                Text(
                    text = "Sign in with Google to enable Sheets backup.",
                    fontSize = 13.sp,
                    color = WalletTheme.colors.subtext
                )
            } else if (!currentSettings.syncEnabled) {
                Text(
                    text = "Back up your Transactions, Budgets, Income, and Monthly Summary to a personal Google Sheet. Only these structured records are sent — nothing else.",
                    fontSize = 13.sp,
                    color = WalletTheme.colors.subtext
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = WalletTheme.colors.accent,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Creating your personal spreadsheet…",
                            fontSize = 13.5.sp,
                            color = WalletTheme.colors.subtext
                        )
                    }
                } else {
                    PrimaryAppButton(
                        text = "Enable Google Sheets Backup",
                        leadingIcon = Icons.Default.Cloud,
                        onClick = {
                            isSyncing = true
                            scope.launch {
                                delay(1200)
                                isSyncing = false
                                onToggleSync(true)
                            }
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PillBadge(
                        text = "Up to date",
                        textColor = WalletTheme.colors.success,
                        backgroundColor = WalletTheme.colors.successSoft,
                        icon = Icons.Default.Cloud
                    )
                    Text(
                        text = "Last synced: ${currentSettings.lastSyncTime.ifBlank { "Just now" }}",
                        fontSize = 11.5.sp,
                        color = WalletTheme.colors.faint
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wallet Scholar - Financial Backup",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WalletTheme.colors.text
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentSettings.userEmail,
                    fontSize = 12.sp,
                    color = WalletTheme.colors.subtext
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GhostAppButton(
                        text = "Sync now",
                        leadingIcon = Icons.Default.Refresh,
                        onClick = {
                            scope.launch {
                                onToggleSync(true)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    GhostAppButton(
                        text = "Disconnect",
                        leadingIcon = Icons.Default.CloudOff,
                        textColor = WalletTheme.colors.danger,
                        borderColor = WalletTheme.colors.danger.copy(alpha = 0.4f),
                        onClick = { onToggleSync(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Security Section
        SectionHeader(title = "Security")
        AppCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric & PIN App Lock",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isBiometricAvailable) WalletTheme.colors.text else WalletTheme.colors.faint
                    )
                    Text(
                        text = if (isBiometricAvailable)
                            "Require fingerprint, face, or PIN to open the app"
                        else
                            "No biometrics or lock enrolled on device",
                        fontSize = 12.sp,
                        color = WalletTheme.colors.faint
                    )
                }
                Switch(
                    checked = currentSettings.biometricLockEnabled,
                    onCheckedChange = { onToggleBiometric(it) },
                    enabled = isBiometricAvailable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WalletTheme.colors.accent,
                        uncheckedTrackColor = WalletTheme.colors.borderSoft
                    ),
                    modifier = Modifier.testTag("biometric_switch")
                )
            }
        }

        // Appearance Section
        SectionHeader(title = "Appearance")
        AppCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark theme",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = WalletTheme.colors.text
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = WalletTheme.colors.accent,
                        uncheckedTrackColor = WalletTheme.colors.borderSoft
                    )
                )
            }
        }
    }

    if (showAddGoalSheet) {
        AddGoalSheet(
            onDismiss = { showAddGoalSheet = false },
            onSave = { name, target, current, date ->
                onSaveGoal(null, name, target, current, date)
                showAddGoalSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, current: Double, date: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WalletTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New Savings Goal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "GOAL NAME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Emergency Fund or Vacation", color = WalletTheme.colors.faint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TARGET AMOUNT (₹)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = target,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        target = input
                    }
                },
                placeholder = { Text("0", color = WalletTheme.colors.faint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CURRENT SAVED (₹)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = current,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        current = input
                    }
                },
                placeholder = { Text("0", color = WalletTheme.colors.faint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TARGET DATE (OPTIONAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                placeholder = { Text("2026-12-31", color = WalletTheme.colors.faint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WalletTheme.colors.accent,
                    unfocusedBorderColor = WalletTheme.colors.border,
                    focusedTextColor = WalletTheme.colors.text,
                    unfocusedTextColor = WalletTheme.colors.text,
                    focusedContainerColor = WalletTheme.colors.appBg,
                    unfocusedContainerColor = WalletTheme.colors.appBg
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = WalletTheme.colors.danger,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryAppButton(
                text = "Create Goal",
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a goal name."
                        return@PrimaryAppButton
                    }
                    val tgt = target.toDoubleOrNull()
                    if (tgt == null || tgt <= 0) {
                        errorMessage = "Please enter a target amount greater than 0."
                        return@PrimaryAppButton
                    }
                    val cur = current.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), tgt, cur, date.trim())
                },
                testTag = "create_goal_button"
            )
        }
    }
}
