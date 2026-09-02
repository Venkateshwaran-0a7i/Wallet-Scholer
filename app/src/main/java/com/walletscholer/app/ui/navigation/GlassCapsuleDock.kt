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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
 * Modern floating glass capsule dock navigation bar.
 * Uses Compose blur effects, transparent glassmorphism background,
 * subtle specular light borders, and smooth spring animations.
 */
@Composable
fun GlassCapsuleDock(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(36.dp)

    // Glass translucency colors with alpha
    val glassBgBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xCC1E1B29),
                    Color(0xB312101B)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD9FFFFFF),
                    Color(0xB8F1F3F9)
                )
            )
        }
    }

    // Specular glass highlight border
    val glassBorderBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x66FFFFFF),
                    Color(0x1AFFFFFF),
                    Color(0x0DFFFFFF)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x80FFFFFF),
                    Color(0x33B0BEC5),
                    Color(0x1A90A4AE)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("glass_capsule_dock"),
        contentAlignment = Alignment.Center
    ) {
        // Floating Frosted Glass Container
        Surface(
            shape = dockShape,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = dockShape,
                    spotColor = if (isDarkTheme) Color(0x99000000) else Color(0x334B5563),
                    ambientColor = if (isDarkTheme) Color(0x66000000) else Color(0x1F1E293B)
                )
                .clip(dockShape)
                .background(glassBgBrush)
                .border(1.2.dp, glassBorderBrush, dockShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NAV_ITEMS.forEach { screen ->
                    val selected = currentScreen == screen

                    DockItem(
                        screen = screen,
                        selected = selected,
                        onClick = { onScreenSelected(screen) },
                        modifier = Modifier.weight(1f)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
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

    val itemShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .clip(itemShape)
            .background(
                if (selected) WalletTheme.colors.accentSoft else Color.Transparent,
                itemShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            )
            .padding(vertical = 8.dp)
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
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                letterSpacing = if (selected) 0.2.sp else 0.sp
            )
        }
    }
}
