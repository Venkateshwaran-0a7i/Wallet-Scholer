package com.walletscholer.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Wallet : Screen("wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Calculator : Screen("calculator", "Calculator", Icons.Default.Calculate)
    object Budget : Screen("budget", "Budget", Icons.Default.PieChart)
    object More : Screen("more", "More", Icons.Default.MoreHoriz)
}

val NAV_ITEMS = listOf(
    Screen.Home,
    Screen.Wallet,
    Screen.Calculator,
    Screen.Budget,
    Screen.More
)
