package com.walletscholer.app

import com.walletscholer.app.data.banking.BankingAppHelper
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardUnitTest {

    @Test
    fun testDashboardBalanceCalculations() {
        val txs = listOf(
            TransactionEntity(id = "1", type = "INCOME", categoryId = "salary", amount = 75000.0, date = "2026-09-01"),
            TransactionEntity(id = "2", type = "EXPENSE", categoryId = "rent", amount = 22000.0, date = "2026-09-02"),
            TransactionEntity(id = "3", type = "EXPENSE", categoryId = "food", amount = 8500.0, date = "2026-09-03"),
            TransactionEntity(id = "4", type = "EXPENSE", categoryId = "shopping", amount = 4500.0, date = "2026-09-04"),
            TransactionEntity(id = "5", type = "EXPENSE", categoryId = "other", amount = 10000.0, date = "2026-09-05", status = "VOIDED")
        )

        val balance = FinanceEngine.computeBalance(txs)
        // 75000 - 22000 - 8500 - 4500 = 40000 (VOIDED tx 5 excluded)
        assertEquals(40000.0, balance, 0.001)

        val monthTxs = txs.filter { it.status == "ACTIVE" && it.date.startsWith("2026-09") }
        val monthIncome = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthExpense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        assertEquals(75000.0, monthIncome, 0.001)
        assertEquals(35000.0, monthExpense, 0.001)
    }

    @Test
    fun testCalendarMonthNavigation() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 1)
        val initialKey = sdf.format(cal.time)
        assertEquals("2026-09", initialKey)

        // Previous month
        cal.add(Calendar.MONTH, -1)
        val prevKey = sdf.format(cal.time)
        assertEquals("2026-08", prevKey)

        // Next month
        cal.add(Calendar.MONTH, 2)
        val nextKey = sdf.format(cal.time)
        assertEquals("2026-10", nextKey)
    }

    @Test
    fun testBankingCatalogList() {
        val catalog = BankingAppHelper.SUPPORTED_BANKING_APPS
        assertTrue(catalog.isNotEmpty())
        assertTrue(catalog.any { it.appName.contains("Google Pay", ignoreCase = true) })
        assertTrue(catalog.any { it.appName.contains("PhonePe", ignoreCase = true) })
        assertTrue(catalog.any { it.appName.contains("Paytm", ignoreCase = true) })
        assertTrue(catalog.any { it.appName.contains("SBI", ignoreCase = true) })
        assertTrue(catalog.any { it.appName.contains("HDFC", ignoreCase = true) })
    }
}
