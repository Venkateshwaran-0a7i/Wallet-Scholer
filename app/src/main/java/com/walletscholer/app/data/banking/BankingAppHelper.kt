package com.walletscholer.app.data.banking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri

data class BankingAppInfo(
    val packageName: String,
    val appName: String,
    val category: String,
    val icon: Drawable? = null,
    val isInstalled: Boolean = false,
    val launchIntent: Intent? = null
)

object BankingAppHelper {

    val SUPPORTED_BANKING_APPS = listOf(
        // UPI & Payments
        BankingAppInfo(
            packageName = "com.google.android.apps.nbu.paisa.user",
            appName = "Google Pay",
            category = "UPI & Wallet"
        ),
        BankingAppInfo(
            packageName = "com.phonepe.app",
            appName = "PhonePe",
            category = "UPI & Wallet"
        ),
        BankingAppInfo(
            packageName = "net.one97.paytm",
            appName = "Paytm",
            category = "UPI & Payments"
        ),
        BankingAppInfo(
            packageName = "in.org.npci.upiapp",
            appName = "BHIM UPI",
            category = "Govt UPI"
        ),
        BankingAppInfo(
            packageName = "com.dreamplug.androidapp",
            appName = "CRED",
            category = "Credit & UPI"
        ),
        BankingAppInfo(
            packageName = "in.amazon.mShop.android.shopping",
            appName = "Amazon Pay",
            category = "UPI & Shopping"
        ),
        BankingAppInfo(
            packageName = "com.whatsapp",
            appName = "WhatsApp Pay",
            category = "Chat & Pay"
        ),

        // Major Banks
        BankingAppInfo(
            packageName = "com.sbi.lotusintouch",
            appName = "YONO SBI",
            category = "State Bank of India"
        ),
        BankingAppInfo(
            packageName = "com.snapwork.hdfc",
            appName = "HDFC MobileBanking",
            category = "HDFC Bank"
        ),
        BankingAppInfo(
            packageName = "com.csam.icici.bank.imobile",
            appName = "iMobile Pay (ICICI)",
            category = "ICICI Bank"
        ),
        BankingAppInfo(
            packageName = "com.axis.mobile",
            appName = "Axis Mobile",
            category = "Axis Bank"
        ),
        BankingAppInfo(
            packageName = "com.msf.kbank.mobile",
            appName = "Kotak 811",
            category = "Kotak Mahindra"
        ),
        BankingAppInfo(
            packageName = "com.bankofbaroda.mconnect",
            appName = "bob World (BoB)",
            category = "Bank of Baroda"
        ),
        BankingAppInfo(
            packageName = "com.canarabank.mobility",
            appName = "canara ai1",
            category = "Canara Bank"
        ),
        BankingAppInfo(
            packageName = "com.pnb.pnbone",
            appName = "PNB ONE",
            category = "Punjab National Bank"
        ),

        // NeoBanks & Fintech
        BankingAppInfo(
            packageName = "money.jupiter",
            appName = "Jupiter Money",
            category = "NeoBank"
        ),
        BankingAppInfo(
            packageName = "co.fi.money",
            appName = "Fi Money",
            category = "NeoBank"
        ),
        BankingAppInfo(
            packageName = "com.zerodha.kite3",
            appName = "Kite by Zerodha",
            category = "Investments"
        ),
        BankingAppInfo(
            packageName = "com.nextbillion.groww",
            appName = "Groww",
            category = "Investments & UPI"
        )
    )

    fun getInstalledBankingApps(context: Context): List<BankingAppInfo> {
        val pm = context.packageManager
        val installedList = mutableListOf<BankingAppInfo>()

        for (app in SUPPORTED_BANKING_APPS) {
            try {
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)

                installedList.add(
                    app.copy(
                        appName = appName,
                        icon = icon,
                        isInstalled = true,
                        launchIntent = launchIntent
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
                // Not installed on this device
            } catch (_: Exception) {
                // Safe ignore
            }
        }
        return installedList
    }

    fun getAllSupportedWithStatus(context: Context): List<BankingAppInfo> {
        val pm = context.packageManager
        return SUPPORTED_BANKING_APPS.map { app ->
            try {
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                app.copy(
                    appName = appName,
                    icon = icon,
                    isInstalled = true,
                    launchIntent = launchIntent
                )
            } catch (_: Exception) {
                app.copy(isInstalled = false)
            }
        }
    }

    fun launchBankingApp(context: Context, app: BankingAppInfo): Boolean {
        return try {
            if (app.launchIntent != null) {
                app.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(app.launchIntent)
                true
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } else {
                    // Open Google Play Store page
                    val playStoreIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${app.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(playStoreIntent)
                    true
                }
            }
        } catch (_: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
