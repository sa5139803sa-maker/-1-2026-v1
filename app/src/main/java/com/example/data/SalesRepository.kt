package com.example.data

import androidx.room.withTransaction
import com.example.util.LedgerBackupData
import com.example.util.LedgerFormatters
import com.example.util.LedgerJsonBackup
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow

data class ImportBackupResult(
    val importedSales: Int,
    val importedPayments: Int,
    val importedExpenses: Int,
    val skippedDuplicates: Int,
)

class SalesRepository(
    private val salesDao: SalesDao,
    private val expenseDao: ExpenseDao,
    private val database: AppDatabase? = null,
) {

    fun observeAllSales(): Flow<List<SaleWithPayments>> = salesDao.observeAllSalesWithPayments()

    fun observeRecentCustomerNames(): Flow<List<String>> = salesDao.observeRecentCustomerNames()

    fun observeAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAllExpenses()

    fun observeRecentExpenseTitles(): Flow<List<String>> = expenseDao.observeRecentExpenseTitles()

    suspend fun getAllSalesOnce(): List<SaleWithPayments> = salesDao.getAllSalesWithPaymentsOnce()

    suspend fun exportJsonBackupString(): String {
        val sales = salesDao.getAllSalesWithPaymentsOnce()
        val expenses = expenseDao.getAllExpensesOnce()
        val backup = LedgerJsonBackup.fromEntities(sales = sales, expenses = expenses)
        return LedgerJsonBackup.toJsonString(backup)
    }

    suspend fun importBackup(
        backup: LedgerBackupData,
        replaceExisting: Boolean,
    ): ImportBackupResult {
        suspend fun performImport(): ImportBackupResult {
            var importedSales = 0
            var importedPayments = 0
            var importedExpenses = 0
            var skippedDuplicates = 0

            val existingSaleSignatures = HashSet<Triple<String, Long, Long>>()
            val existingExpenseSignatures = HashSet<Triple<String, Long, Long>>()

            if (replaceExisting) {
                salesDao.clearAllPayments()
                salesDao.clearAllSales()
                expenseDao.clearAllExpenses()
            } else {
                salesDao.getAllSalesWithPaymentsOnce().forEach { item ->
                    existingSaleSignatures += Triple(
                        LedgerFormatters.normalizeArabicText(item.sale.customerName),
                        (item.sale.totalAmount * 100.0).roundToLong(),
                        item.sale.createdAtMillis,
                    )
                }
                expenseDao.getAllExpensesOnce().forEach { exp ->
                    existingExpenseSignatures += Triple(
                        LedgerFormatters.normalizeArabicText(exp.title),
                        (exp.amount * 100.0).roundToLong(),
                        exp.createdAtMillis,
                    )
                }
            }

            backup.sales.forEach { bSale ->
                val sig = Triple(
                    LedgerFormatters.normalizeArabicText(bSale.customerName),
                    (bSale.totalAmount * 100.0).roundToLong(),
                    bSale.createdAtMillis,
                )
                if (!replaceExisting && sig in existingSaleSignatures) {
                    skippedDuplicates++
                } else {
                    val newSaleId = salesDao.insertSale(
                        SaleEntity(
                            customerName = bSale.customerName.trim(),
                            totalAmount = bSale.totalAmount,
                            note = bSale.note.trim(),
                            createdAtMillis = bSale.createdAtMillis,
                        )
                    )
                    importedSales++
                    existingSaleSignatures += sig
                    if (bSale.payments.isNotEmpty()) {
                        val mappedPayments = bSale.payments.map { bp ->
                            PaymentEntity(
                                saleId = newSaleId,
                                amount = bp.amount,
                                paidAtMillis = bp.paidAtMillis,
                                note = bp.note.trim(),
                            )
                        }
                        salesDao.insertPaymentsBatch(mappedPayments)
                        importedPayments += mappedPayments.size
                    }
                }
            }

            backup.expenses.forEach { bExp ->
                val sig = Triple(
                    LedgerFormatters.normalizeArabicText(bExp.title),
                    (bExp.amount * 100.0).roundToLong(),
                    bExp.createdAtMillis,
                )
                if (!replaceExisting && sig in existingExpenseSignatures) {
                    skippedDuplicates++
                } else {
                    expenseDao.insertExpense(
                        ExpenseEntity(
                            title = bExp.title.trim(),
                            category = ExpenseCategory.fromKey(bExp.category).name,
                            amount = bExp.amount,
                            note = bExp.note.trim(),
                            createdAtMillis = bExp.createdAtMillis,
                        )
                    )
                    importedExpenses++
                    existingExpenseSignatures += sig
                }
            }

            return ImportBackupResult(
                importedSales = importedSales,
                importedPayments = importedPayments,
                importedExpenses = importedExpenses,
                skippedDuplicates = skippedDuplicates,
            )
        }

        return if (database != null) {
            database.withTransaction { performImport() }
        } else {
            performImport()
        }
    }

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
