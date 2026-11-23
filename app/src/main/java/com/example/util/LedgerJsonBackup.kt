package com.example.util

import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.data.SaleWithPayments

data class BackupPayment(
    val id: Long = 0L,
    val amount: Double,
    val paidAtMillis: Long,
    val note: String = "",
)

data class BackupSale(
    val id: Long = 0L,
    val customerName: String,
    val totalAmount: Double,
    val note: String = "",
    val createdAtMillis: Long,
    val payments: List<BackupPayment> = emptyList(),
)

data class BackupExpense(
    val id: Long = 0L,
    val title: String,
    val category: String = ExpenseCategory.OTHER.name,
    val amount: Double,
    val note: String = "",
    val createdAtMillis: Long,
)

data class LedgerBackupData(
    val format: String = BACKUP_FORMAT,
    val version: Int = BACKUP_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val sales: List<BackupSale> = emptyList(),
    val expenses: List<BackupExpense> = emptyList(),
) {
    val totalPaymentsCount: Int get() = sales.sumOf { it.payments.size }

    companion object {
        const val BACKUP_FORMAT = "sales_ledger_backup"
        const val BACKUP_VERSION = 2
    }
}

object LedgerJsonBackup {

    fun fromEntities(
        sales: List<SaleWithPayments>,
        expenses: List<ExpenseEntity>,
        exportedAtMillis: Long = System.currentTimeMillis(),
    ): LedgerBackupData {
        return LedgerBackupData(
            format = LedgerBackupData.BACKUP_FORMAT,
            version = LedgerBackupData.BACKUP_VERSION,
            exportedAtMillis = exportedAtMillis,
            sales = sales.map { item ->
                BackupSale(
                    id = item.sale.id,
                    customerName = item.sale.customerName,
                    totalAmount = item.sale.totalAmount,
                    note = item.sale.note,
                    createdAtMillis = item.sale.createdAtMillis,
                    payments = item.payments.map { p ->
                        BackupPayment(
                            id = p.id,
                            amount = p.amount,
                            paidAtMillis = p.paidAtMillis,
                            note = p.note,
                        )
                    },
                )
            },
            expenses = expenses.map { e ->
                BackupExpense(
                    id = e.id,
                    title = e.title,
                    category = e.category,
                    amount = e.amount,
                    note = e.note,
                    createdAtMillis = e.createdAtMillis,
                )
            },
        )
    }

    fun toJsonString(backup: LedgerBackupData): String {
        val sb = StringBuilder(2048)
        sb.append("{\n")
        sb.append("  \"format\": ").append(quoteJson(backup.format)).append(",\n")
        sb.append("  \"version\": ").append(backup.version).append(",\n")
        sb.append("  \"exportedAtMillis\": ").append(backup.exportedAtMillis).append(",\n")
        sb.append("  \"sales\": [")
        if (backup.sales.isNotEmpty()) sb.append('\n')
        backup.sales.forEachIndexed { sIdx, sale ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(sale.id).append(",\n")
            sb.append("      \"customerName\": ").append(quoteJson(sale.customerName)).append(",\n")
            sb.append("      \"totalAmount\": ").append(formatJsonDouble(sale.totalAmount)).append(",\n")
            sb.append("      \"note\": ").append(quoteJson(sale.note)).append(",\n")
            sb.append("      \"createdAtMillis\": ").append(sale.createdAtMillis).append(",\n")
            sb.append("      \"payments\": [")
            if (sale.payments.isNotEmpty()) sb.append('\n')
            sale.payments.forEachIndexed { pIdx, payment ->
                sb.append("        {\n")
                sb.append("          \"id\": ").append(payment.id).append(",\n")
                sb.append("          \"amount\": ").append(formatJsonDouble(payment.amount)).append(",\n")
                sb.append("          \"paidAtMillis\": ").append(payment.paidAtMillis).append(",\n")
                sb.append("          \"note\": ").append(quoteJson(payment.note)).append('\n')
                sb.append("        }")
                if (pIdx < sale.payments.lastIndex) sb.append(',')
                sb.append('\n')
            }
            if (sale.payments.isNotEmpty()) sb.append("      ")
            sb.append("]\n")
            sb.append("    }")
            if (sIdx < backup.sales.lastIndex) sb.append(',')
            sb.append('\n')
        }
        if (backup.sales.isNotEmpty()) sb.append("  ")
        sb.append("],\n")
        sb.append("  \"expenses\": [")
        if (backup.expenses.isNotEmpty()) sb.append('\n')
        backup.expenses.forEachIndexed { eIdx, exp ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(exp.id).append(",\n")
            sb.append("      \"title\": ").append(quoteJson(exp.title)).append(",\n")
            sb.append("      \"category\": ").append(quoteJson(exp.category)).append(",\n")
            sb.append("      \"amount\": ").append(formatJsonDouble(exp.amount)).append(",\n")
            sb.append("      \"note\": ").append(quoteJson(exp.note)).append(",\n")
            sb.append("      \"createdAtMillis\": ").append(exp.createdAtMillis).append('\n')
            sb.append("    }")
            if (eIdx < backup.expenses.lastIndex) sb.append(',')
            sb.append('\n')
        }
        if (backup.expenses.isNotEmpty()) sb.append("  ")
        sb.append("]\n")
        sb.append('}')
        return sb.toString()
    }

    fun parseJsonString(json: String): LedgerBackupData {
        val cleaned = json.removePrefix("\uFEFF").trim()
        require(cleaned.isNotEmpty()) { "الملف فارغ" }
        val rootAny = JsonReader(cleaned).readValue()
        val root = rootAny as? Map<*, *> ?: throw IllegalArgumentException("صيغة ملف JSON غير صالحة")

        val format = (root["format"] as? String).orEmpty()
        if (format.isNotEmpty() && format != LedgerBackupData.BACKUP_FORMAT) {
            throw IllegalArgumentException("صيغة الملف غير مدعومة")
        }
        val hasSalesKey = root.containsKey("sales")
        val hasExpensesKey = root.containsKey("expenses")
        if (!hasSalesKey && !hasExpensesKey) {
            throw IllegalArgumentException("الملف لا يحتوي على بيانات مبيعات أو مصروفات")
        }

        val version = (root["version"] as? Number)?.toInt() ?: 1
        val exportedAt = (root["exportedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()

        val salesList = (root["sales"] as? List<*>).orEmpty().mapNotNull { entry ->
            val obj = entry as? Map<*, *> ?: return@mapNotNull null
            val customerName = (obj["customerName"] as? String)?.trim().orEmpty()
            val totalAmount = (obj["totalAmount"] as? Number)?.toDouble() ?: return@mapNotNull null
            if (customerName.isEmpty() || totalAmount <= 0.0) return@mapNotNull null

            val payments = (obj["payments"] as? List<*>).orEmpty().mapNotNull { pEntry ->
                val pObj = pEntry as? Map<*, *> ?: return@mapNotNull null
                val pAmt = (pObj["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
                if (pAmt <= 0.0) return@mapNotNull null
                BackupPayment(
                    id = (pObj["id"] as? Number)?.toLong() ?: 0L,
                    amount = pAmt,
                    paidAtMillis = (pObj["paidAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    note = (pObj["note"] as? String).orEmpty(),
                )
            }
            BackupSale(
                id = (obj["id"] as? Number)?.toLong() ?: 0L,
                customerName = customerName,
                totalAmount = totalAmount,
                note = (obj["note"] as? String).orEmpty(),
                createdAtMillis = (obj["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                payments = payments,
            )
        }

        val expensesList = (root["expenses"] as? List<*>).orEmpty().mapNotNull { entry ->
            val obj = entry as? Map<*, *> ?: return@mapNotNull null
            val title = (obj["title"] as? String)?.trim().orEmpty()
            val amount = (obj["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
            if (title.isEmpty() || amount <= 0.0) return@mapNotNull null
            val rawCat = (obj["category"] as? String).orEmpty()
            val validCategory = ExpenseCategory.fromKey(rawCat).name
            BackupExpense(
                id = (obj["id"] as? Number)?.toLong() ?: 0L,
                title = title,
                category = validCategory,
                amount = amount,
                note = (obj["note"] as? String).orEmpty(),
                createdAtMillis = (obj["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            )
        }

        return LedgerBackupData(
            format = format.ifEmpty { LedgerBackupData.BACKUP_FORMAT },
            version = version,
            exportedAtMillis = exportedAt,
            sales = salesList,
            expenses = expensesList,
        )
    }

    private fun formatJsonDouble(value: Double): String {
        return if (value.isFinite()) value.toString() else "0.0"
    }

    private fun quoteJson(text: String): String {
        val sb = StringBuilder(text.length + 2)
        sb.append('"')
        for (ch in text) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) {
                    sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class JsonReader(private val input: String) {
        private var pos = 0

        fun readValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) throw IllegalArgumentException("نهاية غير متوقعة لملف JSON")
            return when (val c = input[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> readNull()
                '-', in '0'..'9' -> readNumber()
                else -> throw IllegalArgumentException("رمز غير متوقع '$c' عند الموضع $pos")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                val value = readValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw IllegalArgumentException("متوقع ',' أو '}' في JSON")
                }
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(readValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw IllegalArgumentException("متوقع ',' أو ']' في JSON")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val c = input[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) throw IllegalArgumentException("تسلسل هروب غير مكتمل")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) throw IllegalArgumentException("رمز unicode غير صالح")
                                val hex = input.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("حرف هروب غير معروف \\$esc")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw IllegalArgumentException("نص JSON غير مغلق")
        }

        private fun readNumber(): Number {
            val start = pos
            if (input[pos] == '-') pos++
            while (pos < input.length && input[pos] in '0'..'9') pos++
            var isFloat = false
            if (pos < input.length && input[pos] == '.') {
                isFloat = true
                pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                isFloat = true
                pos++
                if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                while (pos < input.length && input[pos] in '0'..'9') pos++
            }
            val raw = input.substring(start, pos)
            return if (isFloat) raw.toDouble() else raw.toLongOrNull() ?: raw.toDouble()
        }

        private fun readBoolean(): Boolean {
            return if (input.startsWith("true", pos)) {
                pos += 4
                true
            } else if (input.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                throw IllegalArgumentException("قيمة منطقية غير صالحة في JSON")
            }
        }

        private fun readNull(): Any? {
            if (input.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw IllegalArgumentException("قيمة null غير صالحة في JSON")
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= input.length) throw IllegalArgumentException("نهاية غير متوقعة لملف JSON")
            return input[pos]
        }

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                throw IllegalArgumentException("متوقع '$expected' في JSON")
            }
            pos++
        }
    }
}
