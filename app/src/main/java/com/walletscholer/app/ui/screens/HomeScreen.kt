package com.walletscholer.app.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.walletscholer.app.R
import com.walletscholer.app.data.banking.BankingAppHelper
import com.walletscholer.app.data.banking.BankingAppInfo
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.components.MonthlySpendingTrendsChart
import com.walletscholer.app.ui.components.PillBadge
import com.walletscholer.app.ui.components.ProgressRing
import com.walletscholer.app.ui.components.SectionHeader
import com.walletscholer.app.ui.theme.WalletTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class BudgetAlertItem(
    val id: String,
    val name: String,
    val pct: Double,
    val th: String,
    val spent: Double,
    val alloc: Double
)

private data class HomeCalculatedData(
    val activeTxs: List<TransactionEntity>,
    val balance: Double,
    val currentMonthKey: String,
    val monthTxs: List<TransactionEntity>,
    val monthIncome: Double,
    val monthExpense: Double,
    val totalAllocated: Double,
    val utilization: Double,
    val remainingBudget: Double,
    val safeToSpend: Double,
    val actualSavingsPct: Double,
    val alerts: List<BudgetAlertItem>
)

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    budgetEntity: BudgetEntity?,
    allocations: Map<String, Double>,
    goals: List<GoalEntity>,
    settings: UserSettingsEntity?,
    bankingApps: List<BankingAppInfo>,
    selectedMonthKey: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onAutoCreditSalaryToggle: (Boolean) -> Unit,
    onCreditSalaryNow: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenAddTransaction: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenAccountSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentActualMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

    val formattedMonthLabel = remember(selectedMonthKey) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonthKey)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            } else selectedMonthKey
        } catch (_: Exception) {
            selectedMonthKey
        }
    }

    val homeData = remember(transactions, budgetEntity, allocations, selectedMonthKey) {
        val activeTxs = transactions.filter { it.status == "ACTIVE" }
        val balance = FinanceEngine.computeBalance(transactions)

        val monthTxs = activeTxs.filter { it.date.startsWith(selectedMonthKey) }
        val monthIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val totalAllocated = allocations.values.sum()
        val utilization = FinanceEngine.utilizationPct(monthExpense, totalAllocated)
        val remainingBudget = totalAllocated - monthExpense

        val essentialCatIds = DefaultCategories.EXPENSE_CATEGORIES.filter { it.group == "NEEDS" }.map { it.id }.toSet()
        val essentialRemaining = essentialCatIds.sumOf { id ->
            val alloc = allocations[id] ?: 0.0
            val spent = monthTxs.filter { it.type == "EXPENSE" && it.categoryId == id }.sumOf { it.amount }
            kotlin.math.max(0.0, alloc - spent)
        }
        val savingsCommitment = kotlin.math.max(0.0, allocations["savings"] ?: 0.0)
        val emiCommitment = kotlin.math.max(0.0, allocations["emi"] ?: 0.0)
        val safeToSpend = FinanceEngine.safeToSpend(balance, essentialRemaining, savingsCommitment, emiCommitment)
        val actualSavingsPct = if (monthIncome > 0) ((monthIncome - monthExpense) / monthIncome) * 100.0 else 0.0

        val alertsList = mutableListOf<BudgetAlertItem>()
        DefaultCategories.EXPENSE_CATEGORIES.forEach { cat ->
            val alloc = allocations[cat.id] ?: 0.0
            if (alloc > 0.0) {
                val spent = monthTxs.filter { it.type == "EXPENSE" && it.categoryId == cat.id }.sumOf { it.amount }
                val pct = FinanceEngine.utilizationPct(spent, alloc)
                val th = FinanceEngine.thresholdFor(pct)
                if (th != null) {
                    alertsList.add(BudgetAlertItem(cat.id, cat.name, pct, th, spent, alloc))
                }
            }
        }
        alertsList.sortByDescending { it.pct }

        HomeCalculatedData(
            activeTxs = activeTxs,
            balance = balance,
            currentMonthKey = selectedMonthKey,
            monthTxs = monthTxs,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            totalAllocated = totalAllocated,
            utilization = utilization,
            remainingBudget = remainingBudget,
            safeToSpend = safeToSpend,
            actualSavingsPct = actualSavingsPct,
            alerts = alertsList
        )
    }

    val balance = homeData.balance
    val monthIncome = homeData.monthIncome
    val monthExpense = homeData.monthExpense
    val totalAllocated = homeData.totalAllocated
    val utilization = homeData.utilization
    val remainingBudget = homeData.remainingBudget
    val safeToSpend = homeData.safeToSpend
    val actualSavingsPct = homeData.actualSavingsPct
    val monthTxs = homeData.monthTxs
    val alerts = homeData.alerts

    // Insights
    val insights = mutableListOf<String>()
    val byCat = mutableMapOf<String, Double>()
    monthTxs.filter { it.type == "EXPENSE" }.forEach {
        byCat[it.categoryId] = (byCat[it.categoryId] ?: 0.0) + it.amount
    }
    val topCatEntry = byCat.entries.maxByOrNull { it.value }
    if (topCatEntry != null && topCatEntry.value > 0) {
        val catName = DefaultCategories.findCategory(topCatEntry.key).name
        insights.add("Your biggest spend this month is $catName at ${FinanceEngine.fmtMoney(topCatEntry.value)}.")
    }
    if (monthIncome > 0) {
        insights.add(
            if (actualSavingsPct >= 20.0) {
                "You're saving ${actualSavingsPct.toInt()}% of income — at or above your 20% target."
            } else {
                "Your actual savings (${actualSavingsPct.toInt()}%) are below your 20% target this month."
            }
        )
    }
    if (alerts.isNotEmpty()) {
        insights.add("You have used ${alerts[0].pct.toInt()}% of your ${alerts[0].name} budget.")
    }

    val userDisplayName = settings?.userName?.takeIf { it.isNotBlank() } ?: "there"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 96.dp)
    ) {
        // App Header with Profile, Gemini AI, Sheets Sync & Theme switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_icon),
                    contentDescription = "Wallet Scholar Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, $userDisplayName 👋",
                        fontSize = 12.sp,
                        color = WalletTheme.colors.subtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Wallet Scholar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gemini AI Advisor Action Button
                IconButton(
                    onClick = onOpenAiAdvisor,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletTheme.colors.accentSoft)
                        .border(1.dp, WalletTheme.colors.accent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("ai_advisor_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI Advisor",
                        tint = WalletTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Google Sheets Sync Action Button
                IconButton(
                    onClick = onOpenAccountSync,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletTheme.colors.surface)
                        .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(12.dp))
                        .testTag("google_sheets_sync_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Google Sheets & Account",
                        tint = WalletTheme.colors.success,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Theme Toggle
                IconButton(
                    onClick = { onToggleDarkTheme(!isDarkTheme) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WalletTheme.colors.surface)
                        .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(12.dp))
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = WalletTheme.colors.text,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Calendar Navigation Bar (Month & Year Selector)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = WalletTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WalletTheme.colors.surfaceAlt)
                        .testTag("prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = WalletTheme.colors.text,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onResetMonth() }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = WalletTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formattedMonthLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.text
                    )

                    if (selectedMonthKey != currentActualMonthKey) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WalletTheme.colors.accentSoft,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = WalletTheme.colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Current",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WalletTheme.colors.accent
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WalletTheme.colors.surfaceAlt)
                        .testTag("next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = WalletTheme.colors.text,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Signature Wallet Card
        val cardBrush = if (isDarkTheme) {
            Brush.linearGradient(
                colors = listOf(Color(0xFF241E15), Color(0xFF19171C), Color(0xFF141318))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFFFCEFD6), Color(0xFFF6EEDD), Color(0xFFF3F1EC))
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.dp,
                    if (isDarkTheme) Color(0xFF332B1A) else Color(0xFFEADFBF),
                    RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(cardBrush)
                    .padding(22.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT BALANCE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletTheme.colors.subtext,
                            letterSpacing = 0.6.sp
                        )
                        // Detailed Gold Credit Card EMV Chip
                        Box(
                            modifier = Modifier
                                .size(width = 34.dp, height = 24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                    )
                                )
                                .border(1.dp, Color(0xFFFDE68A).copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 22.dp, height = 14.dp)
                                    .border(0.8.dp, Color(0xFF78350F).copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = FinanceEngine.fmtMoney(balance),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.text,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "SAFE TO SPEND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.faint,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FinanceEngine.fmtMoney(safeToSpend),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.success
                            )
                        }

                        Column {
                            Text(
                                text = "REMAINING BUDGET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.faint,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = FinanceEngine.fmtMoney(remainingBudget),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBudget < 0) WalletTheme.colors.danger else WalletTheme.colors.text
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Banking & UPI Apps Section
        SectionHeader(title = "Banking & UPI Apps")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = WalletTheme.colors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = WalletTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Installed & Supported Banking Apps",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletTheme.colors.text
                        )
                    }
                    Text(
                        text = "Tap to open",
                        fontSize = 11.5.sp,
                        color = WalletTheme.colors.faint
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bankingApps) { app ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = WalletTheme.colors.surfaceAlt,
                            modifier = Modifier
                                .width(94.dp)
                                .clickable {
                                    BankingAppHelper.launchBankingApp(context, app)
                                }
                                .border(
                                    1.dp,
                                    if (app.isInstalled) WalletTheme.colors.accent.copy(alpha = 0.4f) else WalletTheme.colors.border,
                                    RoundedCornerShape(14.dp)
                                )
                                .testTag("banking_app_${app.packageName.replace(".", "_")}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(WalletTheme.colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = remember(app.icon) {
                                        try {
                                            app.icon?.let { icon ->
                                                (icon as? BitmapDrawable)?.bitmap ?: icon.toBitmap(96, 96)
                                            }
                                        } catch (_: Throwable) {
                                            null
                                        }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = app.appName,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Paid,
                                            contentDescription = null,
                                            tint = WalletTheme.colors.accent,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = app.appName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WalletTheme.colors.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (app.isInstalled) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = WalletTheme.colors.success,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Open",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WalletTheme.colors.success
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = WalletTheme.colors.subtext,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Get App",
                                            fontSize = 10.sp,
                                            color = WalletTheme.colors.subtext
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Quick Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenAddTransaction,
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_transaction_quick_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalletTheme.colors.accent,
                    contentColor = WalletTheme.colors.accentText
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Entry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOpenAiAdvisor,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ask_gemini_ai_quick_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalletTheme.colors.surfaceAlt,
                    contentColor = WalletTheme.colors.text
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = WalletTheme.colors.accent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ask AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Salary Cycle Quick Card
        val salaryAmt = settings?.salaryAmount ?: 0.0
        val isAutoSalary = settings?.autoCreditSalary == true
        if (salaryAmt > 0.0) {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = null,
                                tint = WalletTheme.colors.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Monthly Salary Cycle",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.text
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${FinanceEngine.fmtMoney(salaryAmt)} / month · Auto-credit: ${if (isAutoSalary) "On" else "Off"}",
                            fontSize = 12.sp,
                            color = WalletTheme.colors.subtext
                        )
                    }

                    Button(
                        onClick = onCreditSalaryNow,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletTheme.colors.successSoft,
                            contentColor = WalletTheme.colors.success
                        ),
                        modifier = Modifier.testTag("credit_salary_quick_btn")
                    ) {
                        Text("Credit Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Month Spent
            AppCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = WalletTheme.colors.danger,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "This month spent",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WalletTheme.colors.subtext
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FinanceEngine.fmtMoney(monthExpense),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
            }

            // Month Income
            AppCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = WalletTheme.colors.success,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "This month income",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WalletTheme.colors.subtext
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FinanceEngine.fmtMoney(monthIncome),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Monthly Spending Trends Chart by Category
        MonthlySpendingTrendsChart(
            transactions = transactions,
            allocations = allocations,
            currentMonthKey = selectedMonthKey
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Budget Utilization Card
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgressRing(pct = utilization, size = 68.dp, strokeWidth = 7.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget utilization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WalletTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${FinanceEngine.fmtMoney(monthExpense)} of ${FinanceEngine.fmtMoney(totalAllocated)} used",
                        fontSize = 12.5.sp,
                        color = WalletTheme.colors.subtext
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PillBadge(
                        text = "${utilization.toInt()}% used",
                        textColor = when {
                            utilization >= 100 -> WalletTheme.colors.danger
                            utilization >= 90 -> WalletTheme.colors.warn
                            else -> WalletTheme.colors.success
                        },
                        backgroundColor = when {
                            utilization >= 100 -> WalletTheme.colors.dangerSoft
                            utilization >= 90 -> WalletTheme.colors.warnSoft
                            else -> WalletTheme.colors.successSoft
                        }
                    )
                }
            }
        }

        // Alerts Section
        if (alerts.isNotEmpty()) {
            SectionHeader(title = "Alerts")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                alerts.take(3).forEach { a ->
                    val exceeded = a.th == "EXCEEDED"
                    val alertColor = when {
                        exceeded || a.th == "100" -> WalletTheme.colors.danger
                        a.th == "90" -> WalletTheme.colors.warn
                        else -> WalletTheme.colors.accent
                    }
                    val alertBg = when {
                        exceeded || a.th == "100" -> WalletTheme.colors.dangerSoft
                        a.th == "90" -> WalletTheme.colors.warnSoft
                        else -> WalletTheme.colors.accentSoft
                    }
                    val msg = when {
                        exceeded -> "You exceeded your ${a.name} budget by ${FinanceEngine.fmtMoney(a.spent - a.alloc)}."
                        a.th == "100" -> "You have reached your ${a.name} budget limit."
                        a.th == "90" -> "You have almost reached your ${a.name} budget."
                        else -> "You have used ${a.pct.toInt()}% of your ${a.name} budget."
                    }

                    AppCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(alertBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = alertColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = msg,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = WalletTheme.colors.text,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Insights Section
        if (insights.isNotEmpty()) {
            SectionHeader(title = "Insights")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                insights.forEach { ins ->
                    AppCard {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = WalletTheme.colors.accent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = ins,
                                fontSize = 13.5.sp,
                                color = WalletTheme.colors.text,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Savings Goals Section
        if (goals.isNotEmpty()) {
            SectionHeader(title = "Savings Goals")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                goals.take(2).forEach { g ->
                    val pct = FinanceEngine.clamp((g.currentAmount / g.targetAmount) * 100.0, 0.0, 100.0)
                    AppCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ProgressRing(
                                pct = pct,
                                size = 52.dp,
                                strokeWidth = 6.dp,
                                color = WalletTheme.colors.success
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = g.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WalletTheme.colors.text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${FinanceEngine.fmtMoney(g.currentAmount)} of ${FinanceEngine.fmtMoney(g.targetAmount)} · ${pct.toInt()}%",
                                    fontSize = 12.5.sp,
                                    color = WalletTheme.colors.subtext
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
