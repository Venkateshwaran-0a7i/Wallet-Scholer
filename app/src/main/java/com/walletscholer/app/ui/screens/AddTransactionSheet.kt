package com.walletscholer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.walletscholer.app.data.model.CategoryItem
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.ui.components.GhostAppButton
import com.walletscholer.app.ui.components.PrimaryAppButton
import com.walletscholer.app.ui.components.getCategoryIcon
import com.walletscholer.app.ui.theme.WalletTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    editingTx: TransactionEntity?,
    customCategories: List<CategoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (id: String?, type: String, categoryId: String, amount: Double, date: String, description: String) -> Unit,
    onVoid: (TransactionEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(editingTx?.type ?: "EXPENSE") }
    var amountText by remember { mutableStateOf(editingTx?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var categoryId by remember { mutableStateOf(editingTx?.categoryId ?: if (type == "INCOME") "salary" else "food") }
    var dateText by remember { mutableStateOf(editingTx?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var description by remember { mutableStateOf(editingTx?.description ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = remember(type, customCategories) {
        if (type == "INCOME") DefaultCategories.INCOME_CATEGORIES else DefaultCategories.EXPENSE_CATEGORIES + customCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WalletTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WalletTheme.colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTx != null) "Edit Transaction" else "Add Transaction",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = WalletTheme.colors.subtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type Toggle (Expense / Income)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("EXPENSE" to "Expense", "INCOME" to "Income").forEach { (tpKey, tpLabel) ->
                    val selected = type == tpKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) WalletTheme.colors.accentSoft else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (selected) WalletTheme.colors.accent else WalletTheme.colors.border,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                type = tpKey
                                categoryId = if (tpKey == "INCOME") "salary" else "food"
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tpLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            Text(
                text = "AMOUNT (₹)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = input
                    }
                },
                placeholder = { Text("0.00", color = WalletTheme.colors.faint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            Text(
                text = "CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val active = categoryId == cat.id
                    val icon = getCategoryIcon(cat.iconKey)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (active) WalletTheme.colors.accentSoft else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (active) WalletTheme.colors.accent else WalletTheme.colors.border
                        ),
                        modifier = Modifier.clickable { categoryId = cat.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) WalletTheme.colors.accent else WalletTheme.colors.subtext
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date
            Text(
                text = "DATE (YYYY-MM-DD)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                placeholder = { Text("2026-08-29", color = WalletTheme.colors.faint) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_input"),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "DESCRIPTION (OPTIONAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.subtext,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("e.g. Groceries or Taxi", color = WalletTheme.colors.faint) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("description_input"),
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WalletTheme.colors.dangerSoft)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WalletTheme.colors.danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = WalletTheme.colors.danger,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            PrimaryAppButton(
                text = if (editingTx != null) "Save Changes" else "Save Transaction",
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter an amount greater than 0."
                        return@PrimaryAppButton
                    }
                    if (dateText.isBlank()) {
                        errorMessage = "Please enter a valid date."
                        return@PrimaryAppButton
                    }
                    errorMessage = null
                    onSave(editingTx?.id, type, categoryId, amt, dateText.trim(), description.trim())
                    onDismiss()
                },
                testTag = "save_transaction_button"
            )

            // Void Button if editing
            if (editingTx != null && editingTx.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(10.dp))
                GhostAppButton(
                    text = "Void Transaction",
                    onClick = {
                        onVoid(editingTx)
                        onDismiss()
                    },
                    textColor = WalletTheme.colors.danger,
                    borderColor = WalletTheme.colors.danger.copy(alpha = 0.5f),
                    testTag = "void_transaction_button"
                )
            }
        }
    }
}
