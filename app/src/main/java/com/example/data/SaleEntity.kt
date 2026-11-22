package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerName: String,
    val totalAmount: Double,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
