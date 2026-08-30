package com.walletscholer.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.model.UserSettingsEntity
import com.walletscholer.app.data.remote.GoogleSheetsSyncEngine
import com.walletscholer.app.ui.components.AppCard
import com.walletscholer.app.ui.components.PillBadge
import com.walletscholer.app.ui.theme.WalletTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSyncSheet(
    settings: UserSettingsEntity?,
    transactions: List<TransactionEntity>,
    goals: List<GoalEntity>,
    allocations: Map<String, Double>,
    googleAccount: GoogleSignInAccount?,
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit,
    onLogout: () -> Unit,
    onSaveSheetId: (String) -> Unit,
    onSyncResult: (success: Boolean) -> Unit,
    onFreshStart: (preset: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf(settings?.lastSyncTime?.let { "Last synced at $it" } ?: "Not synced yet") }
    var showFreshStartDialog by remember { mutableStateOf(false) }

    // Real Google account is the source of truth for identity/login state.
    val userLoggedIn = googleAccount != null
    val userName = googleAccount?.displayName ?: googleAccount?.email ?: "Not signed in"
    val userEmail = googleAccount?.email ?: "Sign in to sync with your own Google Sheet"

    var sheetIdInput by remember(settings?.googleSheetId) { mutableStateOf(settings?.googleSheetId ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WalletTheme.colors.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account & Cloud Sync",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WalletTheme.colors.text
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = WalletTheme.colors.text
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile / Login Card
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(WalletTheme.colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletTheme.colors.accentText
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.text
                            )
                            PillBadge(
                                text = if (userLoggedIn) "Google Active" else "Offline Guest",
                                textColor = if (userLoggedIn) WalletTheme.colors.success else WalletTheme.colors.warn,
                                backgroundColor = if (userLoggedIn) WalletTheme.colors.successSoft else WalletTheme.colors.warnSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            fontSize = 12.5.sp,
                            color = WalletTheme.colors.subtext
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (userLoggedIn) {
                        OutlinedButton(
                            onClick = { onLogout() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WalletTheme.colors.subtext
                            )
                        ) {
                            Text("Log Out", fontSize = 13.sp)
                        }
                    } else {
                        Button(
                            onClick = { onLoginClick() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletTheme.colors.accent,
                                contentColor = WalletTheme.colors.accentText
                            )
                        ) {
                            Text("Sign In with Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Google Sheets Live Sync Section
            Text(
                text = "Google Sheets Live Integration",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )
            Spacer(modifier = Modifier.height(8.dp))

            AppCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F9D58).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color(0xFF0F9D58),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Spreadsheet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletTheme.colors.text
                        )
                        Text(
                            text = "Paste the ID of a sheet YOU own or can edit",
                            fontSize = 12.sp,
                            color = WalletTheme.colors.subtext
                        )
                    }

                    if (settings?.syncStatus == "SYNCED") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Synced",
                            tint = WalletTheme.colors.success,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sheetIdInput,
                    onValueChange = { sheetIdInput = it },
                    label = { Text("Google Sheet ID") },
                    placeholder = { Text("From the sheet's URL: /d/<THIS PART>/edit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSaveSheetId(sheetIdInput) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Sheet ID", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = syncMessage,
                    fontSize = 12.sp,
                    color = WalletTheme.colors.subtext
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val account = googleAccount
                            val currentSheetId = settings?.googleSheetId ?: ""
                            if (account == null) {
                                Toast.makeText(context, "Sign in with Google first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (currentSheetId.isBlank()) {
                                Toast.makeText(context, "Save a Sheet ID first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSyncing = true
                            scope.launch {
                                val res = GoogleSheetsSyncEngine.performGoogleSheetsSync(
                                    context = context,
                                    account = account,
                                    sheetId = currentSheetId,
                                    transactions = transactions,
                                    goals = goals,
                                    allocations = allocations
                                )
                                isSyncing = false
                                syncMessage = if (res.success) "${res.message} at ${res.timestamp}" else res.message
                                onSyncResult(res.success)
                                Toast.makeText(
                                    context,
                                    if (res.success) "✅ Synced with Google Sheets!" else "❌ ${res.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("google_sheets_sync_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalletTheme.colors.accent,
                            contentColor = WalletTheme.colors.accentText
                        )
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = WalletTheme.colors.accentText,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val csv = GoogleSheetsSyncEngine.exportTransactionsToCsv(
                                transactions = transactions,
                                goals = goals,
                                allocations = allocations
                            )
                            GoogleSheetsSyncEngine.shareCsvExport(context, csv)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WalletTheme.colors.text
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fresh Start & Presets
            Text(
                text = "Fresh Start & Reset Wizard",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )
            Spacer(modifier = Modifier.height(8.dp))

            AppCard {
                Text(
                    text = "Start with a clean budget, or load realistic pre-configured profiles suited to your current lifestyle.",
                    fontSize = 12.5.sp,
                    color = WalletTheme.colors.subtext
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetOptionRow(
                        title = "🌱 Clean Slate (Zero Balance)",
                        subtitle = "Start fresh from 0, set custom salary & categories",
                        onClick = {
                            onFreshStart("CLEAN")
                            Toast.makeText(context, "Fresh start initialized!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    PresetOptionRow(
                        title = "💼 Tech Professional (₹85k/mo)",
                        subtitle = "High savings rate, investments, rent & tech gadgets",
                        onClick = {
                            onFreshStart("TECH")
                            Toast.makeText(context, "Loaded Tech Professional budget profile!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    PresetOptionRow(
                        title = "🎓 Student / Young Grad (₹25k/mo)",
                        subtitle = "Optimized for minimal rent, food, transport & study",
                        onClick = {
                            onFreshStart("STUDENT")
                            Toast.makeText(context, "Loaded Student budget profile!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )

                    PresetOptionRow(
                        title = "🏡 Family & Household (₹120k/mo)",
                        subtitle = "Mortgage/EMI, groceries, child education & emergency",
                        onClick = {
                            onFreshStart("FAMILY")
                            Toast.makeText(context, "Loaded Family budget profile!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WalletTheme.colors.surfaceAlt)
            .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = WalletTheme.colors.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = WalletTheme.colors.subtext
            )
        }
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = WalletTheme.colors.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}
