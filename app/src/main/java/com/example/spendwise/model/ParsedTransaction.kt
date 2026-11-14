package com.example.spendwise.model

import java.util.UUID

data class ParsedTransaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val description: String,
    val category: String,
    val date: Long,
    val type: TransactionType = TransactionType.EXPENSE,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val referenceNumber: String? = null,
    val isLikelyDuplicate: Boolean = false,
    val isSelected: Boolean = true
)
