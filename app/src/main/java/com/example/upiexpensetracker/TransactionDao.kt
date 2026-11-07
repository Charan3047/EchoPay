package com.example.upiexpensetracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(
        transaction: TransactionEntity
    )

    @Query(
        """SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE amount = :amount
              AND transactionType = :transactionType
              AND merchant IS :merchant
              AND paymentMethod IS :paymentMethod
              AND timestamp BETWEEN :timestamp - 30000 AND :timestamp + 30000
        )"""
    )
    suspend fun hasRecentMatchingTransaction(
        amount: Double,
        transactionType: String,
        merchant: String?,
        paymentMethod: String?,
        timestamp: Long
    ): Boolean

    @Transaction
    suspend fun insertIfNotDuplicate(transaction: TransactionEntity): Boolean {
        val duplicate = hasRecentMatchingTransaction(
            amount = transaction.amount,
            transactionType = transaction.transactionType,
            merchant = transaction.merchant,
            paymentMethod = transaction.paymentMethod,
            timestamp = transaction.timestamp
        )
        if (duplicate) return false
        insertTransaction(transaction)
        return true
    }

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'DEBIT'")
    fun getTotalExpenses(): Flow<Double?>

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("UPDATE transactions SET category = :category, needsReview = 0 WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)
}
