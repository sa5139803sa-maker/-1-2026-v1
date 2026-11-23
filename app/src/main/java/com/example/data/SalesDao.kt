package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAtMillis DESC")
    fun observeAllSalesWithPayments(): Flow<List<SaleWithPayments>>

    @Query("SELECT DISTINCT customerName FROM sales ORDER BY createdAtMillis DESC")
    fun observeRecentCustomerNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Transaction
    suspend fun insertPaymentsBatch(payments: List<PaymentEntity>) {
        payments.forEach { insertPayment(it) }
    }

    @Query("DELETE FROM payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteSaleById(saleId: Long)

    @Transaction
    suspend fun insertSaleWithInitialPayment(
        sale: SaleEntity,
        initialPaymentAmount: Double,
    ): Long {
        val saleId = insertSale(sale)
        if (initialPaymentAmount > 0.0) {
            insertPayment(
                PaymentEntity(
                    saleId = saleId,
                    amount = initialPaymentAmount.coerceAtMost(sale.totalAmount),
                    paidAtMillis = sale.createdAtMillis,
                )
            )
        }
        return saleId
    }
}
