package com.walletscholer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val salaryAmount: Double = 0.0,
    val salaryDate: Int = 1,
    val autoCreditSalary: Boolean = false,
    val lastSalaryCreditedMonth: String = "",
    val notifMaster: Boolean = true,
    val notifThresholdsJson: String = "{\"75\":true,\"90\":true,\"100\":true,\"Over budget\":true}",
    val isDarkTheme: Boolean = true,
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val isLoggedIn: Boolean = false,
    val loginProvider: String = "GUEST",
    val syncEnabled: Boolean = false,
    val googleSheetId: String = "1ITMN0Zz5vg0vTECz_Uty__-IKneTCtu1Fw-lYe2Ic_M",
    val googleSheetUrl: String = "https://docs.google.com/spreadsheets/d/1ITMN0Zz5vg0vTECz_Uty__-IKneTCtu1Fw-lYe2Ic_M/edit",
    val lastSyncTime: String = "",
    val syncStatus: String = "NOT_SYNCED",
    val biometricLockEnabled: Boolean = false
)
