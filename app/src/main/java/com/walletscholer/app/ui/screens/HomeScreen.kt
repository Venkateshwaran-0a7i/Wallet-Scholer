package com.walletscholer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun HomeScreen(
    transactions: List<TransactionEntity>,
    budgetEntity: BudgetEntity?,
    allocations: Map<String, Double>,
    goals: List<GoalEntity>,
    settings: UserSettingsEntity?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onOpenAddTransaction: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenAccountSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTxs = transactions.filter { it.status == "ACTIVE" }
    val balance = FinanceEngine.computeBalance(transactions)

    // Current month transactions
    val currentMonthKey = budgetEntity?.monthKey ?: ""
    val monthTxs = activeTxs.filter { it.date.startsWith(currentMonthKey) }
    val monthIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    val monthExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val totalAllocated = allocations.values.sum()
    val utilization = FinanceEngine.utilizationPct(monthExpense, totalAllocated)
    val remainingBudget = totalAllocated - monthExpense

    // Essential categories ("NEEDS")
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

    // Alerts
    data class BudgetAlert(val id: String, val name: String, val pct: Double, val th: String, val spent: Double, val alloc: Double)
    val alerts = mutableListOf<BudgetAlert>()
    DefaultCategories.EXPENSE_CATEGORIES.forEach { cat ->
        val alloc = allocations[cat.id] ?: 0.0
        if (alloc > 0.0) {
            val spent = monthTxs.filter { it.type == "EXPENSE" && it.categoryId == cat.id }.sumOf { it.amount }
            val pct = FinanceEngine.utilizationPct(spent, alloc)
            val th = FinanceEngine.thresholdFor(pct)
            if (th != null) {
                alerts.add(BudgetAlert(cat.id, cat.name, pct, th, spent, alloc))
            }
        }
    }
    alerts.sortByDescending { it.pct }

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
    insights.add(
        if (actualSavingsPct >= 20.0) {
            "You're saving ${actualSavingsPct.toInt()}% of income — at or above your 20% target."
        } else {
            "Your actual savings (${actualSavingsPct.toInt()}%) are below your 20% target this month."
        }
    )
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
            Column {
                Text(
                    text = "Welcome back, $userDisplayName 👋",
                    fontSize = 13.sp,
                    color = WalletTheme.colors.subtext
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Wallet Scholar",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
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

        Spacer(modifier = Modifier.height(18.dp))

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
                        // Simulated Gold Chip
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(WalletTheme.colors.accent.copy(alpha = 0.85f))
                        )
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

        // Monthly Spending Trends Chart by Category (Full interactive visualization)
        MonthlySpendingTrendsChart(
            transactions = transactions,
            allocations = allocations,
            currentMonthKey = currentMonthKey
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
        SectionHeader(title = "Insights")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.forEach { ins ->
                AppCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
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
