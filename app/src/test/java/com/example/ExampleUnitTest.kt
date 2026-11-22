package com.example

import com.example.data.PaymentEntity
import com.example.data.PaymentStatus
import com.example.data.SaleEntity
import com.example.data.SaleWithPayments
import com.example.util.LedgerFormatters
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `sale status and remaining balance compute accurately`() {
        val sale = SaleEntity(id = 1L, customerName = "سعيد", totalAmount = 120.5)
        val unpaid = SaleWithPayments(sale, emptyList())
        assertEquals(PaymentStatus.UNPAID, unpaid.status)
        assertEquals(120.5, unpaid.remainingAmount, 0.001)

        val partial = SaleWithPayments(sale, listOf(PaymentEntity(saleId = 1L, amount = 50.0)))
        assertEquals(PaymentStatus.PARTIAL, partial.status)
        assertEquals(70.5, partial.remainingAmount, 0.001)

        val paid = SaleWithPayments(
            sale,
            listOf(
                PaymentEntity(saleId = 1L, amount = 50.0),
                PaymentEntity(saleId = 1L, amount = 70.5),
            ),
        )
        assertEquals(PaymentStatus.PAID, paid.status)
        assertEquals(0.0, paid.remainingAmount, 0.001)
    }

    @Test
    fun `shekel formatter formats whole and decimal amounts cleanly`() {
        assertEquals("\u200E150 ₪", LedgerFormatters.formatShekel(150.0))
        assertEquals("\u200E150.50 ₪", LedgerFormatters.formatShekel(150.5))
    }
}
