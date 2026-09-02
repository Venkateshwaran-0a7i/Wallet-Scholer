package com.walletscholer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.theme.WalletTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class ChartViewMode {
    CATEGORIES, MONTHLY_TREND, DAILY_PACE
}

data class CategorySpendData(
    val categoryId: String,
    val name: String,
    val spent: Double,
    val allocated: Double,
    val percentageOfTotal: Double,
    val color: Color
)

data class MonthTrendData(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

@Composable
fun MonthlySpendingTrendsChart(
    transactions: List<TransactionEntity>,
    allocations: Map<String, Double>,
    currentMonthKey: String,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(ChartViewMode.CATEGORIES) }
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }

    val activeTxs = remember(transactions) { transactions.filter { it.status == "ACTIVE" } }
    val currentMonthTxs = remember(activeTxs, currentMonthKey) {
        activeTxs.filter { it.date.startsWith(currentMonthKey) }
    }

    val totalMonthExpense = remember(currentMonthTxs) {
        currentMonthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalMonthIncome = remember(currentMonthTxs) {
        currentMonthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    // Palette for distinct visual category distinction
    val categoryColors = listOf(
        Color(0xFFE5A93C), // Amber Gold
        Color(0xFF4E8AF4), // Royal Blue
        Color(0xFFE55858), // Crimson Red
        Color(0xFF34C759), // Emerald Green
        Color(0xFF9D65E5), // Purple
        Color(0xFFF38B42), // Coral Orange
        Color(0xFF20B2AA), // Teal
        Color(0xFFE861A5), // Pink
        Color(0xFF8E8E93)  // Neutral Slate
    )

    // Aggregate category spend
    val categorySpendList = remember(currentMonthTxs, allocations, totalMonthExpense) {
        val expensesByCat = mutableMapOf<String, Double>()
        currentMonthTxs.filter { it.type == "EXPENSE" }.forEach { tx ->
            expensesByCat[tx.categoryId] = (expensesByCat[tx.categoryId] ?: 0.0) + tx.amount
        }

        val allCatKeys = (expensesByCat.keys + allocations.keys).distinct()
        var colorIdx = 0

        allCatKeys.mapNotNull { catId ->
            val spent = expensesByCat[catId] ?: 0.0
            val alloc = allocations[catId] ?: 0.0
            if (spent > 0 || alloc > 0) {
                val cat = DefaultCategories.findCategory(catId)
                val pct = if (totalMonthExpense > 0) (spent / totalMonthExpense) * 100.0 else 0.0
                val color = categoryColors[colorIdx % categoryColors.size]
                colorIdx++
                CategorySpendData(
                    categoryId = catId,
                    name = cat.name,
                    spent = spent,
                    allocated = alloc,
                    percentageOfTotal = pct,
                    color = color
                )
            } else null
        }.sortedByDescending { it.spent }
    }

    // Historical 5-month trend computation
    val monthlyTrends = remember(activeTxs) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthNames = SimpleDateFormat("MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val list = mutableListOf<MonthTrendData>()

        for (i in 4 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val key = sdf.format(c.time)
            val label = monthNames.format(c.time)
            val txsInMonth = activeTxs.filter { it.date.startsWith(key) }
            val inc = txsInMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = txsInMonth.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            list.add(MonthTrendData(monthLabel = label, income = inc, expense = exp))
        }
        list
    }

    // Key Summary Metrics
    val topCategory = categorySpendList.firstOrNull()
    val totalAllocated = allocations.values.sum()
    val calendar = Calendar.getInstance()
    val dayOfMonth = max(1, calendar.get(Calendar.DAY_OF_MONTH))
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyAverage = if (dayOfMonth > 0) totalMonthExpense / dayOfMonth else 0.0
    val projectedMonthEnd = dailyAverage * daysInMonth

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_spending_trends_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Chart Header & Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Spending Trends",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.text
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Category breakdown & cash flow analysis",
                        fontSize = 12.sp,
                        color = WalletTheme.colors.subtext
                    )
                }

                // Mode Selector Buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(WalletTheme.colors.surfaceAlt)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ChartModeTab(
                        icon = Icons.Default.PieChart,
                        selected = selectedMode == ChartViewMode.CATEGORIES,
                        onClick = { selectedMode = ChartViewMode.CATEGORIES }
                    )
                    ChartModeTab(
                        icon = Icons.Default.BarChart,
                        selected = selectedMode == ChartViewMode.MONTHLY_TREND,
                        onClick = { selectedMode = ChartViewMode.MONTHLY_TREND }
                    )
                    ChartModeTab(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        selected = selectedMode == ChartViewMode.DAILY_PACE,
                        onClick = { selectedMode = ChartViewMode.DAILY_PACE }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key Highlights Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(WalletTheme.colors.surfaceAlt)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOP CATEGORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.faint
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = topCategory?.let { "${it.name} (${it.percentageOfTotal.toInt()}%)" } ?: "None",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = topCategory?.color ?: WalletTheme.colors.text
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DAILY AVERAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.faint
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = FinanceEngine.fmtMoney(dailyAverage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.text
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PROJECTED TOTAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WalletTheme.colors.faint
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = FinanceEngine.fmtMoney(projectedMonthEnd),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (projectedMonthEnd > totalAllocated && totalAllocated > 0) WalletTheme.colors.warn else WalletTheme.colors.success
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Views
            when (selectedMode) {
                ChartViewMode.CATEGORIES -> {
                    CategoryBreakdownView(
                        categoryList = categorySpendList,
                        totalExpense = totalMonthExpense,
                        selectedIndex = selectedCategoryIndex,
                        onSelectCategory = { index ->
                            selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                        }
                    )
                }
                ChartViewMode.MONTHLY_TREND -> {
                    MonthlyBarChartView(monthlyTrends = monthlyTrends)
                }
                ChartViewMode.DAILY_PACE -> {
                    DailyPaceTrajectoryView(
                        dayOfMonth = dayOfMonth,
                        daysInMonth = daysInMonth,
                        totalMonthExpense = totalMonthExpense,
                        totalAllocated = totalAllocated
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartModeTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) WalletTheme.colors.accent else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) WalletTheme.colors.accentText else WalletTheme.colors.subtext,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CategoryBreakdownView(
    categoryList: List<CategorySpendData>,
    totalExpense: Double,
    selectedIndex: Int?,
    onSelectCategory: (Int) -> Unit
) {
    if (categoryList.isEmpty() || totalExpense <= 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category expense data recorded for this month.",
                fontSize = 12.5.sp,
                color = WalletTheme.colors.subtext
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Multi-Segment Horizontal Distribution Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            categoryList.forEachIndexed { index, item ->
                val weight = max(0.01f, (item.spent / totalExpense).toFloat())
                val isSelected = selectedIndex == null || selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(14.dp)
                        .background(if (isSelected) item.color else item.color.copy(alpha = 0.3f))
                        .clickable { onSelectCategory(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Category Detail Rows
        categoryList.take(5).forEachIndexed { index, item ->
            val isSelected = selectedIndex == null || selectedIndex == index
            val progress = if (item.allocated > 0) {
                FinanceEngine.clamp(item.spent / item.allocated, 0.0, 1.0).toFloat()
            } else 0f

            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 600),
                label = "cat_progress"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedIndex == index) WalletTheme.colors.surfaceAlt else Color.Transparent)
                    .clickable { onSelectCategory(index) }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = WalletTheme.colors.text
                        )
                        Text(
                            text = "${FinanceEngine.fmtMoney(item.spent)} (${item.percentageOfTotal.toInt()}%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletTheme.colors.text
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Mini Progress Indicator against budget
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(WalletTheme.colors.border)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (item.spent > item.allocated && item.allocated > 0)
                                        WalletTheme.colors.danger
                                    else
                                        item.color
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBarChartView(monthlyTrends: List<MonthTrendData>) {
    val accentColor = WalletTheme.colors.accent
    val dangerColor = WalletTheme.colors.danger
    val borderCol = WalletTheme.colors.border
    val textCol = WalletTheme.colors.subtext

    val maxVal = remember(monthlyTrends) {
        max(1.0, monthlyTrends.maxOf { max(it.income, it.expense) })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Income", fontSize = 11.sp, color = textCol)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dangerColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Expense", fontSize = 11.sp, color = textCol)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Bar Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val barGroupWidth = width / monthlyTrends.size
            val barWidth = barGroupWidth * 0.28f
            val spacing = barGroupWidth * 0.08f

            // Baseline
            drawLine(
                color = borderCol,
                start = Offset(0f, height - 20f),
                end = Offset(width, height - 20f),
                strokeWidth = 1.5f
            )

            monthlyTrends.forEachIndexed { i, d ->
                val centerX = (i * barGroupWidth) + (barGroupWidth / 2f)

                // Income bar
                val incHeight = ((d.income / maxVal) * (height - 30f)).toFloat()
                val incLeft = centerX - barWidth - (spacing / 2f)
                val incTop = height - 20f - incHeight

                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(incLeft, incTop),
                    size = Size(barWidth, incHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Expense bar
                val expHeight = ((d.expense / maxVal) * (height - 30f)).toFloat()
                val expLeft = centerX + (spacing / 2f)
                val expTop = height - 20f - expHeight

                drawRoundRect(
                    color = dangerColor,
                    topLeft = Offset(expLeft, expTop),
                    size = Size(barWidth, expHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        // Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            monthlyTrends.forEach { d ->
                Text(
                    text = d.monthLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = WalletTheme.colors.subtext
                )
            }
        }
    }
}

@Composable
private fun DailyPaceTrajectoryView(
    dayOfMonth: Int,
    daysInMonth: Int,
    totalMonthExpense: Double,
    totalAllocated: Double
) {
    val successColor = WalletTheme.colors.success
    val warnColor = WalletTheme.colors.warn
    val borderCol = WalletTheme.colors.border
    val accentColor = WalletTheme.colors.accent

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Day $dayOfMonth of $daysInMonth",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = WalletTheme.colors.text
            )
            val paceStatus = if (totalAllocated > 0 && (totalMonthExpense / max(1.0, totalAllocated)) > (dayOfMonth.toDouble() / daysInMonth)) {
                "Pacing Higher than Target"
            } else {
                "Pacing on Target 🎯"
            }
            Text(
                text = paceStatus,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (paceStatus.contains("Higher")) warnColor else successColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val w = size.width
            val h = size.height - 20f

            // Draw target diagonal line (Ideal budget pacing)
            val idealPath = Path().apply {
                moveTo(0f, h)
                lineTo(w, 10f)
            }
            drawPath(
                path = idealPath,
                color = borderCol,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Current spent trajectory curve
            val currentFraction = (dayOfMonth.toFloat() / daysInMonth.toFloat()).coerceIn(0.05f, 1f)
            val spentFraction = if (totalAllocated > 0) {
                (totalMonthExpense / totalAllocated).toFloat().coerceIn(0f, 1.2f)
            } else 0f

            val curX = w * currentFraction
            val curY = h - (spentFraction * (h - 10f)).coerceAtMost(h)

            val actualPath = Path().apply {
                moveTo(0f, h)
                quadraticTo(curX * 0.5f, h * 0.7f, curX, curY)
            }

            drawPath(
                path = actualPath,
                color = accentColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Current pointer point
            drawCircle(
                color = accentColor,
                radius = 6f,
                center = Offset(curX, curY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(curX, curY)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Day 1", fontSize = 10.sp, color = WalletTheme.colors.faint)
            Text("Day 15", fontSize = 10.sp, color = WalletTheme.colors.faint)
            Text("Day $daysInMonth", fontSize = 10.sp, color = WalletTheme.colors.faint)
        }
    }
}
