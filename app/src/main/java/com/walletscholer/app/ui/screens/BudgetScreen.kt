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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.data.model.BudgetEntity
import com.walletscholer.app.data.model.CategoryItem
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.components.CategoryIconBox
import com.walletscholer.app.ui.components.PillBadge
import com.walletscholer.app.ui.components.PrimaryAppButton
import com.walletscholer.app.ui.components.SectionHeader
import com.walletscholer.app.ui.theme.WalletTheme

@Composable
fun BudgetScreen(
    budgetEntity: BudgetEntity?,
    allocations: Map<String, Double>,
    customCategories: List<CategoryItem>,
    transactions: List<TransactionEntity>,
    onUpdateAllocations: (Map<String, Double>) -> Unit,
    onAddCustomCategory: (name: String, amount: Double, iconKey: String) -> Unit,
    onUpdateIncome: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }

    val currentMonthKey = budgetEntity?.monthKey?.takeIf { it.isNotBlank() }
        ?: java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
    val monthExpenseTxs = transactions.filter {
        it.status == "ACTIVE" && it.type == "EXPENSE" && it.date.startsWith(currentMonthKey)
    }

    val totalSpent = monthExpenseTxs.sumOf { it.amount }
    val rawIncome = budgetEntity?.income ?: 0.0
    val income = if (rawIncome > 0) rawIncome else 0.0
    val totalAllocated = allocations.values.sum()
    val exceedsIncome = income > 0 && totalAllocated > income

    val allExpenseCategories = (DefaultCategories.EXPENSE_CATEGORIES + customCategories).distinctBy { it.id }

    // Local state map for editing
    val editableAllocations = remember(allocations, isEditing) {
        mutableStateMapOf<String, String>().apply {
            allExpenseCategories.forEach { cat ->
                val amt = (allocations[cat.id] ?: 0.0).let { if (it.isNaN() || it.isInfinite()) 0.0 else it }
                put(cat.id, if (amt % 1 == 0.0) amt.toInt().toString() else amt.toString())
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budget",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )

            TextButton(
                onClick = {
                    if (isEditing) {
                        // Save changes
                        val newMap = allocations.toMutableMap()
                        editableAllocations.forEach { (k, v) ->
                            newMap[k] = v.toDoubleOrNull() ?: 0.0
                        }
                        onUpdateAllocations(newMap)
                    }
                    isEditing = !isEditing
                },
                modifier = Modifier.testTag("budget_edit_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = WalletTheme.colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEditing) "Done" else "Edit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WalletTheme.colors.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Top Summary Card
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Income",
                        fontSize = 11.sp,
                        color = WalletTheme.colors.subtext
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (income > 0) FinanceEngine.fmtMoney(income) else "Set income →",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (income > 0) WalletTheme.colors.text else WalletTheme.colors.accent
                        )
                    }
                }
                IconButton(
                    onClick = { showIncomeDialog = true },
                    modifier = Modifier.testTag("budget_income_edit")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit income",
                        tint = WalletTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allocated",
                    fontSize = 13.sp,
                    color = WalletTheme.colors.subtext
                )
                Text(
                    text = if (income > 0)
                        "${FinanceEngine.fmtMoney(totalAllocated)} / ${FinanceEngine.fmtMoney(income)}"
                    else
                        FinanceEngine.fmtMoney(totalAllocated),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WalletTheme.colors.text
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (income > 0) {
                val progressPct = FinanceEngine.clamp(totalAllocated / income, 0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { progressPct },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (exceedsIncome) WalletTheme.colors.danger else WalletTheme.colors.accent,
                    trackColor = WalletTheme.colors.borderSoft
                )
            }

            if (exceedsIncome) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WalletTheme.colors.danger,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = "Your allocated budget (${FinanceEngine.fmtMoney(totalAllocated)}) exceeds your income (${FinanceEngine.fmtMoney(income)}).",
                        fontSize = 12.5.sp,
                        color = WalletTheme.colors.danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row {
                Text(
                    text = "Spent so far: ",
                    fontSize = 12.5.sp,
                    color = WalletTheme.colors.subtext
                )
                Text(
                    text = FinanceEngine.fmtMoney(totalSpent),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
                if (income > 0) {
                    Text(
                        text = "  (${((totalSpent / income) * 100).toInt()}% of income)",
                        fontSize = 12.5.sp,
                        color = WalletTheme.colors.faint
                    )
                }
            }
        }

        SectionHeader(
            title = "Categories",
            actionText = if (isEditing) "+ Add category" else null,
            onActionClick = { showAddCategorySheet = true }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allExpenseCategories, key = { it.id }) { cat ->
                val alloc = allocations[cat.id] ?: 0.0
                val spent = monthExpenseTxs.filter { it.categoryId == cat.id }.sumOf { it.amount }
                val pct = FinanceEngine.utilizationPct(spent, alloc)

                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryIconBox(
                            iconKey = cat.iconKey,
                            isIncome = false,
                            size = 34.dp,
                            iconSize = 16.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = WalletTheme.colors.text
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${FinanceEngine.fmtMoney(spent)} of ${FinanceEngine.fmtMoney(alloc)}",
                                fontSize = 11.5.sp,
                                color = WalletTheme.colors.faint
                            )
                        }

                        if (isEditing) {
                            OutlinedTextField(
                                value = editableAllocations[cat.id] ?: "",
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        editableAllocations[cat.id] = input
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .width(90.dp)
                                    .testTag("alloc_input_${cat.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WalletTheme.colors.accent,
                                    unfocusedBorderColor = WalletTheme.colors.border,
                                    focusedTextColor = WalletTheme.colors.text,
                                    unfocusedTextColor = WalletTheme.colors.text,
                                    focusedContainerColor = WalletTheme.colors.appBg,
                                    unfocusedContainerColor = WalletTheme.colors.appBg
                                )
                            )
                        } else {
                            PillBadge(
                                text = "${pct.toInt()}%",
                                textColor = when {
                                    pct >= 100 -> WalletTheme.colors.danger
                                    pct >= 90 -> WalletTheme.colors.warn
                                    else -> WalletTheme.colors.success
                                },
                                backgroundColor = when {
                                    pct >= 100 -> WalletTheme.colors.dangerSoft
                                    pct >= 90 -> WalletTheme.colors.warnSoft
                                    else -> WalletTheme.colors.successSoft
                                }
                            )
                        }
                    }

                    if (!isEditing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val linePct = FinanceEngine.clamp(pct / 100.0, 0.0, 1.0).toFloat()
                        LinearProgressIndicator(
                            progress = { linePct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                pct >= 100 -> WalletTheme.colors.danger
                                pct >= 90 -> WalletTheme.colors.warn
                                else -> WalletTheme.colors.accent
                            },
                            trackColor = WalletTheme.colors.borderSoft
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    // Income edit dialog
    if (showIncomeDialog) {
        var incomeInput by remember { mutableStateOf(if (income > 0) income.toInt().toString() else "") }
        AlertDialog(
            onDismissRequest = { showIncomeDialog = false },
            title = {
                Text(
                    "Monthly Income",
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your total monthly income (salary + other sources).",
                        fontSize = 13.sp,
                        color = WalletTheme.colors.subtext
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = incomeInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                incomeInput = input
                            }
                        },
                        placeholder = { Text("e.g. 55000", color = WalletTheme.colors.faint) },
                        prefix = { Text("₹ ", color = WalletTheme.colors.subtext) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("income_input"),
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = incomeInput.toDoubleOrNull()
                        if (v != null && v >= 0) {
                            onUpdateIncome(v)
                            showIncomeDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_income_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = WalletTheme.colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncomeDialog = false }) {
                    Text("Cancel", color = WalletTheme.colors.subtext)
                }
            },
            containerColor = WalletTheme.colors.surface
        )
    }

    if (showAddCategorySheet) {
        AddCustomCategorySheet(
            onDismiss = { showAddCategorySheet = false },
            onAdd = { name, amount, iconKey ->
                onAddCustomCategory(name, amount, iconKey)
                showAddCategorySheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomCategorySheet(
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Double, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("target") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val iconChoices = listOf(
        "target" to Icons.Default.TrackChanges,
        "gift" to Icons.Default.CardGiftcard,
        "book" to Icons.Default.Laptop,
        "heart" to Icons.Default.MedicalServices,
        "shield" to Icons.Default.Security,
        "more" to Icons.Default.MoreHoriz
    )

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
                text = "Add Custom Category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CATEGORY NAME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Gym or Subscriptions", color = WalletTheme.colors.faint) },
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

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "BUDGET AMOUNT (₹)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = input
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

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ICON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconChoices.forEach { (key, icon) ->
                    val selected = selectedIconKey == key
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) WalletTheme.colors.accentSoft else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (selected) WalletTheme.colors.accent else WalletTheme.colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedIconKey = key },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

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
                text = "Add Category",
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a category name."
                        return@PrimaryAppButton
                    }
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter a budget amount greater than 0."
                        return@PrimaryAppButton
                    }
                    onAdd(name.trim(), amt, selectedIconKey)
                },
                testTag = "confirm_add_category_button"
            )
        }
    }
}
