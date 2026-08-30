package com.walletscholer.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.theme.WalletTheme

enum class CalcTab(val id: String, val label: String) {
    NORMAL("normal", "Calculator"),
    SPLIT("split", "Split Bill"),
    EMI("emi", "EMI"),
    LOAN("loan", "Loan"),
    SI("si", "Simple Interest"),
    CI("ci", "Compound Interest"),
    SAVINGS("savings", "Savings"),
    SIP("sip", "SIP"),
    PERCENTAGE("pct", "Percentage")
}

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(CalcTab.NORMAL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Calculator",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.text
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Module selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalcTab.entries.forEach { tab ->
                val selected = activeTab == tab
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selected) WalletTheme.colors.accent else WalletTheme.colors.border
                    ),
                    modifier = Modifier.clickable { activeTab = tab }
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp)
        ) {
            when (activeTab) {
                CalcTab.NORMAL -> NormalCalculatorView()
                CalcTab.SPLIT -> SplitCalculatorView()
                CalcTab.EMI -> EmiCalculatorView()
                CalcTab.LOAN -> LoanAffordabilityView()
                CalcTab.SI -> SimpleInterestView()
                CalcTab.CI -> CompoundInterestView()
                CalcTab.SAVINGS -> SavingsGoalView()
                CalcTab.SIP -> SipCalculatorView()
                CalcTab.PERCENTAGE -> PercentageCalculatorView()
            }
        }
    }
}

// ─────────────────────────────────────────────
// 0. Normal Calculator
// ─────────────────────────────────────────────
@Composable
fun NormalCalculatorView() {
    var display by remember { mutableStateOf("0") }
    var operator by remember { mutableStateOf<String?>(null) }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var justEvaluated by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (justEvaluated) {
            display = d
            justEvaluated = false
        } else {
            display = if (display == "0") d else if (display.length < 14) display + d else display
        }
    }

    fun onDot() {
        if (justEvaluated) { display = "0."; justEvaluated = false; return }
        if (!display.contains(".")) display = "$display."
    }

    fun onOperator(op: String) {
        operand1 = display.toDoubleOrNull()
        operator = op
        justEvaluated = true
    }

    fun onEquals() {
        val a = operand1 ?: return
        val b = display.toDoubleOrNull() ?: return
        val result = when (operator) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            "%" -> a * b / 100.0
            else -> b
        }
        display = if (result.isNaN()) "Error" else if (result % 1 == 0.0 && result < 1e12) result.toLong().toString() else String.format(java.util.Locale.US, "%.8g", result).trimEnd('0').trimEnd('.')
        operand1 = null
        operator = null
        justEvaluated = true
    }

    fun onClear() {
        display = "0"
        operator = null
        operand1 = null
        justEvaluated = false
    }

    fun onBackspace() {
        if (justEvaluated || display.length <= 1) { display = "0"; return }
        display = display.dropLast(1)
    }

    AppCard(
        backgroundColor = WalletTheme.colors.surface,
        borderColor = WalletTheme.colors.border
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = display,
                fontSize = if (display.length > 10) 28.sp else 40.sp,
                fontWeight = FontWeight.Light,
                color = WalletTheme.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }

        val buttonRows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { label ->
                        val isOp = label in listOf("÷", "×", "−", "+", "=")
                        val isClear = label == "C"
                        val isBack = label == "⌫"
                        val bgColor = when {
                            label == "=" -> WalletTheme.colors.accent
                            isOp || isClear -> WalletTheme.colors.accentSoft
                            else -> WalletTheme.colors.surfaceAlt
                        }
                        val textColor = when {
                            label == "=" -> WalletTheme.colors.accentText
                            isOp || isClear -> WalletTheme.colors.accent
                            else -> WalletTheme.colors.text
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .clickable {
                                    when (label) {
                                        "C" -> onClear()
                                        "⌫" -> onBackspace()
                                        "=" -> onEquals()
                                        "+", "−", "×", "÷", "%" -> onOperator(label)
                                        "." -> onDot()
                                        "±" -> { display = if (display.startsWith("-")) display.drop(1) else "-$display" }
                                        else -> onDigit(label)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = WalletTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = label,
                                    fontSize = 22.sp,
                                    fontWeight = if (isOp || isClear) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 0b. Split Bill Calculator
// ─────────────────────────────────────────────
@Composable
fun SplitCalculatorView() {
    var bill by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("15") }
    var people by remember { mutableStateOf("2") }

    val billAmt = bill.toDoubleOrNull() ?: 0.0
    val tipPct = tip.toDoubleOrNull() ?: 0.0
    val numPeople = people.toIntOrNull()?.coerceAtLeast(1) ?: 1

    val tipAmount = billAmt * tipPct / 100.0
    val total = billAmt + tipAmount
    val perPerson = if (numPeople > 0) total / numPeople else 0.0

    Column {
        Text(
            text = "Split a restaurant bill evenly among friends.",
            fontSize = 12.5.sp,
            color = WalletTheme.colors.subtext,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CalcInputField(label = "Bill Amount", value = bill, onValueChange = { bill = it }, suffix = "₹", placeholder = "0")
        CalcInputField(label = "Tip %", value = tip, onValueChange = { tip = it }, suffix = "%", placeholder = "15")
        CalcInputField(label = "Number of People", value = people, onValueChange = { v ->
            if (v.isEmpty() || v.matches(Regex("^\\d+$"))) people = v
        }, placeholder = "2")

        // Tip presets
        Text(
            text = "TIP PRESET",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("0", "5", "10", "15", "18", "20").forEach { pct ->
                val selected = tip == pct
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selected) WalletTheme.colors.accent else WalletTheme.colors.border
                    ),
                    modifier = Modifier.clickable { tip = pct }
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (billAmt > 0) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Per Person", FinanceEngine.fmtMoneyPrecise(perPerson), isBig = true),
                    ResultRow("Tip Amount", FinanceEngine.fmtMoney(tipAmount)),
                    ResultRow("Total Bill", FinanceEngine.fmtMoney(total)),
                    ResultRow("Splitting between", "$numPeople people")
                )
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a bill amount to split", "—")))
        }

        ResetButton {
            bill = ""; tip = "15"; people = "2"
        }
    }
}

@Composable
fun CalcInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String? = null,
    placeholder: String = "0"
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                    onValueChange(input)
                }
            },
            placeholder = { Text(placeholder, color = WalletTheme.colors.faint) },
            trailingIcon = suffix?.let {
                {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = WalletTheme.colors.faint,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            },
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
    }
}

data class ResultRow(val label: String, val value: String, val isBig: Boolean = false)

@Composable
fun ResultPanel(
    rows: List<ResultRow>,
    disclaimer: String? = null
) {
    AppCard(
        backgroundColor = WalletTheme.colors.accentSoft,
        borderColor = WalletTheme.colors.accent.copy(alpha = 0.3f)
    ) {
        rows.forEach { r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = r.label,
                    fontSize = 13.sp,
                    color = WalletTheme.colors.subtext
                )
                Text(
                    text = r.value,
                    fontSize = if (r.isBig) 20.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
            }
        }
        if (disclaimer != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WalletTheme.colors.subtext,
                    modifier = Modifier
                        .size(13.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    text = disclaimer,
                    fontSize = 11.5.sp,
                    color = WalletTheme.colors.subtext
                )
            }
        }
    }
}

@Composable
fun ResetButton(onReset: () -> Unit) {
    TextButton(
        onClick = onReset,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = "Reset",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext
        )
    }
}

// 1. EMI
@Composable
fun EmiCalculatorView() {
    var p by remember { mutableStateOf("500000") }
    var r by remember { mutableStateOf("9.5") }
    var y by remember { mutableStateOf("5") }

    val res = FinanceEngine.emiCalc(p.toDoubleOrNull() ?: 0.0, r.toDoubleOrNull() ?: 0.0, y.toDoubleOrNull() ?: 0.0)

    Column {
        CalcInputField(label = "Loan Amount", value = p, onValueChange = { p = it }, suffix = "₹")
        CalcInputField(label = "Annual Interest Rate", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Tenure", value = y, onValueChange = { y = it }, suffix = "years")

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Monthly EMI", FinanceEngine.fmtMoneyPrecise(res.emi), isBig = true),
                    ResultRow("Total Interest", FinanceEngine.fmtMoney(res.totalInterest)),
                    ResultRow("Total Payment", FinanceEngine.fmtMoney(res.totalPayment))
                )
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter valid loan amount and tenure", "—")))
        }

        ResetButton {
            p = "500000"; r = "9.5"; y = "5"
        }
    }
}

// 2. Loan Affordability
@Composable
fun LoanAffordabilityView() {
    var emi by remember { mutableStateOf("15000") }
    var r by remember { mutableStateOf("9.5") }
    var y by remember { mutableStateOf("5") }

    val res = FinanceEngine.loanAffordability(emi.toDoubleOrNull() ?: 0.0, r.toDoubleOrNull() ?: 0.0, y.toDoubleOrNull() ?: 0.0)

    Column {
        Text(
            text = "Find out how much you can borrow for a given monthly payment.",
            fontSize = 12.5.sp,
            color = WalletTheme.colors.subtext,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        CalcInputField(label = "Desired Monthly Payment", value = emi, onValueChange = { emi = it }, suffix = "₹")
        CalcInputField(label = "Annual Interest Rate", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Tenure", value = y, onValueChange = { y = it }, suffix = "years")

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Max Loan Amount", FinanceEngine.fmtMoney(res.principal), isBig = true),
                    ResultRow("Total Interest", FinanceEngine.fmtMoney(res.totalInterest)),
                    ResultRow("Total Payment", FinanceEngine.fmtMoney(res.totalPayment))
                )
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a valid monthly payment and tenure", "—")))
        }

        ResetButton {
            emi = "15000"; r = "9.5"; y = "5"
        }
    }
}

// 3. Simple Interest
@Composable
fun SimpleInterestView() {
    var p by remember { mutableStateOf("100000") }
    var r by remember { mutableStateOf("6") }
    var y by remember { mutableStateOf("2") }

    val res = FinanceEngine.simpleInterest(p.toDoubleOrNull() ?: 0.0, r.toDoubleOrNull() ?: 0.0, y.toDoubleOrNull() ?: 0.0)

    Column {
        CalcInputField(label = "Principal", value = p, onValueChange = { p = it }, suffix = "₹")
        CalcInputField(label = "Annual Rate", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Time", value = y, onValueChange = { y = it }, suffix = "years")

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Interest Earned", FinanceEngine.fmtMoney(res.interest), isBig = true),
                    ResultRow("Final Amount", FinanceEngine.fmtMoney(res.total))
                )
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a valid principal and time", "—")))
        }

        ResetButton {
            p = "100000"; r = "6"; y = "2"
        }
    }
}

// 4. Compound Interest
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompoundInterestView() {
    var p by remember { mutableStateOf("100000") }
    var r by remember { mutableStateOf("7") }
    var y by remember { mutableStateOf("3") }
    var n by remember { mutableStateOf("12") } // monthly

    val res = FinanceEngine.compoundInterest(
        p.toDoubleOrNull() ?: 0.0,
        r.toDoubleOrNull() ?: 0.0,
        y.toDoubleOrNull() ?: 0.0,
        n.toDoubleOrNull() ?: 12.0
    )

    Column {
        CalcInputField(label = "Principal", value = p, onValueChange = { p = it }, suffix = "₹")
        CalcInputField(label = "Annual Rate", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Time", value = y, onValueChange = { y = it }, suffix = "years")

        Text(
            text = "COMPOUNDING FREQUENCY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "1" to "Annually",
                "2" to "Semi-annual",
                "4" to "Quarterly",
                "12" to "Monthly",
                "365" to "Daily"
            ).forEach { (freqVal, freqLbl) ->
                val selected = n == freqVal
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selected) WalletTheme.colors.accent else WalletTheme.colors.border
                    ),
                    modifier = Modifier.clickable { n = freqVal }
                ) {
                    Text(
                        text = freqLbl,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Interest Earned", FinanceEngine.fmtMoney(res.interest), isBig = true),
                    ResultRow("Final Amount", FinanceEngine.fmtMoney(res.total))
                )
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a valid principal and time", "—")))
        }

        ResetButton {
            p = "100000"; r = "7"; y = "3"; n = "12"
        }
    }
}

// 5. Savings Goal
@Composable
fun SavingsGoalView() {
    var target by remember { mutableStateOf("100000") }
    var r by remember { mutableStateOf("6") }
    var y by remember { mutableStateOf("2") }

    val res = FinanceEngine.requiredMonthlySavings(
        target.toDoubleOrNull() ?: 0.0,
        r.toDoubleOrNull() ?: 0.0,
        y.toDoubleOrNull() ?: 0.0
    )

    Column {
        Text(
            text = "Find the monthly amount you need to save to hit a goal.",
            fontSize = 12.5.sp,
            color = WalletTheme.colors.subtext,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        CalcInputField(label = "Target Amount", value = target, onValueChange = { target = it }, suffix = "₹")
        CalcInputField(label = "Expected Annual Return", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Duration", value = y, onValueChange = { y = it }, suffix = "years")

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Required Monthly Saving", FinanceEngine.fmtMoneyPrecise(res.monthly), isBig = true),
                    ResultRow("Total Deposited", FinanceEngine.fmtMoney(res.totalDeposited)),
                    ResultRow("Estimated Growth", FinanceEngine.fmtMoney(res.gain))
                ),
                disclaimer = "Projected returns are estimates and not guaranteed."
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a valid target and duration", "—")))
        }

        ResetButton {
            target = "100000"; r = "6"; y = "2"
        }
    }
}

// 6. SIP Calculator
@Composable
fun SipCalculatorView() {
    var m by remember { mutableStateOf("5000") }
    var r by remember { mutableStateOf("12") }
    var y by remember { mutableStateOf("10") }

    val res = FinanceEngine.sipFutureValue(
        m.toDoubleOrNull() ?: 0.0,
        r.toDoubleOrNull() ?: 0.0,
        y.toDoubleOrNull() ?: 0.0
    )

    Column {
        CalcInputField(label = "Monthly Investment", value = m, onValueChange = { m = it }, suffix = "₹")
        CalcInputField(label = "Expected Annual Return", value = r, onValueChange = { r = it }, suffix = "%")
        CalcInputField(label = "Duration", value = y, onValueChange = { y = it }, suffix = "years")

        if (res != null) {
            ResultPanel(
                rows = listOf(
                    ResultRow("Estimated Future Value", FinanceEngine.fmtMoney(res.futureValue), isBig = true),
                    ResultRow("Total Invested", FinanceEngine.fmtMoney(res.invested)),
                    ResultRow("Estimated Gain", FinanceEngine.fmtMoney(res.gain))
                ),
                disclaimer = "Projected returns are estimates and not guaranteed."
            )
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter a valid monthly amount and duration", "—")))
        }

        ResetButton {
            m = "5000"; r = "12"; y = "10"
        }
    }
}

// 7. Percentage Calculator
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PercentageCalculatorView() {
    var mode by remember { mutableStateOf("of") } // "of", "what", "increase", "decrease"
    var x by remember { mutableStateOf("15") }
    var y by remember { mutableStateOf("2000") }

    val xNum = x.toDoubleOrNull()
    val yNum = y.toDoubleOrNull()

    var result: Double? = null
    var label = ""

    if (xNum != null && yNum != null) {
        when (mode) {
            "of" -> {
                result = FinanceEngine.percentageOf(xNum, yNum)
                label = "$x% of $y"
            }
            "what" -> {
                result = FinanceEngine.whatPercent(xNum, yNum)
                label = "$x is what % of $y"
            }
            "increase" -> {
                result = FinanceEngine.percentChange(yNum, xNum, increase = true)
                label = "$y increased by $x%"
            }
            "decrease" -> {
                result = FinanceEngine.percentChange(yNum, xNum, increase = false)
                label = "$y decreased by $x%"
            }
        }
    }

    Column {
        Text(
            text = "MODE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "of" to "X% of Y",
                "what" to "X is what % of Y",
                "increase" to "Increase Y by X%",
                "decrease" to "Decrease Y by X%"
            ).forEach { (mVal, mLbl) ->
                val selected = mode == mVal
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (selected) WalletTheme.colors.accent else WalletTheme.colors.border
                    ),
                    modifier = Modifier.clickable { mode = mVal }
                ) {
                    Text(
                        text = mLbl,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) WalletTheme.colors.accent else WalletTheme.colors.subtext,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        CalcInputField(
            label = "X",
            value = x,
            onValueChange = { x = it },
            suffix = if (mode == "increase" || mode == "decrease") "%" else null
        )
        CalcInputField(
            label = "Y",
            value = y,
            onValueChange = { y = it }
        )

        if (result != null) {
            val displayValue = if (mode == "what") "${String.format(java.util.Locale.US, "%.2f", result)}%" else FinanceEngine.fmtMoneyPrecise(result)
            ResultPanel(rows = listOf(ResultRow(label, displayValue, isBig = true)))
        } else {
            ResultPanel(rows = listOf(ResultRow("Enter valid numbers", "—")))
        }

        ResetButton {
            x = "15"; y = "2000"
        }
    }
}
