package com.walletscholer.app.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import com.walletscholer.app.data.model.DefaultCategories
import com.walletscholer.app.data.model.GoalEntity
import com.walletscholer.app.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetsSyncEngine {

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val timestamp: String,
        val exportedRows: Int
    )

    /**
     * Builds a real, authenticated Sheets API client scoped to the currently signed-in
     * Google account. Requires that account to have granted the Sheets scope during
     * sign-in (GoogleAuthManager already requests it).
     */
    private fun buildSheetsService(context: Context, account: GoogleSignInAccount): Sheets {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        )
        credential.selectedAccount = account.account
        return Sheets.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Wallet Scholar")
            .build()
    }

    /**
     * Writes transactions, budget allocations, and goals into three tabs of the user's
     * OWN spreadsheet (identified by [sheetId]). The signed-in Google account must have
     * edit access to that spreadsheet, which it will if the account owns it or it has
     * been shared with "Editor" access.
     *
     * Each tab is fully cleared and rewritten on every sync (simplest correct approach
     * for a personal single-user app; safe because it always mirrors local Room data).
     * The three tabs — "Transactions", "Budget", "Goals" — must exist in the target
     * spreadsheet with those exact tab names before the first sync.
     */
    suspend fun performGoogleSheetsSync(
        context: Context,
        account: GoogleSignInAccount,
        sheetId: String,
        transactions: List<TransactionEntity>,
        goals: List<GoalEntity>,
        allocations: Map<String, Double>
    ): SyncResult = withContext(Dispatchers.IO) {
        if (sheetId.isBlank()) {
            return@withContext SyncResult(
                success = false,
                message = "No spreadsheet connected. Paste your Google Sheet ID first.",
                timestamp = "",
                exportedRows = 0
            )
        }

        try {
            val sheets = buildSheetsService(context, account)

            val txHeader = listOf(listOf("ID", "Date", "Time", "Type", "Category", "Amount", "Description", "Status"))
            val txRows = transactions.map { tx ->
                val catName = DefaultCategories.findCategory(tx.categoryId).name
                listOf(tx.id, tx.date, tx.time, tx.type, catName, tx.amount.toString(), tx.description, tx.status)
            }
            writeTab(sheets, sheetId, "Transactions", txHeader + txRows)

            val budgetHeader = listOf(listOf("Category ID", "Category Name", "Allocated Amount"))
            val budgetRows = allocations.map { (catId, amount) ->
                val catName = DefaultCategories.findCategory(catId).name
                listOf(catId, catName, amount.toString())
            }
            writeTab(sheets, sheetId, "Budget", budgetHeader + budgetRows)

            val goalsHeader = listOf(listOf("Goal ID", "Name", "Target Amount", "Current Saved", "Target Date"))
            val goalsRows = goals.map { g ->
                listOf(g.id, g.name, g.targetAmount.toString(), g.currentAmount.toString(), g.targetDate)
            }
            writeTab(sheets, sheetId, "Goals", goalsHeader + goalsRows)

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val rowCount = txRows.size + budgetRows.size + goalsRows.size
            SyncResult(
                success = true,
                message = "Synced $rowCount rows to your Google Sheet",
                timestamp = timestamp,
                exportedRows = rowCount
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync failed: ${e.message ?: e.javaClass.simpleName}",
                timestamp = "",
                exportedRows = 0
            )
        }
    }

    private fun writeTab(sheets: Sheets, sheetId: String, tabName: String, rows: List<List<Any>>) {
        sheets.spreadsheets().values()
            .clear(sheetId, "$tabName!A:Z", ClearValuesRequest())
            .execute()

        if (rows.isEmpty()) return

        val body = ValueRange().setValues(rows)
        sheets.spreadsheets().values()
            .update(sheetId, "$tabName!A1", body)
            .setValueInputOption("RAW")
            .execute()
    }

    /**
     * Builds a CSV of all transactions — used for the "Export CSV" share action, which
     * works regardless of whether Google Sync is set up.
     */
    fun exportTransactionsToCsv(
        transactions: List<TransactionEntity>,
        goals: List<GoalEntity>,
        allocations: Map<String, Double>
    ): String {
        val sb = StringBuilder()
        sb.append("=== WALLET SCHOLAR - EXPORT ===\n")
        sb.append("Export Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

        sb.append("--- TRANSACTIONS ---\n")
        sb.append("ID,Date,Time,Type,Category,Amount,Description,Status\n")
        transactions.forEach { tx ->
            val catName = DefaultCategories.findCategory(tx.categoryId).name
            val desc = tx.description.replace(",", " ")
            sb.append("${tx.id},${tx.date},${tx.time},${tx.type},\"$catName\",${tx.amount},\"$desc\",${tx.status}\n")
        }

        sb.append("\n--- BUDGET ALLOCATIONS ---\n")
        sb.append("Category ID,Category Name,Allocated Amount\n")
        allocations.forEach { (catId, amount) ->
            val catName = DefaultCategories.findCategory(catId).name
            sb.append("$catId,\"$catName\",$amount\n")
        }

        sb.append("\n--- SAVINGS GOALS ---\n")
        sb.append("Goal ID,Name,Target Amount,Current Saved,Target Date\n")
        goals.forEach { g ->
            sb.append("${g.id},\"${g.name}\",${g.targetAmount},${g.currentAmount},${g.targetDate}\n")
        }

        return sb.toString()
    }

    fun shareCsvExport(context: Context, csvData: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvData)
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export to Google Sheets / CSV")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
