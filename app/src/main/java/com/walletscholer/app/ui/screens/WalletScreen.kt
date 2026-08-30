package com.walletscholer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.components.EmptyStateView
import com.walletscholer.app.ui.components.TransactionItemRow
import com.walletscholer.app.ui.theme.WalletTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    transactions: List<TransactionEntity>,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val (filteredList, sortedDates, groupedByDate) = remember(transactions, selectedFilter) {
        val filtered = transactions.filter { tx ->
            when (selectedFilter) {
                "INCOME" -> tx.type == "INCOME" && tx.status == "ACTIVE"
                "EXPENSE" -> tx.type == "EXPENSE" && tx.status == "ACTIVE"
                "VOIDED" -> tx.status == "VOIDED"
                else -> true
            }
        }
        val grouped = filtered.groupBy { it.date }
        val dates = grouped.keys.sortedDescending()
        Triple(filtered, dates, grouped)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Wallet",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.text
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All", "INCOME" to "Income", "EXPENSE" to "Expense", "VOIDED" to "Voided").forEach { (fKey, fLabel) ->
                val isSelected = selectedFilter == fKey
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) WalletTheme.colors.accentSoft else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) WalletTheme.colors.accent else WalletTheme.colors.border
                    ),
                    modifier = Modifier.clickable { selectedFilter = fKey }
                ) {
                    Text(
                        text = fLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sortedDates.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AccountBalanceWallet,
                title = "No transactions yet",
                subtitle = "Tap + to add your first income or expense."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                sortedDates.forEach { dateKey ->
                    val txsInDate = groupedByDate[dateKey] ?: emptyList()
                    val displayDate = if (dateKey == todayStr) "Today" else dateKey

                    item(key = dateKey) {
                        Column {
                            Text(
                                text = displayDate.uppercase(),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.faint,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            AppCard {
                                txsInDate.forEachIndexed { index, tx ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = WalletTheme.colors.borderSoft,
                                            thickness = 1.dp
                                        )
                                    }
                                    TransactionItemRow(
                                        tx = tx,
                                        onClick = { onTransactionClick(tx) }
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
