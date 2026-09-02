package com.walletscholer.app.domain

import com.walletscholer.app.domain.result.AppResult
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object FinanceEngine {

    val MATH_CONTEXT: MathContext = MathContext(16, RoundingMode.HALF_EVEN)
    private val SCALE_MONEY = 2
    private val SCALE_INTEREST = 4

    fun clamp(value: Double, minVal: Double, maxVal: Double): Double {
        if (value.isNaN()) return minVal
        return kotlin.math.min(maxVal, kotlin.math.max(minVal, value))
    }

    fun toBigDecimalOrZero(value: Double): BigDecimal {
        return if (value.isNaN() || value.isInfinite()) BigDecimal.ZERO else BigDecimal.valueOf(value)
    }

    fun fmtMoney(amount: Double): String = fmtMoney(toBigDecimalOrZero(amount))

    fun fmtMoney(amount: BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance("INR")
        return format.format(amount.setScale(0, RoundingMode.HALF_UP).toDouble())
            .replace("INR", "₹")
            .trim()
    }

    fun fmtMoneyPrecise(amount: Double): String = fmtMoneyPrecise(toBigDecimalOrZero(amount))

    fun fmtMoneyPrecise(amount: BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        format.currency = Currency.getInstance("INR")
        return format.format(amount.setScale(2, RoundingMode.HALF_UP).toDouble())
            .replace("INR", "₹")
            .trim()
    }

    fun computeBalance(transactions: List<com.walletscholer.app.data.model.TransactionEntity>): Double {
        var balance = BigDecimal.ZERO
        for (tx in transactions) {
            if (tx.status == "ACTIVE") {
                val amt = toBigDecimalOrZero(tx.amount)
                balance = if (tx.type == "INCOME") {
                    balance.add(amt)
                } else {
                    balance.subtract(amt)
                }
            }
        }
        return balance.setScale(SCALE_MONEY, RoundingMode.HALF_EVEN).toDouble()
    }

    fun utilizationPct(spent: Double, allocated: Double): Double {
        if (allocated <= 0.0 || spent.isNaN() || allocated.isNaN()) return if (spent > 0) 100.0 else 0.0
        val spentBd = toBigDecimalOrZero(spent)
        val allocBd = toBigDecimalOrZero(allocated)
        if (allocBd.compareTo(BigDecimal.ZERO) <= 0) return if (spentBd > BigDecimal.ZERO) 100.0 else 0.0
        val pct = spentBd.multiply(BigDecimal.valueOf(100)).divide(allocBd, 2, RoundingMode.HALF_UP).toDouble()
        return clamp(pct, 0.0, 999.0)
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
        val total: Double,
        val interestBd: BigDecimal = BigDecimal.valueOf(interest),
        val totalBd: BigDecimal = BigDecimal.valueOf(total)
    )

    fun simpleInterest(p: Double, r: Double, t: Double): InterestResult? {
        if (p <= 0.0 || r < 0.0 || t <= 0.0) return null
        val pBd = BigDecimal.valueOf(p)
        val rBd = BigDecimal.valueOf(r)
        val tBd = BigDecimal.valueOf(t)

        // SI = (P * R * T) / 100
        val si = pBd.multiply(rBd).multiply(tBd).divide(BigDecimal.valueOf(100), SCALE_MONEY, RoundingMode.HALF_EVEN)
        val total = pBd.add(si).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return InterestResult(
            interest = si.toDouble(),
            total = total.toDouble(),
            interestBd = si,
            totalBd = total
        )
    }

    fun compoundInterest(p: Double, r: Double, t: Double, n: Double): InterestResult? {
        if (p <= 0.0 || r < 0.0 || t <= 0.0 || n <= 0.0) return null
        val pBd = BigDecimal.valueOf(p)
        val rateDec = r / 100.0
        val totalCompoundFactor = Math.pow(1.0 + rateDec / n, n * t)
        val aBd = pBd.multiply(BigDecimal.valueOf(totalCompoundFactor)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        val interestBd = aBd.subtract(pBd).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return InterestResult(
            interest = interestBd.toDouble(),
            total = aBd.toDouble(),
            interestBd = interestBd,
            totalBd = aBd
        )
    }

    data class SipResult(
        val futureValue: Double,
        val invested: Double,
        val gain: Double,
        val futureValueBd: BigDecimal = BigDecimal.valueOf(futureValue),
        val investedBd: BigDecimal = BigDecimal.valueOf(invested),
        val gainBd: BigDecimal = BigDecimal.valueOf(gain)
    )

    fun sipFutureValue(monthly: Double, annualReturnPct: Double, years: Double): SipResult? {
        if (monthly <= 0.0 || years <= 0.0) return null
        val totalMonths = (years * 12.0).toLong()
        val mBd = BigDecimal.valueOf(monthly)
        val investedBd = mBd.multiply(BigDecimal.valueOf(totalMonths)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        if (annualReturnPct == 0.0) {
            return SipResult(
                futureValue = investedBd.toDouble(),
                invested = investedBd.toDouble(),
                gain = 0.0,
                futureValueBd = investedBd,
                investedBd = investedBd,
                gainBd = BigDecimal.ZERO
            )
        }

        val i = (annualReturnPct / 12.0) / 100.0
        val factor = ((Math.pow(1.0 + i, totalMonths.toDouble()) - 1.0) / i) * (1.0 + i)
        val fvBd = mBd.multiply(BigDecimal.valueOf(factor)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        val gainBd = fvBd.subtract(investedBd).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return SipResult(
            futureValue = fvBd.toDouble(),
            invested = investedBd.toDouble(),
            gain = gainBd.toDouble(),
            futureValueBd = fvBd,
            investedBd = investedBd,
            gainBd = gainBd
        )
    }

    data class SavingsRequirementResult(
        val monthly: Double,
        val totalDeposited: Double,
        val gain: Double,
        val monthlyBd: BigDecimal = BigDecimal.valueOf(monthly),
        val totalDepositedBd: BigDecimal = BigDecimal.valueOf(totalDeposited),
        val gainBd: BigDecimal = BigDecimal.valueOf(gain)
    )

    fun requiredMonthlySavings(target: Double, annualReturnPct: Double, years: Double): SavingsRequirementResult? {
        if (target <= 0.0 || years <= 0.0) return null
        val totalMonths = (years * 12.0).toLong()
        val targetBd = BigDecimal.valueOf(target).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        if (annualReturnPct == 0.0) {
            val mBd = targetBd.divide(BigDecimal.valueOf(totalMonths), SCALE_MONEY, RoundingMode.HALF_EVEN)
            return SavingsRequirementResult(
                monthly = mBd.toDouble(),
                totalDeposited = targetBd.toDouble(),
                gain = 0.0,
                monthlyBd = mBd,
                totalDepositedBd = targetBd,
                gainBd = BigDecimal.ZERO
            )
        }

        val i = (annualReturnPct / 12.0) / 100.0
        val factor = ((Math.pow(1.0 + i, totalMonths.toDouble()) - 1.0) / i) * (1.0 + i)
        val monthlyBd = targetBd.divide(BigDecimal.valueOf(factor), SCALE_MONEY, RoundingMode.HALF_EVEN)
        val depositedBd = monthlyBd.multiply(BigDecimal.valueOf(totalMonths)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        val gainBd = targetBd.subtract(depositedBd).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return SavingsRequirementResult(
            monthly = monthlyBd.toDouble(),
            totalDeposited = depositedBd.toDouble(),
            gain = gainBd.toDouble(),
            monthlyBd = monthlyBd,
            totalDepositedBd = depositedBd,
            gainBd = gainBd
        )
    }

    data class EmiResult(
        val emi: Double,
        val totalPayment: Double,
        val totalInterest: Double,
        val totalMonths: Long,
        val emiBd: BigDecimal = BigDecimal.valueOf(emi),
        val totalPaymentBd: BigDecimal = BigDecimal.valueOf(totalPayment),
        val totalInterestBd: BigDecimal = BigDecimal.valueOf(totalInterest)
    )

    fun emiCalc(p: Double, annualRatePct: Double, years: Double): EmiResult? {
        if (p <= 0.0 || years <= 0.0) return null
        val totalMonths = (years * 12.0).toLong()
        val pBd = BigDecimal.valueOf(p)
        val r = (annualRatePct / 12.0) / 100.0

        val emiBd = if (r == 0.0) {
            pBd.divide(BigDecimal.valueOf(totalMonths), SCALE_MONEY, RoundingMode.HALF_EVEN)
        } else {
            val compound = Math.pow(1.0 + r, totalMonths.toDouble())
            val emiVal = (p * r * compound) / (compound - 1.0)
            BigDecimal.valueOf(emiVal).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        }

        val totalPaymentBd = emiBd.multiply(BigDecimal.valueOf(totalMonths)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        val totalInterestBd = totalPaymentBd.subtract(pBd).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return EmiResult(
            emi = emiBd.toDouble(),
            totalPayment = totalPaymentBd.toDouble(),
            totalInterest = totalInterestBd.toDouble(),
            totalMonths = totalMonths,
            emiBd = emiBd,
            totalPaymentBd = totalPaymentBd,
            totalInterestBd = totalInterestBd
        )
    }

    data class LoanAffordabilityResult(
        val principal: Double,
        val totalPayment: Double,
        val totalInterest: Double,
        val principalBd: BigDecimal = BigDecimal.valueOf(principal),
        val totalPaymentBd: BigDecimal = BigDecimal.valueOf(totalPayment),
        val totalInterestBd: BigDecimal = BigDecimal.valueOf(totalInterest)
    )

    fun loanAffordability(desiredEmi: Double, annualRatePct: Double, years: Double): LoanAffordabilityResult? {
        if (desiredEmi <= 0.0 || years <= 0.0) return null
        val totalMonths = (years * 12.0).toLong()
        val emiBd = BigDecimal.valueOf(desiredEmi)
        val r = (annualRatePct / 12.0) / 100.0

        val principalBd = if (r == 0.0) {
            emiBd.multiply(BigDecimal.valueOf(totalMonths)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        } else {
            val compound = Math.pow(1.0 + r, totalMonths.toDouble())
            val pVal = (desiredEmi * (compound - 1.0)) / (r * compound)
            BigDecimal.valueOf(pVal).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        }

        val totalPaymentBd = emiBd.multiply(BigDecimal.valueOf(totalMonths)).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
        val totalInterestBd = totalPaymentBd.subtract(principalBd).setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)

        return LoanAffordabilityResult(
            principal = principalBd.toDouble(),
            totalPayment = totalPaymentBd.toDouble(),
            totalInterest = totalInterestBd.toDouble(),
            principalBd = principalBd,
            totalPaymentBd = totalPaymentBd,
            totalInterestBd = totalInterestBd
        )
    }

    fun percentageOf(x: Double, y: Double): Double {
        val xBd = BigDecimal.valueOf(x)
        val yBd = BigDecimal.valueOf(y)
        return xBd.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_EVEN)
            .multiply(yBd)
            .setScale(SCALE_MONEY, RoundingMode.HALF_EVEN)
            .toDouble()
    }

    fun whatPercent(x: Double, y: Double): Double? {
        if (y == 0.0) return null
        val xBd = BigDecimal.valueOf(x)
        val yBd = BigDecimal.valueOf(y)
        return xBd.multiply(BigDecimal.valueOf(100))
            .divide(yBd, 4, RoundingMode.HALF_EVEN)
            .toDouble()
    }

    fun percentChange(x: Double, pct: Double, increase: Boolean): Double {
        val xBd = BigDecimal.valueOf(x)
        val pctBd = BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_EVEN)
        val delta = xBd.multiply(pctBd)
        val result = if (increase) xBd.add(delta) else xBd.subtract(delta)
        return result.setScale(SCALE_MONEY, RoundingMode.HALF_EVEN).toDouble()
    }

    fun safeToSpend(
        balance: Double,
        essentialRemaining: Double,
        savingsCommitment: Double,
        emiCommitment: Double = 0.0
    ): Double {
        val balBd = toBigDecimalOrZero(balance)
        val essBd = toBigDecimalOrZero(kotlin.math.max(0.0, essentialRemaining))
        val savBd = toBigDecimalOrZero(kotlin.math.max(0.0, savingsCommitment))
        val emiBd = toBigDecimalOrZero(kotlin.math.max(0.0, emiCommitment))

        val reserved = essBd.add(savBd).add(emiBd)
        val safe = balBd.subtract(reserved)
        return if (safe < BigDecimal.ZERO) 0.0 else safe.setScale(SCALE_MONEY, RoundingMode.HALF_EVEN).toDouble()
    }
}
