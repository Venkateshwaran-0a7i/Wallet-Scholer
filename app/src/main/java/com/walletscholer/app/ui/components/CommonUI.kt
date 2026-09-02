package com.walletscholer.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.theme.WalletTheme

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = WalletTheme.colors.surface,
    borderColor: Color = WalletTheme.colors.border,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun PillBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProgressRing(
    pct: Double,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 8.dp,
    color: Color? = null
) {
    val clamped = FinanceEngine.clamp(pct, 0.0, 100.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = clamped / 100f, label = "progress")
    val defaultColor = when {
        pct >= 100 -> WalletTheme.colors.danger
        pct >= 90 -> WalletTheme.colors.warn
        else -> WalletTheme.colors.accent
    }
    val ringColor = color ?: defaultColor
    val trackColor = WalletTheme.colors.borderSoft

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun getCategoryIcon(key: String): ImageVector {
    return when (key.lowercase()) {
        "rent", "building" -> Icons.Default.AccountBalance
        "food", "utensils" -> Icons.Default.Restaurant
        "transport", "car" -> Icons.Default.DirectionsCar
        "shopping", "shopping_bag" -> Icons.Default.ShoppingBag
        "entertainment", "film" -> Icons.Default.Movie
        "savings", "piggy_bank" -> Icons.Default.Savings
        "emergency", "alert" -> Icons.Default.Warning
        "medicine", "pill" -> Icons.Default.MedicalServices
        "emi", "credit_card" -> Icons.Default.CreditCard
        "investment", "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
        "salary", "briefcase" -> Icons.Default.Work
        "freelance", "laptop" -> Icons.Default.Laptop
        "bonus", "gift" -> Icons.Default.CardGiftcard
        "target" -> Icons.Default.TrackChanges
        "shield" -> Icons.Default.Security
        else -> Icons.Default.MoreHoriz
    }
}

@Composable
fun CategoryIconBox(
    iconKey: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val bg = if (isIncome) WalletTheme.colors.successSoft else WalletTheme.colors.borderSoft
    val tint = if (isIncome) WalletTheme.colors.success else WalletTheme.colors.subtext

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = WalletTheme.colors.subtext,
            letterSpacing = 0.6.sp
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.accent,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Composable
fun PrimaryAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WalletTheme.colors.accent,
            contentColor = WalletTheme.colors.accentText,
            disabledContainerColor = WalletTheme.colors.border,
            disabledContentColor = WalletTheme.colors.faint
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun GhostAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = WalletTheme.colors.text,
    borderColor: Color = WalletTheme.colors.border,
    leadingIcon: ImageVector? = null,
    testTag: String = "ghost_button"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun TransactionItemRow(
    tx: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = tx.type == "INCOME"
    val isVoided = tx.status == "VOIDED"
    val cat = DefaultCategories.findCategory(tx.categoryId)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryIconBox(
            iconKey = cat.iconKey,
            isIncome = isIncome
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (tx.description.isNotBlank()) tx.description else cat.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = if (isVoided) WalletTheme.colors.faint else WalletTheme.colors.text,
                textDecoration = if (isVoided) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${cat.name} · ${tx.date}${if (isVoided) " · Voided" else ""}",
                fontSize = 12.sp,
                color = WalletTheme.colors.faint
            )
        }

        Text(
            text = "${if (isIncome) "+" else "-"}${FinanceEngine.fmtMoney(tx.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = when {
                isVoided -> WalletTheme.colors.faint
                isIncome -> WalletTheme.colors.success
                else -> WalletTheme.colors.text
            }
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = WalletTheme.colors.subtext.copy(alpha = 0.5f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = WalletTheme.colors.text
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = WalletTheme.colors.subtext
        )
    }
}
