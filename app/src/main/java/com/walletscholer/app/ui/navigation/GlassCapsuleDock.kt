package com.walletscholer.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.ui.theme.WalletTheme

/**
 * Modern floating glass capsule dock navigation bar (detached deck).
 * Features translucent frosted glassmorphism styling, ambient floating shadow,
 * smooth spring animated active indicator pills, and ergonomic touch targets.
 */
@Composable
fun GlassCapsuleDock(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // Frosted glass background gradient
    val glassBgBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF0181520),
                    Color(0xF5100E17)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF2FFFFFF),
                    Color(0xFAF4F6FB)
                )
            )
        }
    }

    val glassBorderColor = if (isDarkTheme) {
        Color(0x38FFFFFF)
    } else {
        Color(0x26000000)
    }

    val dockShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("glass_capsule_dock")
    ) {
        Surface(
            shape = dockShape,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = dockShape,
                    spotColor = if (isDarkTheme) Color(0x99000000) else Color(0x33000000),
                    ambientColor = if (isDarkTheme) Color(0x4D000000) else Color(0x1A000000)
                )
                .clip(dockShape)
                .background(glassBgBrush)
                .border(1.dp, glassBorderColor, dockShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_ITEMS.forEach { screen ->
                    val selected = currentScreen == screen

                    DockItem(
                        screen = screen,
                        selected = selected,
                        onClick = { onScreenSelected(screen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) WalletTheme.colors.accent else WalletTheme.colors.faint,
        animationSpec = tween(durationMillis = 200),
        label = "icon_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) WalletTheme.colors.accent else WalletTheme.colors.faint,
        animationSpec = tween(durationMillis = 200),
        label = "text_color"
    )

    val itemShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .clip(itemShape)
            .background(
                if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                itemShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("nav_tab_${screen.route}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = screen.title,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                letterSpacing = if (selected) 0.2.sp else 0.sp
            )
        }
    }
}
