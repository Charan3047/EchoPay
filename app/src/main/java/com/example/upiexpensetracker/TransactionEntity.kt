package com.example.upiexpensetracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,
    val merchant: String?,
    val transactionType: String,
    val paymentMethod: String?,
    val category: String,
    val confidence: Double,
    val needsReview: Boolean,
    val timestamp: Long
)