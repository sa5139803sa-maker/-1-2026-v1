package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val labelAr: String) {
    GOODS("بضاعة ومشتريات"),
    RENT("إيجار"),
    UTILITIES("كهرباء ومياه"),
    SALARIES("رواتب وأجور"),
    TRANSPORT("نقل ومواصلات"),
    MAINTENANCE("صيانة"),
    OTHER("أخرى");

    companion object {
        fun fromKey(key: String): ExpenseCategory =
            entries.firstOrNull { it.name == key } ?: OTHER
    }
}

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["category"]),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val categoryEnum: ExpenseCategory
        get() = ExpenseCategory.fromKey(category)
}
