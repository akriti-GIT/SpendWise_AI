package com.example.spendwise.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String, // Unique, immutable UUID
    val name: String,
    val email: String, // Normalized (trimmed, lowercase)
    val passwordHash: String, // SHA-256 hash
    val securityPin: String = "1234",
    val collegeId: String = "",
    val currency: String = "₹",
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val monthlyBudget: Double = 0.0,
    val isAppLockEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
