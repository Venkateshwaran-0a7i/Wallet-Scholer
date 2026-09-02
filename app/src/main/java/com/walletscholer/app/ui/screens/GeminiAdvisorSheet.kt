package com.walletscholer.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import com.walletscholer.app.data.remote.gemini.ChatMessage
import com.walletscholer.app.data.remote.gemini.GeminiApiClient
import com.walletscholer.app.data.remote.gemini.MessageSender
import com.walletscholer.app.domain.FinanceEngine
import com.walletscholer.app.ui.components.PillBadge
import com.walletscholer.app.ui.theme.WalletTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAdvisorSheet(
    transactions: List<TransactionEntity>,
    allocations: Map<String, Double>,
    goals: List<GoalEntity>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Pre-seed conversation with greeting
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AI,
                text = "Hello! I am **Wallet Scholar AI**, your personal wealth strategist and financial advisor. I have analyzed your active transactions and budget allocations. How can I assist you with your money goals today?"
            )
        )
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Build user financial summary context for Gemini
    val balance = remember(transactions) { FinanceEngine.computeBalance(transactions) }
    val monthExpense = remember(transactions) {
        transactions.filter { it.status == "ACTIVE" && it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalAllocated = remember(allocations) { allocations.values.sum() }

    val financialContext = remember(balance, monthExpense, totalAllocated, goals) {
        val goalsSummary = goals.joinToString(", ") { "${it.name}: ${FinanceEngine.fmtMoney(it.currentAmount)}/${FinanceEngine.fmtMoney(it.targetAmount)}" }
        """
        - Current Total Balance: ${FinanceEngine.fmtMoney(balance)}
        - This Month Total Spend: ${FinanceEngine.fmtMoney(monthExpense)}
        - Total Monthly Budget: ${FinanceEngine.fmtMoney(totalAllocated)}
        - Active Savings Goals: ${if (goals.isNotEmpty()) goalsSummary else "None"}
        - Category Allocations: ${allocations.entries.joinToString(", ") { "${it.key}: ${FinanceEngine.fmtMoney(it.value)}" }}
        """.trimIndent()
    }

    fun sendMessage(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || isGenerating) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )
        messages.add(userMsg)
        inputText = ""
        isGenerating = true

        scope.launch {
            try {
                val reply = GeminiApiClient.sendChatMessage(
                    conversationHistory = messages.toList(),
                    financialContext = financialContext
                )
                messages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text = reply
                    )
                )
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.AI,
                        text = "I had a moment analyzing the figures. Here is a recommendation: Check your top spending categories this month to trim non-essential leaks."
                    )
                )
            } finally {
                isGenerating = false
            }
        }
    }

    val suggestionPrompts = listOf(
        "💡 How can I save $5,000 extra this month?",
        "📊 Analyze my top expense categories",
        "🎯 Am I on track for my savings goals?",
        "📉 Should I pay off EMI or invest in SIP?",
        "⚖️ Give me a 50/30/20 budget recommendation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WalletTheme.colors.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(WalletTheme.colors.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WalletTheme.colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Wallet Scholar AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WalletTheme.colors.text
                            )
                            PillBadge(
                                text = "Gemini Flash",
                                textColor = WalletTheme.colors.accent,
                                backgroundColor = WalletTheme.colors.accentSoft
                            )
                        }
                        Text(
                            text = "Real-time AI Financial Strategist",
                            fontSize = 12.sp,
                            color = WalletTheme.colors.subtext
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    sender = MessageSender.AI,
                                    text = "Conversation cleared. How else can I assist your financial planning?"
                                )
                            )
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Chat",
                            tint = WalletTheme.colors.subtext,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = WalletTheme.colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Chat Messages Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = WalletTheme.colors.accent,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analyzing financial figures...",
                                fontSize = 12.5.sp,
                                color = WalletTheme.colors.subtext
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Suggestion Chips Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestionPrompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(WalletTheme.colors.surfaceAlt)
                            .border(1.dp, WalletTheme.colors.border, RoundedCornerShape(16.dp))
                            .clickable { sendMessage(prompt) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WalletTheme.colors.text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask anything about your money...",
                            fontSize = 13.5.sp,
                            color = WalletTheme.colors.faint
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gemini_chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WalletTheme.colors.accent,
                        unfocusedBorderColor = WalletTheme.colors.border,
                        focusedContainerColor = WalletTheme.colors.surfaceAlt,
                        unfocusedContainerColor = WalletTheme.colors.surfaceAlt,
                        focusedTextColor = WalletTheme.colors.text,
                        unfocusedTextColor = WalletTheme.colors.text
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isGenerating) WalletTheme.colors.accent else WalletTheme.colors.surfaceAlt)
                        .testTag("gemini_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isGenerating) WalletTheme.colors.accentText else WalletTheme.colors.faint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(WalletTheme.colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = WalletTheme.colors.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.8f else 0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) WalletTheme.colors.accent else WalletTheme.colors.surfaceAlt
                )
                .border(
                    1.dp,
                    if (isUser) Color.Transparent else WalletTheme.colors.border,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = if (isUser) WalletTheme.colors.accentText else WalletTheme.colors.text
            )
        }
    }
}
