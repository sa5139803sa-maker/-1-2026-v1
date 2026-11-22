package com.example.data

import androidx.room.Embedded
import androidx.room.Relation
import kotlin.math.max

enum class PaymentStatus {
    PAID,
    PARTIAL,
    UNPAID,
}

data class SaleWithPayments(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId",
    )
    val payments: List<PaymentEntity>,
) {
    val paidAmount: Double
        get() {
            val sum = payments.sumOf { it.amount }
            return if (sum >= sale.totalAmount - 0.009) {
                sale.totalAmount
            } else {
                max(0.0, sum)
            }
        }

    val remainingAmount: Double
        get() {
            val rem = sale.totalAmount - paidAmount
            return if (rem < 0.01) 0.0 else rem
        }

    val status: PaymentStatus
        get() = when {
            remainingAmount <= 0.0 -> PaymentStatus.PAID
            paidAmount > 0.009 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.UNPAID
        }

    val progressFraction: Float
        get() = if (sale.totalAmount <= 0.0) {
            1f
        } else {
            (paidAmount / sale.totalAmount).toFloat().coerceIn(0f, 1f)
        }
}
