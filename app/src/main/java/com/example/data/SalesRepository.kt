package com.example.data

import kotlinx.coroutines.flow.Flow

class SalesRepository(
    private val salesDao: SalesDao,
    private val expenseDao: ExpenseDao,
) {

    fun observeAllSales(): Flow<List<SaleWithPayments>> = salesDao.observeAllSalesWithPayments()

    fun observeRecentCustomerNames(): Flow<List<String>> = salesDao.observeRecentCustomerNames()

    fun observeAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()

    fun observeRecentExpenseTitles(): Flow<List<String>> = expenseDao.observeRecentExpenseTitles()

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
        val payments = sales.mapNotNull { item ->
            val remaining = item.remainingAmount
            if (remaining > 0.0) {
                PaymentEntity(
                    saleId = item.sale.id,
                    amount = remaining,
                    paidAtMillis = now,
                )
            } else {
                null
            }
        }
        if (payments.isNotEmpty()) {
            salesDao.insertPaymentsBatch(payments)
        }
    }

    suspend fun distributePaymentAcrossCustomerSales(
        sales: List<SaleWithPayments>,
        paymentAmount: Double,
    ): Pair<Int, Double> {
        if (paymentAmount <= 0.0) return 0 to 0.0
        val oldestFirst = sales
            .filter { it.remainingAmount > 0.0 }
            .sortedWith(compareBy<SaleWithPayments> { it.sale.createdAtMillis }.thenBy { it.sale.id })

        var remainingToDistribute = paymentAmount
        val now = System.currentTimeMillis()
        val paymentsToInsert = mutableListOf<PaymentEntity>()
        var appliedTotal = 0.0

        for (saleItem in oldestFirst) {
            if (remainingToDistribute < 0.01) break
            val applyAmount = minOf(remainingToDistribute, saleItem.remainingAmount)
            if (applyAmount >= 0.01) {
                paymentsToInsert += PaymentEntity(
                    saleId = saleItem.sale.id,
                    amount = applyAmount,
                    paidAtMillis = now,
                    note = "دفعة على الحساب",
                )
                remainingToDistribute -= applyAmount
                appliedTotal += applyAmount
            }
        }

        if (paymentsToInsert.isNotEmpty()) {
            salesDao.insertPaymentsBatch(paymentsToInsert)
        }
        return paymentsToInsert.size to appliedTotal
    }

    suspend fun deletePayment(paymentId: Long) {
        salesDao.deletePaymentById(paymentId)
    }

    suspend fun deleteSale(saleId: Long) {
        salesDao.deleteSaleById(saleId)
    }

    suspend fun addExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        note: String,
        createdAtMillis: Long,
    ): Long {
        return expenseDao.insertExpense(
            ExpenseEntity(
                title = title.trim(),
                category = category.name,
                amount = amount,
                note = note.trim(),
                createdAtMillis = createdAtMillis,
            )
        )
    }

    suspend fun updateExpense(
        existing: ExpenseEntity,
        title: String,
        category: ExpenseCategory,
        amount: Double,
        note: String,
    ) {
        expenseDao.updateExpense(
            existing.copy(
                title = title.trim(),
                category = category.name,
                amount = amount,
                note = note.trim(),
            )
        )
    }

    suspend fun deleteExpense(expenseId: Long) {
        expenseDao.deleteExpenseById(expenseId)
    }
}
