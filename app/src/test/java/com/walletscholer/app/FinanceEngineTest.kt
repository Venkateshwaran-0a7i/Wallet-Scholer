package com.walletscholer.app

import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceEngineTest {

    @Test
    fun testComputeBalance() {
        val txs = listOf(
            TransactionEntity(id = "1", type = "INCOME", categoryId = "salary", amount = 50000.0, date = "2026-08-01"),
            TransactionEntity(id = "2", type = "EXPENSE", categoryId = "rent", amount = 15000.0, date = "2026-08-02"),
            TransactionEntity(id = "3", type = "EXPENSE", categoryId = "food", amount = 5000.0, date = "2026-08-03"),
            TransactionEntity(id = "4", type = "EXPENSE", categoryId = "other", amount = 2000.0, date = "2026-08-04", status = "VOIDED")
        )
        val balance = FinanceEngine.computeBalance(txs)
        assertEquals(30000.0, balance, 0.001)
    }

    @Test
    fun testUtilizationPct() {
        assertEquals(50.0, FinanceEngine.utilizationPct(5000.0, 10000.0), 0.001)
        assertEquals(100.0, FinanceEngine.utilizationPct(10000.0, 10000.0), 0.001)
        assertEquals(120.0, FinanceEngine.utilizationPct(12000.0, 10000.0), 0.001)
    }

    @Test
    fun testThresholdFor() {
        assertEquals("50", FinanceEngine.thresholdFor(50.0))
        assertEquals("75", FinanceEngine.thresholdFor(75.0))
        assertEquals("90", FinanceEngine.thresholdFor(92.0))
        assertEquals("100", FinanceEngine.thresholdFor(100.0))
        assertEquals("EXCEEDED", FinanceEngine.thresholdFor(105.0))
        assertNull(FinanceEngine.thresholdFor(30.0))
    }

    @Test
    fun testSimpleInterest() {
        val res = FinanceEngine.simpleInterest(100000.0, 6.0, 2.0)
        assertNotNull(res)
        assertEquals(12000.0, res!!.interest, 0.01)
        assertEquals(112000.0, res.total, 0.01)
    }

    @Test
    fun testCompoundInterest() {
        val res = FinanceEngine.compoundInterest(100000.0, 7.0, 3.0, 12.0)
        assertNotNull(res)
        assertTrue(res!!.interest > 23000.0)
        assertTrue(res.total > 123000.0)
    }

    @Test
    fun testSipCalculation() {
        val res = FinanceEngine.sipFutureValue(5000.0, 12.0, 10.0)
        assertNotNull(res)
        assertEquals(600000.0, res!!.invested, 0.01)
        assertTrue(res.futureValue > 1100000.0)
        assertTrue(res.gain > 500000.0)
    }

    @Test
    fun testEmiCalculation() {
        val res = FinanceEngine.emiCalc(500000.0, 9.5, 5.0)
        assertNotNull(res)
        assertEquals(60L, res!!.totalMonths)
        assertTrue(res.emi in 10400.0..10600.0)
        assertTrue(res.totalInterest > 100000.0)
    }

    @Test
    fun testLoanAffordability() {
        val res = FinanceEngine.loanAffordability(15000.0, 9.5, 5.0)
        assertNotNull(res)
        assertTrue(res!!.principal > 700000.0)
    }

    @Test
    fun testPercentageCalculations() {
        assertEquals(300.0, FinanceEngine.percentageOf(15.0, 2000.0), 0.001)
        assertEquals(25.0, FinanceEngine.whatPercent(500.0, 2000.0)!!, 0.001)
        assertEquals(2300.0, FinanceEngine.percentChange(2000.0, 15.0, increase = true), 0.001)
        assertEquals(1700.0, FinanceEngine.percentChange(2000.0, 15.0, increase = false), 0.001)
    }

    @Test
    fun testSafeToSpend() {
        val safe = FinanceEngine.safeToSpend(
            balance = 45000.0,
            essentialRemaining = 15000.0,
            savingsCommitment = 8000.0,
            emiCommitment = 5000.0
        )
        assertEquals(17000.0, safe, 0.001)
    }
}
