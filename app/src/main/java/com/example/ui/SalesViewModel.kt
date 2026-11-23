package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppThemeMode
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.data.PaymentStatus
import com.example.data.SaleWithPayments
import com.example.data.SalesRepository
import com.example.data.ThemePreferences
import com.example.util.LedgerFormatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DAILY,
    UNPAID,
    CUSTOMERS,
    EXPENSES,
    ANALYTICS,
}

enum class ExpenseRange {
    DAY,
    THIS_MONTH,
    ALL_TIME,
    CUSTOM,
}

enum class DailySalesPeriod {
    TODAY,
    YESTERDAY,
    WEEK,
    MONTH,
    CUSTOM_DAY,
}

enum class DailyFilter {
    ALL,
    PAID,
    PARTIAL,
    UNPAID,
}

enum class UnpaidViewMode {
    BY_SALE,
    BY_CUSTOMER,
}

enum class UnpaidSortOrder {
    HIGHEST_AMOUNT,
    OLDEST_FIRST,
    NEWEST_FIRST,
}

enum class CustomerSortOrder {
    MOST_PURCHASES,
    MOST_ORDERS,
    HIGHEST_DEBT,
    RECENT_ACTIVITY,
}

enum class AnalyticsRange {
    LAST_7_DAYS,
    THIS_MONTH,
    ALL_TIME,
    CUSTOM,
}

data class DailySummary(
    val totalSales: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val salesCount: Int = 0,
) {
    val collectionProgress: Float
        get() = if (totalSales <= 0.0) 0f else (totalCollected / totalSales).toFloat().coerceIn(0f, 1f)
}

data class CustomerDebtSummary(
    val customerName: String,
    val totalRemaining: Double,
    val totalAmount: Double,
    val totalPaid: Double,
    val openSalesCount: Int,
    val oldestSaleMillis: Long,
    val sales: List<SaleWithPayments>,
)

data class CustomerPurchaseSummary(
    val customerName: String,
    val totalPurchases: Double,
    val totalPaid: Double,
    val totalRemaining: Double,
    val purchasesCount: Int,
    val lastPurchaseMillis: Long,
    val sales: List<SaleWithPayments>,
    val rank: Int,
) {
    val paidProgress: Float
        get() = if (totalPurchases <= 0.0) 1f else (totalPaid / totalPurchases).toFloat().coerceIn(0f, 1f)
}

data class ChartBucket(
    val label: String,
    val collected: Double,
    val remaining: Double,
) {
    val total: Double get() = collected + remaining
}

data class ExpenseCategoryBreakdown(
    val category: ExpenseCategory,
    val amount: Double,
    val count: Int,
    val share: Float,
)

data class ExpensesSummary(
    val totalExpenses: Double = 0.0,
    val expensesCount: Int = 0,
    val periodCollectedSales: Double = 0.0,
    val netCash: Double = 0.0,
    val categoryBreakdown: List<ExpenseCategoryBreakdown> = emptyList(),
)

data class AnalyticsSummary(
    val totalSales: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val expensesCount: Int = 0,
    val salesCount: Int = 0,
    val paidCount: Int = 0,
    val partialCount: Int = 0,
    val unpaidCount: Int = 0,
    val chartBuckets: List<ChartBucket> = emptyList(),
    val topIndebtedCustomers: List<CustomerDebtSummary> = emptyList(),
    val expenseCategoryBreakdown: List<ExpenseCategoryBreakdown> = emptyList(),
) {
    val collectionRate: Float
        get() = if (totalSales <= 0.0) 0f else (totalCollected / totalSales).toFloat().coerceIn(0f, 1f)

    val netCollectedProfit: Double
        get() = totalCollected - totalExpenses

    val netSalesProfit: Double
        get() = totalSales - totalExpenses
}

data class SalesUiState(
    val isLoading: Boolean = true,
    val selectedTab: AppTab = AppTab.DAILY,
    val dailySalesPeriod: DailySalesPeriod = DailySalesPeriod.TODAY,
    val selectedDayMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val isTodaySelected: Boolean = true,
    val dailyFilter: DailyFilter = DailyFilter.ALL,
    val dailySearchQuery: String = "",
    val dailySummary: DailySummary = DailySummary(),
    val dailySales: List<SaleWithPayments> = emptyList(),
    val allDaySalesCount: Int = 0,
    val unpaidViewMode: UnpaidViewMode = UnpaidViewMode.BY_SALE,
    val unpaidSortOrder: UnpaidSortOrder = UnpaidSortOrder.HIGHEST_AMOUNT,
    val unpaidSearchQuery: String = "",
    val totalUnpaidBalance: Double = 0.0,
    val totalUnpaidInvoicesCount: Int = 0,
    val totalUnpaidCustomersCount: Int = 0,
    val unpaidSales: List<SaleWithPayments> = emptyList(),
    val unpaidByCustomer: List<CustomerDebtSummary> = emptyList(),
    val customersSortOrder: CustomerSortOrder = CustomerSortOrder.MOST_PURCHASES,
    val customersSearchQuery: String = "",
    val customersList: List<CustomerPurchaseSummary> = emptyList(),
    val totalCustomersCount: Int = 0,
    val allCustomersTotalPurchases: Double = 0.0,
    val topBuyer: CustomerPurchaseSummary? = null,
    val expenseRange: ExpenseRange = ExpenseRange.THIS_MONTH,
    val expenseDayMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val isExpenseTodaySelected: Boolean = true,
    val expenseCustomStartMillis: Long = LedgerFormatters.startOfMonthMillis(System.currentTimeMillis()),
    val expenseCustomEndMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val expenseCategoryFilter: ExpenseCategory? = null,
    val expenseSearchQuery: String = "",
    val expensesSummary: ExpensesSummary = ExpensesSummary(),
    val expensesList: List<ExpenseEntity> = emptyList(),
    val allRangeExpensesCount: Int = 0,
    val recentExpenseTitles: List<String> = emptyList(),
    val selectedExpense: ExpenseEntity? = null,
    val isAddExpenseSheetVisible: Boolean = false,
    val analyticsRange: AnalyticsRange = AnalyticsRange.LAST_7_DAYS,
    val analyticsCustomStartMillis: Long = LedgerFormatters.startOfMonthMillis(System.currentTimeMillis()),
    val analyticsCustomEndMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val analyticsSummary: AnalyticsSummary = AnalyticsSummary(),
    val recentCustomers: List<String> = emptyList(),
    val allCustomerSummaries: List<CustomerPurchaseSummary> = emptyList(),
    val selectedSale: SaleWithPayments? = null,
    val selectedCustomerDebt: CustomerDebtSummary? = null,
    val selectedCustomerPurchase: CustomerPurchaseSummary? = null,
    val isAddSaleSheetVisible: Boolean = false,
    val prefilledCustomerName: String = "",
)

private data class FilterInputs(
    val selectedTab: AppTab = AppTab.DAILY,
    val dailySalesPeriod: DailySalesPeriod = DailySalesPeriod.TODAY,
    val selectedDayMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val dailyFilter: DailyFilter = DailyFilter.ALL,
    val dailySearchQuery: String = "",
    val unpaidViewMode: UnpaidViewMode = UnpaidViewMode.BY_SALE,
    val unpaidSortOrder: UnpaidSortOrder = UnpaidSortOrder.HIGHEST_AMOUNT,
    val unpaidSearchQuery: String = "",
    val customersSortOrder: CustomerSortOrder = CustomerSortOrder.MOST_PURCHASES,
    val customersSearchQuery: String = "",
    val expenseRange: ExpenseRange = ExpenseRange.THIS_MONTH,
    val expenseDayMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val expenseCustomStartMillis: Long = LedgerFormatters.startOfMonthMillis(System.currentTimeMillis()),
    val expenseCustomEndMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val expenseCategoryFilter: ExpenseCategory? = null,
    val expenseSearchQuery: String = "",
    val selectedExpenseId: Long? = null,
    val isAddExpenseSheetVisible: Boolean = false,
    val analyticsRange: AnalyticsRange = AnalyticsRange.LAST_7_DAYS,
    val analyticsCustomStartMillis: Long = LedgerFormatters.startOfMonthMillis(System.currentTimeMillis()),
    val analyticsCustomEndMillis: Long = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
    val selectedSaleId: Long? = null,
    val selectedCustomerName: String? = null,
    val selectedCustomerPurchaseName: String? = null,
    val isAddSaleSheetVisible: Boolean = false,
    val prefilledCustomerName: String = "",
)

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SalesRepository = run {
        val db = AppDatabase.getInstance(application)
        SalesRepository(
            salesDao = db.salesDao(),
            expenseDao = db.expenseDao(),
        )
    }
    private val themePreferences: ThemePreferences = ThemePreferences(application)

    private val filterInputs = MutableStateFlow(FilterInputs())

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppThemeMode.SYSTEM,
    )

    val uiState: StateFlow<SalesUiState> = combine(
        repository.observeAllSales(),
        repository.observeRecentCustomerNames(),
        repository.observeAllExpenses(),
        repository.observeRecentExpenseTitles(),
        filterInputs,
    ) { allSales, recentCustomers, allExpenses, recentExpenseTitles, inputs ->
        buildUiState(
            allSales = allSales,
            recentCustomers = recentCustomers,
            allExpenses = allExpenses,
            recentExpenseTitles = recentExpenseTitles,
            inputs = inputs,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SalesUiState(),
    )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun selectTab(tab: AppTab) {
        filterInputs.value = filterInputs.value.copy(selectedTab = tab)
    }

    fun setDailySalesPeriod(period: DailySalesPeriod) {
        val todayStart = LedgerFormatters.startOfDayMillis(System.currentTimeMillis())
        val targetDay = when (period) {
            DailySalesPeriod.YESTERDAY -> LedgerFormatters.addDays(todayStart, -1)
            else -> todayStart
        }
        filterInputs.value = filterInputs.value.copy(
            dailySalesPeriod = period,
            selectedDayMillis = targetDay,
        )
    }

    private fun periodForDay(dayMillis: Long): DailySalesPeriod {
        val todayStart = LedgerFormatters.startOfDayMillis(System.currentTimeMillis())
        val yesterdayStart = LedgerFormatters.addDays(todayStart, -1)
        return when {
            LedgerFormatters.isSameDay(dayMillis, todayStart) -> DailySalesPeriod.TODAY
            LedgerFormatters.isSameDay(dayMillis, yesterdayStart) -> DailySalesPeriod.YESTERDAY
            else -> DailySalesPeriod.CUSTOM_DAY
        }
    }

    fun previousDay() {
        val current = filterInputs.value.selectedDayMillis
        val nextDay = LedgerFormatters.addDays(current, -1)
        filterInputs.value = filterInputs.value.copy(
            dailySalesPeriod = periodForDay(nextDay),
            selectedDayMillis = nextDay,
        )
    }

    fun nextDay() {
        val current = filterInputs.value.selectedDayMillis
        val nextDay = LedgerFormatters.addDays(current, 1)
        filterInputs.value = filterInputs.value.copy(
            dailySalesPeriod = periodForDay(nextDay),
            selectedDayMillis = nextDay,
        )
    }

    fun goToToday() {
        val todayStart = LedgerFormatters.startOfDayMillis(System.currentTimeMillis())
        filterInputs.value = filterInputs.value.copy(
            dailySalesPeriod = DailySalesPeriod.TODAY,
            selectedDayMillis = todayStart,
        )
    }

    fun selectDay(dayMillis: Long) {
        val start = LedgerFormatters.startOfDayMillis(dayMillis)
        filterInputs.value = filterInputs.value.copy(
            dailySalesPeriod = periodForDay(start),
            selectedDayMillis = start,
        )
    }

    fun setDailyFilter(filter: DailyFilter) {
        filterInputs.value = filterInputs.value.copy(dailyFilter = filter)
    }

    fun setDailySearchQuery(query: String) {
        filterInputs.value = filterInputs.value.copy(dailySearchQuery = query)
    }

    fun setUnpaidViewMode(mode: UnpaidViewMode) {
        filterInputs.value = filterInputs.value.copy(unpaidViewMode = mode)
    }

    fun setUnpaidSortOrder(sort: UnpaidSortOrder) {
        filterInputs.value = filterInputs.value.copy(unpaidSortOrder = sort)
    }

    fun setUnpaidSearchQuery(query: String) {
        filterInputs.value = filterInputs.value.copy(unpaidSearchQuery = query)
    }

    fun setCustomersSortOrder(sort: CustomerSortOrder) {
        filterInputs.value = filterInputs.value.copy(customersSortOrder = sort)
    }

    fun setCustomersSearchQuery(query: String) {
        filterInputs.value = filterInputs.value.copy(customersSearchQuery = query)
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        filterInputs.value = filterInputs.value.copy(analyticsRange = range)
    }

    fun setAnalyticsCustomStart(startMillis: Long) {
        val start = LedgerFormatters.startOfDayMillis(startMillis)
        val currentEnd = filterInputs.value.analyticsCustomEndMillis
        filterInputs.value = filterInputs.value.copy(
            analyticsRange = AnalyticsRange.CUSTOM,
            analyticsCustomStartMillis = minOf(start, currentEnd),
            analyticsCustomEndMillis = maxOf(start, currentEnd),
        )
    }

    fun setAnalyticsCustomEnd(endMillis: Long) {
        val end = LedgerFormatters.startOfDayMillis(endMillis)
        val currentStart = filterInputs.value.analyticsCustomStartMillis
        filterInputs.value = filterInputs.value.copy(
            analyticsRange = AnalyticsRange.CUSTOM,
            analyticsCustomStartMillis = minOf(currentStart, end),
            analyticsCustomEndMillis = maxOf(currentStart, end),
        )
    }

    fun openAddSaleSheet(prefilledCustomer: String = "") {
        filterInputs.value = filterInputs.value.copy(
            isAddSaleSheetVisible = true,
            prefilledCustomerName = prefilledCustomer,
        )
    }

    fun closeAddSaleSheet() {
        filterInputs.value = filterInputs.value.copy(
            isAddSaleSheetVisible = false,
            prefilledCustomerName = "",
        )
    }

    fun openSaleDetail(saleId: Long) {
        filterInputs.value = filterInputs.value.copy(selectedSaleId = saleId)
    }

    fun closeSaleDetail() {
        filterInputs.value = filterInputs.value.copy(selectedSaleId = null)
    }

    fun openCustomerDebts(customerName: String) {
        filterInputs.value = filterInputs.value.copy(selectedCustomerName = customerName)
    }

    fun closeCustomerDebts() {
        filterInputs.value = filterInputs.value.copy(selectedCustomerName = null)
    }

    fun openCustomerPurchases(customerName: String) {
        filterInputs.value = filterInputs.value.copy(selectedCustomerPurchaseName = customerName)
    }

    fun closeCustomerPurchases() {
        filterInputs.value = filterInputs.value.copy(selectedCustomerPurchaseName = null)
    }

    fun settleCustomerPurchaseDebts(summary: CustomerPurchaseSummary) {
        val openSales = summary.sales.filter { it.remainingAmount > 0.0 }
        if (openSales.isEmpty()) return
        viewModelScope.launch {
            repository.settleMultipleSales(openSales)
            _events.emit("تم تسديد جميع ذمم ${summary.customerName}")
        }
    }

    fun addSale(
        customerName: String,
        totalAmount: Double,
        initialPaymentAmount: Double,
        note: String,
        dayStartMillis: Long,
    ) {
        viewModelScope.launch {
            val timestamp = LedgerFormatters.combineDayWithCurrentTime(dayStartMillis)
            repository.addSale(
                customerName = customerName,
                totalAmount = totalAmount,
                initialPaymentAmount = initialPaymentAmount,
                note = note,
                createdAtMillis = timestamp,
            )
            closeAddSaleSheet()
            _events.emit("تم تسجيل عملية البيع بنجاح")
        }
    }

    fun updateSale(
        sale: SaleWithPayments,
        customerName: String,
        totalAmount: Double,
        note: String,
    ) {
        viewModelScope.launch {
            repository.updateSale(
                existingSale = sale.sale,
                customerName = customerName,
                totalAmount = totalAmount,
                note = note,
            )
            _events.emit("تم تحديث عملية البيع")
        }
    }

    fun recordPayment(saleWithPayments: SaleWithPayments, amount: Double, note: String = "") {
        viewModelScope.launch {
            val clamped = amount.coerceAtMost(saleWithPayments.remainingAmount)
            if (clamped <= 0.0) return@launch
            repository.addPayment(
                saleId = saleWithPayments.sale.id,
                amount = clamped,
                note = note,
            )
            val isNowFull = (saleWithPayments.remainingAmount - clamped) < 0.01
            _events.emit(
                if (isNowFull) {
                    "تم سداد الفاتورة بالكامل"
                } else {
                    "تم تسجيل دفعة بقيمة ${LedgerFormatters.formatShekel(clamped)}"
                }
            )
        }
    }

    fun settleSaleFull(saleWithPayments: SaleWithPayments) {
        viewModelScope.launch {
            repository.settleSaleFull(saleWithPayments)
            _events.emit("تم سداد الفاتورة بالكامل")
        }
    }

    fun settleCustomerAllDebts(customerSummary: CustomerDebtSummary) {
        viewModelScope.launch {
            repository.settleMultipleSales(customerSummary.sales)
            closeCustomerDebts()
            _events.emit("تم تسديد جميع ذمم ${customerSummary.customerName}")
        }
    }

    fun recordCustomerAccountPayment(
        customerName: String,
        sales: List<SaleWithPayments>,
        amount: Double,
    ) {
        viewModelScope.launch {
            val (count, applied) = repository.distributePaymentAcrossCustomerSales(
                sales = sales,
                paymentAmount = amount,
            )
            if (applied > 0.0) {
                val totalRemainingBefore = sales.sumOf { it.remainingAmount }
                val isFullySettled = (totalRemainingBefore - applied) < 0.01
                if (isFullySettled) {
                    _events.emit("تم تسديد جميع ذمم $customerName بالكامل")
                } else {
                    _events.emit(
                        "تم توزيع ${LedgerFormatters.formatShekel(applied)} على $count فاتورة من الأقدم للأحدث"
                    )
                }
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
            _events.emit("تم حذف الدفعة")
        }
    }

    fun deleteSale(saleId: Long) {
        viewModelScope.launch {
            repository.deleteSale(saleId)
            closeSaleDetail()
            _events.emit("تم حذف عملية البيع")
        }
    }

    // Expense Filters & CRUD Actions
    fun setExpenseRange(range: ExpenseRange) {
        filterInputs.value = filterInputs.value.copy(expenseRange = range)
    }

    fun previousExpenseDay() {
        val current = filterInputs.value.expenseDayMillis
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.DAY,
            expenseDayMillis = LedgerFormatters.addDays(current, -1),
        )
    }

    fun nextExpenseDay() {
        val current = filterInputs.value.expenseDayMillis
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.DAY,
            expenseDayMillis = LedgerFormatters.addDays(current, 1),
        )
    }

    fun goToExpenseToday() {
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.DAY,
            expenseDayMillis = LedgerFormatters.startOfDayMillis(System.currentTimeMillis()),
        )
    }

    fun selectExpenseDay(dayMillis: Long) {
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.DAY,
            expenseDayMillis = LedgerFormatters.startOfDayMillis(dayMillis),
        )
    }

    fun setExpenseCustomStart(startMillis: Long) {
        val start = LedgerFormatters.startOfDayMillis(startMillis)
        val currentEnd = filterInputs.value.expenseCustomEndMillis
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.CUSTOM,
            expenseCustomStartMillis = minOf(start, currentEnd),
            expenseCustomEndMillis = maxOf(start, currentEnd),
        )
    }

    fun setExpenseCustomEnd(endMillis: Long) {
        val end = LedgerFormatters.startOfDayMillis(endMillis)
        val currentStart = filterInputs.value.expenseCustomStartMillis
        filterInputs.value = filterInputs.value.copy(
            expenseRange = ExpenseRange.CUSTOM,
            expenseCustomStartMillis = minOf(currentStart, end),
            expenseCustomEndMillis = maxOf(currentStart, end),
        )
    }

    fun setExpenseCategoryFilter(category: ExpenseCategory?) {
        filterInputs.value = filterInputs.value.copy(expenseCategoryFilter = category)
    }

    fun setExpenseSearchQuery(query: String) {
        filterInputs.value = filterInputs.value.copy(expenseSearchQuery = query)
    }

    fun openAddExpenseSheet() {
        filterInputs.value = filterInputs.value.copy(isAddExpenseSheetVisible = true)
    }

    fun closeAddExpenseSheet() {
        filterInputs.value = filterInputs.value.copy(isAddExpenseSheetVisible = false)
    }

    fun openExpenseDetail(expenseId: Long) {
        filterInputs.value = filterInputs.value.copy(selectedExpenseId = expenseId)
    }

    fun closeExpenseDetail() {
        filterInputs.value = filterInputs.value.copy(selectedExpenseId = null)
    }

    fun addExpense(
        title: String,
        category: ExpenseCategory,
        amount: Double,
        note: String,
        dayStartMillis: Long,
    ) {
        viewModelScope.launch {
            val timestamp = LedgerFormatters.combineDayWithCurrentTime(dayStartMillis)
            repository.addExpense(
                title = title,
                category = category,
                amount = amount,
                note = note,
                createdAtMillis = timestamp,
            )
            closeAddExpenseSheet()
            _events.emit("تم تسجيل المصروف بنجاح")
        }
    }

    fun updateExpense(
        existing: ExpenseEntity,
        title: String,
        category: ExpenseCategory,
        amount: Double,
        note: String,
    ) {
        viewModelScope.launch {
            repository.updateExpense(
                existing = existing,
                title = title,
                category = category,
                amount = amount,
                note = note,
            )
            _events.emit("تم تحديث المصروف")
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            closeExpenseDetail()
            _events.emit("تم حذف المصروف")
        }
    }

    private fun buildUiState(
        allSales: List<SaleWithPayments>,
        recentCustomers: List<String>,
        allExpenses: List<ExpenseEntity>,
        recentExpenseTitles: List<String>,
        inputs: FilterInputs,
    ): SalesUiState {
        val now = System.currentTimeMillis()
        val todayStart = LedgerFormatters.startOfDayMillis(now)
        val isTodaySelected = inputs.dailySalesPeriod == DailySalesPeriod.TODAY &&
            LedgerFormatters.isSameDay(inputs.selectedDayMillis, todayStart)

        // 1. Daily / Quick-Period Sales computation
        val dayStart = LedgerFormatters.startOfDayMillis(inputs.selectedDayMillis)
        val (periodStart, periodEnd) = computeDailyPeriodBounds(
            period = inputs.dailySalesPeriod,
            selectedDayMillis = dayStart,
            nowMillis = now,
        )
        val salesForSelectedPeriod = allSales.filter {
            it.sale.createdAtMillis in periodStart..periodEnd
        }
        val dailySummary = DailySummary(
            totalSales = salesForSelectedPeriod.sumOf { it.sale.totalAmount },
            totalCollected = salesForSelectedPeriod.sumOf { it.paidAmount },
            totalRemaining = salesForSelectedPeriod.sumOf { it.remainingAmount },
            salesCount = salesForSelectedPeriod.size,
        )
        val dailyQuery = inputs.dailySearchQuery.trim()
        val filteredDailySales = salesForSelectedPeriod.filter { item ->
            val matchesFilter = when (inputs.dailyFilter) {
                DailyFilter.ALL -> true
                DailyFilter.PAID -> item.status == PaymentStatus.PAID
                DailyFilter.PARTIAL -> item.status == PaymentStatus.PARTIAL
                DailyFilter.UNPAID -> item.status == PaymentStatus.UNPAID
            }
            val matchesSearch = dailyQuery.isEmpty() ||
                LedgerFormatters.matchesAllNameParts(item.sale.customerName, dailyQuery) ||
                LedgerFormatters.matchesAllNameParts(item.sale.note, dailyQuery)
            matchesFilter && matchesSearch
        }

        // 2. Unpaid / Debts computation (across all dates)
        val allUnpaid = allSales.filter { it.remainingAmount > 0.0 }
        val totalUnpaidBalance = allUnpaid.sumOf { it.remainingAmount }
        val allCustomerDebts = groupDebtsByCustomer(allUnpaid)

        val unpaidQuery = inputs.unpaidSearchQuery.trim()
        val filteredUnpaidSales = allUnpaid
            .filter { item ->
                unpaidQuery.isEmpty() ||
                    LedgerFormatters.matchesAllNameParts(item.sale.customerName, unpaidQuery) ||
                    LedgerFormatters.matchesAllNameParts(item.sale.note, unpaidQuery)
            }
            .let { list ->
                when (inputs.unpaidSortOrder) {
                    UnpaidSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.remainingAmount }
                    UnpaidSortOrder.OLDEST_FIRST -> list.sortedBy { it.sale.createdAtMillis }
                    UnpaidSortOrder.NEWEST_FIRST -> list.sortedByDescending { it.sale.createdAtMillis }
                }
            }

        val filteredCustomerDebts = allCustomerDebts
            .filter { summary ->
                unpaidQuery.isEmpty() ||
                    LedgerFormatters.matchesAllNameParts(summary.customerName, unpaidQuery)
            }
            .let { list ->
                when (inputs.unpaidSortOrder) {
                    UnpaidSortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.totalRemaining }
                    UnpaidSortOrder.OLDEST_FIRST -> list.sortedBy { it.oldestSaleMillis }
                    UnpaidSortOrder.NEWEST_FIRST -> list.sortedByDescending { it.oldestSaleMillis }
                }
            }

        // 3. Customers Purchases computation (all sales, paid + unpaid)
        val allCustomerPurchases = groupAllPurchasesByCustomer(allSales)
        val customersQuery = inputs.customersSearchQuery.trim()
        val filteredCustomerPurchases = allCustomerPurchases
            .filter { summary ->
                customersQuery.isEmpty() ||
                    LedgerFormatters.matchesAllNameParts(summary.customerName, customersQuery)
            }
            .let { list ->
                when (inputs.customersSortOrder) {
                    CustomerSortOrder.MOST_PURCHASES -> list.sortedByDescending { it.totalPurchases }
                    CustomerSortOrder.MOST_ORDERS -> list.sortedByDescending { it.purchasesCount }
                    CustomerSortOrder.HIGHEST_DEBT -> list.sortedByDescending { it.totalRemaining }
                    CustomerSortOrder.RECENT_ACTIVITY -> list.sortedByDescending { it.lastPurchaseMillis }
                }
            }

        // 4. Expenses computation
        val expDayStart = LedgerFormatters.startOfDayMillis(inputs.expenseDayMillis)
        val expDayEnd = LedgerFormatters.endOfDayMillis(inputs.expenseDayMillis)
        val expMonthStart = LedgerFormatters.startOfMonthMillis(now)
        val expCustomStart = LedgerFormatters.startOfDayMillis(minOf(inputs.expenseCustomStartMillis, inputs.expenseCustomEndMillis))
        val expCustomEnd = LedgerFormatters.endOfDayMillis(maxOf(inputs.expenseCustomStartMillis, inputs.expenseCustomEndMillis))

        val rangeExpenses = when (inputs.expenseRange) {
            ExpenseRange.DAY -> allExpenses.filter { it.createdAtMillis in expDayStart..expDayEnd }
            ExpenseRange.THIS_MONTH -> allExpenses.filter { it.createdAtMillis >= expMonthStart }
            ExpenseRange.ALL_TIME -> allExpenses
            ExpenseRange.CUSTOM -> allExpenses.filter { it.createdAtMillis in expCustomStart..expCustomEnd }
        }
        val rangeSalesForExpenses = when (inputs.expenseRange) {
            ExpenseRange.DAY -> allSales.filter { it.sale.createdAtMillis in expDayStart..expDayEnd }
            ExpenseRange.THIS_MONTH -> allSales.filter { it.sale.createdAtMillis >= expMonthStart }
            ExpenseRange.ALL_TIME -> allSales
            ExpenseRange.CUSTOM -> allSales.filter { it.sale.createdAtMillis in expCustomStart..expCustomEnd }
        }
        val totalPeriodExpenses = rangeExpenses.sumOf { it.amount }
        val periodCollectedSales = rangeSalesForExpenses.sumOf { it.paidAmount }
        val expensesSummary = ExpensesSummary(
            totalExpenses = totalPeriodExpenses,
            expensesCount = rangeExpenses.size,
            periodCollectedSales = periodCollectedSales,
            netCash = periodCollectedSales - totalPeriodExpenses,
            categoryBreakdown = buildExpenseCategoryBreakdown(rangeExpenses),
        )

        val expQuery = inputs.expenseSearchQuery.trim()
        val filteredExpenses = rangeExpenses.filter { exp ->
            val matchesCat = inputs.expenseCategoryFilter == null ||
                exp.categoryEnum == inputs.expenseCategoryFilter
            val matchesSearch = expQuery.isEmpty() ||
                LedgerFormatters.matchesAllNameParts(exp.title, expQuery) ||
                LedgerFormatters.matchesAllNameParts(exp.note, expQuery) ||
                LedgerFormatters.matchesAllNameParts(exp.categoryEnum.labelAr, expQuery)
            matchesCat && matchesSearch
        }

        // 5. Analytics computation
        val analyticsSummary = buildAnalyticsSummary(
            allSales = allSales,
            allExpenses = allExpenses,
            allCustomerDebts = allCustomerDebts,
            range = inputs.analyticsRange,
            customStartMillis = inputs.analyticsCustomStartMillis,
            customEndMillis = inputs.analyticsCustomEndMillis,
        )

        val selectedSale = inputs.selectedSaleId?.let { id ->
            allSales.find { it.sale.id == id }
        }
        val selectedExpense = inputs.selectedExpenseId?.let { id ->
            allExpenses.find { it.id == id }
        }
        val selectedCustomerDebt = inputs.selectedCustomerName?.let { name ->
            allCustomerDebts.find { it.customerName.equals(name, ignoreCase = true) }
        }
        val selectedCustomerPurchase = inputs.selectedCustomerPurchaseName?.let { name ->
            allCustomerPurchases.find { it.customerName.equals(name, ignoreCase = true) }
        }

        return SalesUiState(
            isLoading = false,
            selectedTab = inputs.selectedTab,
            dailySalesPeriod = inputs.dailySalesPeriod,
            selectedDayMillis = dayStart,
            isTodaySelected = isTodaySelected,
            dailyFilter = inputs.dailyFilter,
            dailySearchQuery = inputs.dailySearchQuery,
            dailySummary = dailySummary,
            dailySales = filteredDailySales,
            allDaySalesCount = salesForSelectedPeriod.size,
            unpaidViewMode = inputs.unpaidViewMode,
            unpaidSortOrder = inputs.unpaidSortOrder,
            unpaidSearchQuery = inputs.unpaidSearchQuery,
            totalUnpaidBalance = totalUnpaidBalance,
            totalUnpaidInvoicesCount = allUnpaid.size,
            totalUnpaidCustomersCount = allCustomerDebts.size,
            unpaidSales = filteredUnpaidSales,
            unpaidByCustomer = filteredCustomerDebts,
            customersSortOrder = inputs.customersSortOrder,
            customersSearchQuery = inputs.customersSearchQuery,
            customersList = filteredCustomerPurchases,
            totalCustomersCount = allCustomerPurchases.size,
            allCustomersTotalPurchases = allCustomerPurchases.sumOf { it.totalPurchases },
            topBuyer = allCustomerPurchases.firstOrNull(),
            expenseRange = inputs.expenseRange,
            expenseDayMillis = expDayStart,
            isExpenseTodaySelected = LedgerFormatters.isSameDay(expDayStart, todayStart),
            expenseCustomStartMillis = inputs.expenseCustomStartMillis,
            expenseCustomEndMillis = inputs.expenseCustomEndMillis,
            expenseCategoryFilter = inputs.expenseCategoryFilter,
            expenseSearchQuery = inputs.expenseSearchQuery,
            expensesSummary = expensesSummary,
            expensesList = filteredExpenses,
            allRangeExpensesCount = rangeExpenses.size,
            recentExpenseTitles = recentExpenseTitles.distinct().take(8),
            selectedExpense = selectedExpense,
            isAddExpenseSheetVisible = inputs.isAddExpenseSheetVisible,
            analyticsRange = inputs.analyticsRange,
            analyticsCustomStartMillis = inputs.analyticsCustomStartMillis,
            analyticsCustomEndMillis = inputs.analyticsCustomEndMillis,
            analyticsSummary = analyticsSummary,
            recentCustomers = recentCustomers.distinct().take(8),
            allCustomerSummaries = allCustomerPurchases.sortedByDescending { it.lastPurchaseMillis },
            selectedSale = selectedSale,
            selectedCustomerDebt = selectedCustomerDebt,
            selectedCustomerPurchase = selectedCustomerPurchase,
            isAddSaleSheetVisible = inputs.isAddSaleSheetVisible,
            prefilledCustomerName = inputs.prefilledCustomerName,
        )
    }

    private fun groupAllPurchasesByCustomer(allSales: List<SaleWithPayments>): List<CustomerPurchaseSummary> {
        return allSales
            .groupBy { it.sale.customerName.trim().lowercase(Locale.getDefault()) }
            .values
            .map { items ->
                val sortedItems = items.sortedByDescending { it.sale.createdAtMillis }
                CustomerPurchaseSummary(
                    customerName = sortedItems.first().sale.customerName.trim(),
                    totalPurchases = items.sumOf { it.sale.totalAmount },
                    totalPaid = items.sumOf { it.paidAmount },
                    totalRemaining = items.sumOf { it.remainingAmount },
                    purchasesCount = items.size,
                    lastPurchaseMillis = sortedItems.first().sale.createdAtMillis,
                    sales = sortedItems,
                    rank = 0,
                )
            }
            .sortedByDescending { it.totalPurchases }
            .mapIndexed { index, summary ->
                summary.copy(rank = index + 1)
            }
    }

    private fun groupDebtsByCustomer(unpaidSales: List<SaleWithPayments>): List<CustomerDebtSummary> {
        return unpaidSales
            .groupBy { it.sale.customerName.trim() }
            .map { (name, items) ->
                CustomerDebtSummary(
                    customerName = name,
                    totalRemaining = items.sumOf { it.remainingAmount },
                    totalAmount = items.sumOf { it.sale.totalAmount },
                    totalPaid = items.sumOf { it.paidAmount },
                    openSalesCount = items.size,
                    oldestSaleMillis = items.minOf { it.sale.createdAtMillis },
                    sales = items.sortedBy { it.sale.createdAtMillis },
                )
            }
            .sortedByDescending { it.totalRemaining }
    }

    private fun buildAnalyticsSummary(
        allSales: List<SaleWithPayments>,
        allExpenses: List<ExpenseEntity>,
        allCustomerDebts: List<CustomerDebtSummary>,
        range: AnalyticsRange,
        customStartMillis: Long,
        customEndMillis: Long,
    ): AnalyticsSummary {
        val now = System.currentTimeMillis()
        val todayStart = LedgerFormatters.startOfDayMillis(now)

        val customStart = LedgerFormatters.startOfDayMillis(minOf(customStartMillis, customEndMillis))
        val customEnd = LedgerFormatters.endOfDayMillis(maxOf(customStartMillis, customEndMillis))

        val scopedSales = when (range) {
            AnalyticsRange.LAST_7_DAYS -> {
                val start7 = LedgerFormatters.addDays(todayStart, -6)
                allSales.filter { it.sale.createdAtMillis >= start7 }
            }
            AnalyticsRange.THIS_MONTH -> {
                val monthStart = LedgerFormatters.startOfMonthMillis(now)
                allSales.filter { it.sale.createdAtMillis >= monthStart }
            }
            AnalyticsRange.ALL_TIME -> allSales
            AnalyticsRange.CUSTOM -> {
                allSales.filter { it.sale.createdAtMillis in customStart..customEnd }
            }
        }

        val scopedExpenses = when (range) {
            AnalyticsRange.LAST_7_DAYS -> {
                val start7 = LedgerFormatters.addDays(todayStart, -6)
                allExpenses.filter { it.createdAtMillis >= start7 }
            }
            AnalyticsRange.THIS_MONTH -> {
                val monthStart = LedgerFormatters.startOfMonthMillis(now)
                allExpenses.filter { it.createdAtMillis >= monthStart }
            }
            AnalyticsRange.ALL_TIME -> allExpenses
            AnalyticsRange.CUSTOM -> {
                allExpenses.filter { it.createdAtMillis in customStart..customEnd }
            }
        }

        val buckets = when (range) {
            AnalyticsRange.LAST_7_DAYS -> buildLast7DaysBuckets(scopedSales, todayStart)
            AnalyticsRange.THIS_MONTH -> buildThisMonthBuckets(scopedSales, now)
            AnalyticsRange.ALL_TIME -> buildAllTimeMonthlyBuckets(scopedSales)
            AnalyticsRange.CUSTOM -> buildCustomRangeBuckets(scopedSales, customStart, customEnd)
        }

        return AnalyticsSummary(
            totalSales = scopedSales.sumOf { it.sale.totalAmount },
            totalCollected = scopedSales.sumOf { it.paidAmount },
            totalRemaining = scopedSales.sumOf { it.remainingAmount },
            totalExpenses = scopedExpenses.sumOf { it.amount },
            expensesCount = scopedExpenses.size,
            salesCount = scopedSales.size,
            paidCount = scopedSales.count { it.status == PaymentStatus.PAID },
            partialCount = scopedSales.count { it.status == PaymentStatus.PARTIAL },
            unpaidCount = scopedSales.count { it.status == PaymentStatus.UNPAID },
            chartBuckets = buckets,
            topIndebtedCustomers = allCustomerDebts.take(5),
            expenseCategoryBreakdown = buildExpenseCategoryBreakdown(scopedExpenses),
        )
    }

    private fun buildLast7DaysBuckets(
        sales: List<SaleWithPayments>,
        todayStart: Long,
    ): List<ChartBucket> {
        val dayFmt = SimpleDateFormat("EEE", Locale("ar"))
        return (6 downTo 0).map { offset ->
            val dayStart = LedgerFormatters.addDays(todayStart, -offset)
            val dayEnd = LedgerFormatters.endOfDayMillis(dayStart)
            val daySales = sales.filter { it.sale.createdAtMillis in dayStart..dayEnd }
            val label = if (offset == 0) "اليوم" else dayFmt.format(Date(dayStart))
            ChartBucket(
                label = label,
                collected = daySales.sumOf { it.paidAmount },
                remaining = daySales.sumOf { it.remainingAmount },
            )
        }
    }

    private fun buildThisMonthBuckets(
        sales: List<SaleWithPayments>,
        nowMillis: Long,
    ): List<ChartBucket> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weekRanges = listOf(
            1..7 to "أ1",
            8..14 to "أ2",
            15..21 to "أ3",
            22..maxDay to "أ4",
        )
        return weekRanges.map { (dayRange, label) ->
            val matching = sales.filter { item ->
                val itemCal = Calendar.getInstance().apply { timeInMillis = item.sale.createdAtMillis }
                itemCal.get(Calendar.DAY_OF_MONTH) in dayRange
            }
            ChartBucket(
                label = label,
                collected = matching.sumOf { it.paidAmount },
                remaining = matching.sumOf { it.remainingAmount },
            )
        }
    }

    private fun buildAllTimeMonthlyBuckets(sales: List<SaleWithPayments>): List<ChartBucket> {
        if (sales.isEmpty()) return emptyList()
        val monthFmt = SimpleDateFormat("MMM", Locale("ar"))
        val grouped = sales.groupBy { item ->
            val cal = Calendar.getInstance().apply { timeInMillis = item.sale.createdAtMillis }
            cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        }
        return grouped.entries
            .sortedBy { it.key }
            .takeLast(6)
            .map { (_, items) ->
                val sampleMillis = items.first().sale.createdAtMillis
                ChartBucket(
                    label = monthFmt.format(Date(sampleMillis)),
                    collected = items.sumOf { it.paidAmount },
                    remaining = items.sumOf { it.remainingAmount },
                )
            }
    }

    private fun buildCustomRangeBuckets(
        sales: List<SaleWithPayments>,
        startDayMillis: Long,
        endMillis: Long,
    ): List<ChartBucket> {
        val endDayStart = LedgerFormatters.startOfDayMillis(endMillis)
        val totalDays = (TimeUnit.MILLISECONDS.toDays(endDayStart - startDayMillis).toInt() + 1).coerceAtLeast(1)
        val shortDateFmt = SimpleDateFormat("d/M", Locale.US)

        return when {
            totalDays <= 7 -> {
                (0 until totalDays).map { offset ->
                    val dStart = LedgerFormatters.addDays(startDayMillis, offset)
                    val dEnd = LedgerFormatters.endOfDayMillis(dStart)
                    val daySales = sales.filter { it.sale.createdAtMillis in dStart..dEnd }
                    ChartBucket(
                        label = shortDateFmt.format(Date(dStart)),
                        collected = daySales.sumOf { it.paidAmount },
                        remaining = daySales.sumOf { it.remainingAmount },
                    )
                }
            }
            totalDays <= 45 -> {
                val bucketCount = 6
                val chunkDays = ((totalDays + bucketCount - 1) / bucketCount).coerceAtLeast(1)
                val result = mutableListOf<ChartBucket>()
                var cursor = 0
                while (cursor < totalDays) {
                    val bStart = LedgerFormatters.addDays(startDayMillis, cursor)
                    val endOffset = (cursor + chunkDays - 1).coerceAtMost(totalDays - 1)
                    val bEnd = LedgerFormatters.endOfDayMillis(LedgerFormatters.addDays(startDayMillis, endOffset))
                    val chunkSales = sales.filter { it.sale.createdAtMillis in bStart..bEnd }
                    result.add(
                        ChartBucket(
                            label = shortDateFmt.format(Date(bStart)),
                            collected = chunkSales.sumOf { it.paidAmount },
                            remaining = chunkSales.sumOf { it.remainingAmount },
                        )
                    )
                    cursor += chunkDays
                }
                result
            }
            else -> buildAllTimeMonthlyBuckets(sales)
        }
    }
}

internal fun buildExpenseCategoryBreakdown(
    expenses: List<ExpenseEntity>,
): List<ExpenseCategoryBreakdown> {
    if (expenses.isEmpty()) return emptyList()
    val total = expenses.sumOf { it.amount }
    return expenses
        .groupBy { it.categoryEnum }
        .map { (cat, items) ->
            val sum = items.sumOf { it.amount }
            ExpenseCategoryBreakdown(
                category = cat,
                amount = sum,
                count = items.size,
                share = if (total <= 0.0) 0f else (sum / total).toFloat().coerceIn(0f, 1f),
            )
        }
        .sortedByDescending { it.amount }
}

internal fun computeDailyPeriodBounds(
    period: DailySalesPeriod,
    selectedDayMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): Pair<Long, Long> {
    val todayStart = LedgerFormatters.startOfDayMillis(nowMillis)
    val todayEnd = LedgerFormatters.endOfDayMillis(nowMillis)
    return when (period) {
        DailySalesPeriod.TODAY -> todayStart to todayEnd
        DailySalesPeriod.YESTERDAY -> {
            val yesterdayStart = LedgerFormatters.addDays(todayStart, -1)
            yesterdayStart to LedgerFormatters.endOfDayMillis(yesterdayStart)
        }
        DailySalesPeriod.WEEK -> LedgerFormatters.addDays(todayStart, -6) to todayEnd
        DailySalesPeriod.MONTH -> LedgerFormatters.startOfMonthMillis(nowMillis) to todayEnd
        DailySalesPeriod.CUSTOM_DAY -> {
            LedgerFormatters.startOfDayMillis(selectedDayMillis) to LedgerFormatters.endOfDayMillis(selectedDayMillis)
        }
    }
}
