package com.walletscholer.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.walletscholer.app.data.auth.BiometricLockManager
import com.walletscholer.app.ui.theme.WalletTheme
import kotlinx.coroutines.delay

/**
 * Full-screen lock screen shown when biometric lock is enabled.
 * Automatically triggers the biometric prompt on first composition.
 *
 * @param onUnlocked Called when authentication succeeds — caller removes this screen.
 */
@Composable
fun BiometricLockScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var errorMessage by remember { mutableStateOf("") }
    var pulsing by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pulsing) 1.12f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "pulse"
    )

    // Auto-trigger prompt when screen first appears
    LaunchedEffect(Unit) {
        delay(300) // short delay so UI renders first
        activity?.let {
            BiometricLockManager.authenticate(
                activity = it,
                onSuccess = onUnlocked,
                onError = { msg ->
                    if (msg.isNotBlank()) errorMessage = msg
                },
                onFailed = {
                    pulsing = true
                }
            )
        }
    }

    // Reset pulse animation
    LaunchedEffect(pulsing) {
        if (pulsing) {
            delay(250)
            pulsing = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        WalletTheme.colors.appBg,
                        WalletTheme.colors.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // App logo / icon indicator
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(WalletTheme.colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "₩",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WalletTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Wallet Scholar",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WalletTheme.colors.text
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your finances are protected.\nAuthenticate to continue.",
                fontSize = 14.sp,
                color = WalletTheme.colors.subtext,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(52.dp))

            // Fingerprint icon button
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale),
                shape = CircleShape,
                color = WalletTheme.colors.accentSoft,
                shadowElevation = 4.dp,
                onClick = {
                    activity?.let {
                        BiometricLockManager.authenticate(
                            activity = it,
                            onSuccess = onUnlocked,
                            onError = { msg ->
                                if (msg.isNotBlank()) errorMessage = msg
                            },
                            onFailed = {
                                pulsing = true
                            }
                        )
                    }
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Authenticate",
                        tint = WalletTheme.colors.accent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tap to authenticate",
                fontSize = 13.sp,
                color = WalletTheme.colors.faint,
                fontWeight = FontWeight.Medium
            )

            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    color = WalletTheme.colors.dangerSoft,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = WalletTheme.colors.danger,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Try again button
            Button(
                onClick = {
                    errorMessage = ""
                    activity?.let {
                        BiometricLockManager.authenticate(
                            activity = it,
                            onSuccess = onUnlocked,
                            onError = { msg ->
                                if (msg.isNotBlank()) errorMessage = msg
                            },
                            onFailed = { pulsing = true }
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalletTheme.colors.accent,
                    contentColor = WalletTheme.colors.accentText
                ),
                modifier = Modifier.testTag("biometric_retry_button")
            ) {
                Text(
                    text = "Try Again",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}
