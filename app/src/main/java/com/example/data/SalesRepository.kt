package com.example.data

import kotlinx.coroutines.flow.Flow

class SalesRepository(private val salesDao: SalesDao) {

    fun observeAllSales(): Flow<List<SaleWithPayments>> = salesDao.observeAllSalesWithPayments()

    fun observeRecentCustomerNames(): Flow<List<String>> = salesDao.observeRecentCustomerNames()

    suspend fun addSale(
        customerName: String,
        totalAmount: Double,
        initialPaymentAmount: Double,
        note: String,
        createdAtMillis: Long,
    ): Long {
        val cleanName = customerName.trim()
        val cleanNote = note.trim()
        val sale = SaleEntity(
            customerName = cleanName,
            totalAmount = totalAmount,
            note = cleanNote,
            createdAtMillis = createdAtMillis,
        )
        return salesDao.insertSaleWithInitialPayment(
            sale = sale,
            initialPaymentAmount = initialPaymentAmount.coerceIn(0.0, totalAmount),
        )
    }

    suspend fun updateSale(
        existingSale: SaleEntity,
        customerName: String,
        totalAmount: Double,
        note: String,
    ) {
        salesDao.updateSale(
            existingSale.copy(
                customerName = customerName.trim(),
                totalAmount = totalAmount,
                note = note.trim(),
            )
        )
    }

    suspend fun addPayment(
        saleId: Long,
        amount: Double,
        paidAtMillis: Long = System.currentTimeMillis(),
        note: String = "",
    ) {
        if (amount <= 0.0) return
        salesDao.insertPayment(
            PaymentEntity(
                saleId = saleId,
                amount = amount,
                paidAtMillis = paidAtMillis,
                note = note.trim(),
            )
        )
    }

    suspend fun settleSaleFull(saleWithPayments: SaleWithPayments) {
        val remaining = saleWithPayments.remainingAmount
        if (remaining > 0.0) {
            addPayment(saleId = saleWithPayments.sale.id, amount = remaining)
        }
    }

    suspend fun settleMultipleSales(sales: List<SaleWithPayments>) {
        val now = System.currentTimeMillis()
        sales.forEach { item ->
            val remaining = item.remainingAmount
            if (remaining > 0.0) {
                addPayment(saleId = item.sale.id, amount = remaining, paidAtMillis = now)
            }
        }
    }

    suspend fun deletePayment(paymentId: Long) {
        salesDao.deletePaymentById(paymentId)
    }

    suspend fun deleteSale(saleId: Long) {
        salesDao.deleteSaleById(saleId)
    }
}
