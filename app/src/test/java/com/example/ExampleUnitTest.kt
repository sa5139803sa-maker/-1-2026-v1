package com.example

import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.data.PaymentEntity
import com.example.data.PaymentStatus
import com.example.data.SaleEntity
import com.example.data.SaleWithPayments
import com.example.ui.AnalyticsSummary
import com.example.ui.CustomerDebtSummary
import com.example.ui.DailySalesPeriod
import com.example.ui.buildExpenseCategoryBreakdown
import com.example.ui.computeDailyPeriodBounds
import com.example.ui.components.computeFifoPreview
import com.example.util.LedgerFormatters
import com.example.util.LedgerJsonBackup
import com.example.util.UnpaidExcelExporter
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `multi-part arabic name search matches across all name parts`() {
        val fullName = "أحمد محمد خالد أبو سمرة"
        assertTrue(LedgerFormatters.matchesAllNameParts(fullName, "أحمد سمرة"))
        assertTrue(LedgerFormatters.matchesAllNameParts(fullName, "احمد خالد"))
        assertTrue(LedgerFormatters.matchesAllNameParts(fullName, "سمرة محمد"))
        assertFalse(LedgerFormatters.matchesAllNameParts(fullName, "أحمد علي"))
    }

    @Test
    fun `fifo account payment settles oldest invoices first`() {
        val s3Newest = SaleWithPayments(
            SaleEntity(id = 3L, customerName = "خالد", totalAmount = 100.0, createdAtMillis = 3000L),
            emptyList(),
        )
        val s1Oldest = SaleWithPayments(
            SaleEntity(id = 1L, customerName = "خالد", totalAmount = 40.0, createdAtMillis = 1000L),
            emptyList(),
        )
        val s2Middle = SaleWithPayments(
            SaleEntity(id = 2L, customerName = "خالد", totalAmount = 60.0, createdAtMillis = 2000L),
            emptyList(),
        )

        val steps = computeFifoPreview(listOf(s3Newest, s1Oldest, s2Middle), 80.0)
        assertEquals(2, steps.size)

        // Oldest 40 ₪ invoice closed completely
        assertEquals(1000L, steps[0].saleMillis)
        assertEquals(40.0, steps[0].appliedAmount, 0.001)
        assertEquals(0.0, steps[0].newRemaining, 0.001)
        assertTrue(steps[0].willClose)

        // Second 60 ₪ invoice receives remaining 40 ₪, leaving 20 ₪
        assertEquals(2000L, steps[1].saleMillis)
        assertEquals(40.0, steps[1].appliedAmount, 0.001)
        assertEquals(20.0, steps[1].newRemaining, 0.001)
        assertFalse(steps[1].willClose)
    }

    @Test
    fun `expense category breakdown and net profit compute accurately`() {
        val expenses = listOf(
            ExpenseEntity(id = 1L, title = "بضاعة", category = ExpenseCategory.GOODS.name, amount = 300.0),
            ExpenseEntity(id = 2L, title = "كهرباء", category = ExpenseCategory.UTILITIES.name, amount = 100.0),
            ExpenseEntity(id = 3L, title = "بضاعة إضافية", category = ExpenseCategory.GOODS.name, amount = 100.0),
        )
        val breakdown = buildExpenseCategoryBreakdown(expenses)
        assertEquals(2, breakdown.size)
        assertEquals(ExpenseCategory.GOODS, breakdown[0].category)
        assertEquals(400.0, breakdown[0].amount, 0.001)
        assertEquals(2, breakdown[0].count)
        assertEquals(0.8f, breakdown[0].share, 0.001f)

        val summary = AnalyticsSummary(
            totalSales = 1200.0,
            totalCollected = 850.0,
            totalRemaining = 350.0,
            totalExpenses = 500.0,
            expensesCount = 3,
            salesCount = 5,
        )
        assertEquals(350.0, summary.netCollectedProfit, 0.001)
        assertEquals(700.0, summary.netSalesProfit, 0.001)
    }

    @Test
    fun `daily hero period bounds compute today yesterday week and month`() {
        val now = 1_763_890_000_000L // fixed sample timestamp
        val todayStart = LedgerFormatters.startOfDayMillis(now)
        val todayEnd = LedgerFormatters.endOfDayMillis(now)

        val (tStart, tEnd) = computeDailyPeriodBounds(DailySalesPeriod.TODAY, todayStart, now)
        assertEquals(todayStart, tStart)
        assertEquals(todayEnd, tEnd)

        val yesterdayStart = LedgerFormatters.addDays(todayStart, -1)
        val (yStart, yEnd) = computeDailyPeriodBounds(DailySalesPeriod.YESTERDAY, todayStart, now)
        assertEquals(yesterdayStart, yStart)
        assertEquals(LedgerFormatters.endOfDayMillis(yesterdayStart), yEnd)

        val (wStart, wEnd) = computeDailyPeriodBounds(DailySalesPeriod.WEEK, todayStart, now)
        assertEquals(LedgerFormatters.addDays(todayStart, -6), wStart)
        assertEquals(todayEnd, wEnd)

        val (mStart, mEnd) = computeDailyPeriodBounds(DailySalesPeriod.MONTH, todayStart, now)
        assertEquals(LedgerFormatters.startOfMonthMillis(now), mStart)
        assertEquals(todayEnd, mEnd)
    }

    @Test
    fun `json backup round-trip preserves arabic text special characters and nested payments`() {
        val sale = SaleWithPayments(
            sale = SaleEntity(
                id = 7L,
                customerName = "أحمد \"أبو خالد\" محمد",
                totalAmount = 345.75,
                note = "سطر أول\nسطر ثاني",
                createdAtMillis = 1_763_800_000_000L,
            ),
            payments = listOf(
                PaymentEntity(
                    id = 21L,
                    saleId = 7L,
                    amount = 100.25,
                    paidAtMillis = 1_763_805_000_000L,
                    note = "دفعة أولى",
                )
            ),
        )
        val expense = ExpenseEntity(
            id = 4L,
            title = "فاتورة كهرباء \\ شهر",
            category = ExpenseCategory.UTILITIES.name,
            amount = 180.5,
            note = "فرع رئيسي",
            createdAtMillis = 1_763_810_000_000L,
        )

        val backup = LedgerJsonBackup.fromEntities(
            sales = listOf(sale),
            expenses = listOf(expense),
            exportedAtMillis = 1_763_890_000_000L,
        )
        val json = LedgerJsonBackup.toJsonString(backup)
        val parsed = LedgerJsonBackup.parseJsonString(json)

        assertEquals(1_763_890_000_000L, parsed.exportedAtMillis)
        assertEquals(1, parsed.sales.size)
        assertEquals("أحمد \"أبو خالد\" محمد", parsed.sales[0].customerName)
        assertEquals(345.75, parsed.sales[0].totalAmount, 0.001)
        assertEquals("سطر أول\nسطر ثاني", parsed.sales[0].note)
        assertEquals(1, parsed.sales[0].payments.size)
        assertEquals(100.25, parsed.sales[0].payments[0].amount, 0.001)
        assertEquals(1, parsed.expenses.size)
        assertEquals("فاتورة كهرباء \\ شهر", parsed.expenses[0].title)
        assertEquals(ExpenseCategory.UTILITIES.name, parsed.expenses[0].category)
        assertEquals(180.5, parsed.expenses[0].amount, 0.001)
    }

    @Test
    fun `unpaid excel exporter builds valid rtl xlsx archive with customer and invoice sheets`() {
        val sale = SaleWithPayments(
            sale = SaleEntity(
                id = 12L,
                customerName = "محل النور & الشركاء",
                totalAmount = 500.0,
                note = "تسليم <عاجل>",
                createdAtMillis = 1_763_800_000_000L,
            ),
            payments = listOf(PaymentEntity(id = 1L, saleId = 12L, amount = 150.0)),
        )
        val customerDebt = CustomerDebtSummary(
            customerName = "محل النور & الشركاء",
            totalRemaining = 350.0,
            totalAmount = 500.0,
            totalPaid = 150.0,
            openSalesCount = 1,
            oldestSaleMillis = 1_763_800_000_000L,
            sales = listOf(sale),
        )

        val bytes = UnpaidExcelExporter.buildUnpaidWorkbookBytes(
            customerDebts = listOf(customerDebt),
            unpaidSales = listOf(sale),
        )
        assertTrue(bytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))

        val sheet1 = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet1.contains("rightToLeft=\"1\""))
        assertTrue(sheet1.contains("محل النور &amp; الشركاء"))
        assertTrue(sheet1.contains("350.0"))

        val sheet2 = entries.getValue("xl/worksheets/sheet2.xml")
        assertTrue(sheet2.contains("تسليم &lt;عاجل&gt;"))
        assertTrue(sheet2.contains("دفعة جزئية"))
    }
}
