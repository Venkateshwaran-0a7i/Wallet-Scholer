package com.walletscholer.app.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

object FinanceEngine {

    fun clamp(value: Double, minVal: Double, maxVal: Double): Double {
        if (value.isNaN()) return minVal
        return min(maxVal, max(minVal, value))
    }

    fun fmtMoney(amount: Double): String {
        val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        format.currency = java.util.Currency.getInstance("INR")
        return format.format(safeAmount).replace("INR", "₹").trim()
    }

    fun fmtMoneyPrecise(amount: Double): String {
        val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 2
        format.currency = java.util.Currency.getInstance("INR")
        return format.format(safeAmount).replace("INR", "₹").trim()
    }

    fun computeBalance(transactions: List<com.walletscholer.app.data.model.TransactionEntity>): Double {
        return transactions
            .filter { it.status == "ACTIVE" }
            .fold(0.0) { sum, tx ->
                sum + if (tx.type == "INCOME") tx.amount else -tx.amount
            }
    }

    fun utilizationPct(spent: Double, allocated: Double): Double {
        if (allocated <= 0.0 || spent.isNaN() || allocated.isNaN()) return if (spent > 0) 100.0 else 0.0
        return clamp((spent / allocated) * 100.0, 0.0, 999.0)
    }

    fun thresholdFor(pct: Double): String? {
        return when {
            pct > 100.0001 -> "EXCEEDED"
            pct >= 100.0 -> "100"
            pct >= 90.0 -> "90"
            pct >= 75.0 -> "75"
            pct >= 50.0 -> "50"
            else -> null
        }
    }

    data class InterestResult(
        val interest: Double,
        val total: Double
    )

    fun simpleInterest(p: Double, r: Double, t: Double): InterestResult? {
        if (p <= 0.0 || r < 0.0 || t <= 0.0) return null
        val si = (p * r * t) / 100.0
        return InterestResult(interest = si, total = p + si)
    }

    fun compoundInterest(p: Double, r: Double, t: Double, n: Double): InterestResult? {
        if (p <= 0.0 || r < 0.0 || t <= 0.0 || n <= 0.0) return null
        val rateDec = r / 100.0
        val a = p * (1.0 + rateDec / n).pow(n * t)
        return InterestResult(interest = a - p, total = a)
    }

    data class SipResult(
        val futureValue: Double,
        val invested: Double,
        val gain: Double
    )

    fun sipFutureValue(monthly: Double, annualReturnPct: Double, years: Double): SipResult? {
        if (monthly <= 0.0 || years <= 0.0) return null
        val n = (years * 12.0).roundToLong().toDouble()
        val invested = monthly * n
        if (annualReturnPct == 0.0) {
            return SipResult(futureValue = invested, invested = invested, gain = 0.0)
        }
        val i = (annualReturnPct / 12.0) / 100.0
        val fv = monthly * (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
        return SipResult(futureValue = fv, invested = invested, gain = fv - invested)
    }

    data class SavingsRequirementResult(
        val monthly: Double,
        val totalDeposited: Double,
        val gain: Double
    )

    fun requiredMonthlySavings(target: Double, annualReturnPct: Double, years: Double): SavingsRequirementResult? {
        if (target <= 0.0 || years <= 0.0) return null
        val n = (years * 12.0).roundToLong().toDouble()
        if (annualReturnPct == 0.0) {
            val m = target / n
            return SavingsRequirementResult(monthly = m, totalDeposited = target, gain = 0.0)
        }
        val i = (annualReturnPct / 12.0) / 100.0
        val factor = (((1.0 + i).pow(n) - 1.0) / i) * (1.0 + i)
        val monthly = target / factor
        val deposited = monthly * n
        return SavingsRequirementResult(monthly = monthly, totalDeposited = deposited, gain = target - deposited)
    }

    data class EmiResult(
        val emi: Double,
        val totalPayment: Double,
        val totalInterest: Double,
        val totalMonths: Long
    )

    fun emiCalc(p: Double, annualRatePct: Double, years: Double): EmiResult? {
        if (p <= 0.0 || years <= 0.0) return null
        val n = (years * 12.0).roundToLong().toDouble()
        val r = (annualRatePct) / 12.0 / 100.0
        val emi = if (r == 0.0) {
            p / n
        } else {
            val compound = (1.0 + r).pow(n)
            (p * r * compound) / (compound - 1.0)
        }
        val totalPayment = emi * n
        return EmiResult(
            emi = emi,
            totalPayment = totalPayment,
            totalInterest = totalPayment - p,
            totalMonths = n.toLong()
        )
    }

    data class LoanAffordabilityResult(
        val principal: Double,
        val totalPayment: Double,
        val totalInterest: Double
    )

    fun loanAffordability(desiredEmi: Double, annualRatePct: Double, years: Double): LoanAffordabilityResult? {
        if (desiredEmi <= 0.0 || years <= 0.0) return null
        val n = (years * 12.0).roundToLong().toDouble()
        val r = (annualRatePct) / 12.0 / 100.0
        val principal = if (r == 0.0) {
            desiredEmi * n
        } else {
            val compound = (1.0 + r).pow(n)
            (desiredEmi * (compound - 1.0)) / (r * compound)
        }
        val totalPayment = desiredEmi * n
        return LoanAffordabilityResult(
            principal = principal,
            totalPayment = totalPayment,
            totalInterest = totalPayment - principal
        )
    }

    fun percentageOf(x: Double, y: Double): Double = (x / 100.0) * y
    fun whatPercent(x: Double, y: Double): Double? = if (y == 0.0) null else (x / y) * 100.0
    fun percentChange(x: Double, pct: Double, increase: Boolean): Double {
        return if (increase) x * (1.0 + pct / 100.0) else x * (1.0 - pct / 100.0)
    }

    fun safeToSpend(balance: Double, essentialRemaining: Double, savingsCommitment: Double, emiCommitment: Double = 0.0): Double {
        val reserved = max(0.0, essentialRemaining) + max(0.0, savingsCommitment) + max(0.0, emiCommitment)
        return max(0.0, balance - reserved)
    }
}
