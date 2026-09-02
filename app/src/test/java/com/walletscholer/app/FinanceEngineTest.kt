package com.walletscholer.app

import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

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
        assertEquals(BigDecimal.valueOf(12000.0).setScale(2), res.interestBd)
    }

    @Test
    fun testCompoundInterest() {
        val res = FinanceEngine.compoundInterest(100000.0, 7.0, 3.0, 12.0)
        assertNotNull(res)
        assertTrue(res!!.interest > 23000.0)
        assertTrue(res.total > 123000.0)
        assertTrue(res.interestBd > BigDecimal.valueOf(23000.0))
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

        // Deficit / Negative safe to spend returns 0.0
        val deficit = FinanceEngine.safeToSpend(
            balance = 10000.0,
            essentialRemaining = 15000.0,
            savingsCommitment = 5000.0
        )
        assertEquals(0.0, deficit, 0.001)
    }

    @Test
    fun testEdgeCasesAndZeroInterest() {
        // Zero interest SI
        val siZero = FinanceEngine.simpleInterest(50000.0, 0.0, 3.0)
        assertNotNull(siZero)
        assertEquals(0.0, siZero!!.interest, 0.001)
        assertEquals(50000.0, siZero.total, 0.001)

        // Invalid inputs
        assertNull(FinanceEngine.simpleInterest(0.0, 5.0, 1.0))
        assertNull(FinanceEngine.simpleInterest(1000.0, -1.0, 1.0))
        assertNull(FinanceEngine.simpleInterest(1000.0, 5.0, 0.0))

        // Zero return SIP
        val sipZero = FinanceEngine.sipFutureValue(1000.0, 0.0, 2.0)
        assertNotNull(sipZero)
        assertEquals(24000.0, sipZero!!.invested, 0.001)
        assertEquals(24000.0, sipZero.futureValue, 0.001)
        assertEquals(0.0, sipZero.gain, 0.001)

        // Invalid SIP inputs
        assertNull(FinanceEngine.sipFutureValue(0.0, 10.0, 2.0))
        assertNull(FinanceEngine.sipFutureValue(1000.0, 10.0, 0.0))

        // Zero rate EMI
        val emiZero = FinanceEngine.emiCalc(12000.0, 0.0, 1.0)
        assertNotNull(emiZero)
        assertEquals(1000.0, emiZero!!.emi, 0.01)
        assertEquals(12000.0, emiZero.totalPayment, 0.01)
        assertEquals(0.0, emiZero.totalInterest, 0.01)

        // Required monthly savings
        val reqSavings = FinanceEngine.requiredMonthlySavings(120000.0, 0.0, 1.0)
        assertNotNull(reqSavings)
        assertEquals(10000.0, reqSavings!!.monthly, 0.01)
        assertEquals(120000.0, reqSavings.totalDeposited, 0.01)
        assertEquals(0.0, reqSavings.gain, 0.01)

        // What percent division by zero
        assertNull(FinanceEngine.whatPercent(100.0, 0.0))

        // Utilization with zero allocation
        assertEquals(0.0, FinanceEngine.utilizationPct(0.0, 0.0), 0.001)
        assertEquals(100.0, FinanceEngine.utilizationPct(500.0, 0.0), 0.001)
    }
}
