package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.AppThemeMode
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.components.AddSaleBottomSheet
import com.example.ui.components.BackupAndExportBottomSheet
import com.example.ui.components.CustomerDebtDetailSheet
import com.example.ui.components.CustomerPurchasesDetailSheet
import com.example.ui.components.ExpenseDetailBottomSheet
import com.example.ui.components.ImportBackupConfirmationDialog
import com.example.ui.components.SaleDetailBottomSheet
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DailySalesScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.FinancialAnalyticsScreen
import com.example.ui.screens.UnpaidDebtsScreen
import com.example.util.LedgerFormatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TabNavItem(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesLedgerApp(
    viewModel: SalesViewModel = viewModel(),
    isDarkTheme: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let { viewModel.exportJsonBackup(it) }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.prepareJsonImport(it) }
    }

    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ) { uri ->
        uri?.let { viewModel.exportUnpaidExcel(it) }
    }

    fun launchJsonExport() {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        exportJsonLauncher.launch("sales_ledger_backup_$dateStamp.json")
    }

    fun launchExcelExport() {
        val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        exportExcelLauncher.launch("unpaid_debts_$dateStamp.xlsx")
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navItems = remember {
        listOf(
            TabNavItem(
                tab = AppTab.DAILY,
                labelRes = R.string.tab_daily,
                selectedIcon = Icons.Rounded.PointOfSale,
                unselectedIcon = Icons.Outlined.PointOfSale,
                testTag = "nav_tab_daily",
            ),
            TabNavItem(
                tab = AppTab.UNPAID,
                labelRes = R.string.tab_unpaid,
                selectedIcon = Icons.Rounded.PendingActions,
                unselectedIcon = Icons.Outlined.PendingActions,
                testTag = "nav_tab_unpaid",
            ),
            TabNavItem(
                tab = AppTab.CUSTOMERS,
                labelRes = R.string.tab_customers,
                selectedIcon = Icons.Rounded.Group,
                unselectedIcon = Icons.Outlined.Group,
                testTag = "nav_tab_customers",
            ),
            TabNavItem(
                tab = AppTab.EXPENSES,
                labelRes = R.string.tab_expenses,
                selectedIcon = Icons.AutoMirrored.Rounded.ReceiptLong,
                unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                testTag = "nav_tab_expenses",
            ),
            TabNavItem(
                tab = AppTab.ANALYTICS,
                labelRes = R.string.tab_analytics,
                selectedIcon = Icons.Rounded.BarChart,
                unselectedIcon = Icons.Outlined.BarChart,
                testTag = "nav_tab_analytics",
            ),
        )
    }

    val screenTitle = when (uiState.selectedTab) {
        AppTab.DAILY -> stringResource(R.string.daily_title)
        AppTab.UNPAID -> stringResource(R.string.unpaid_title)
        AppTab.CUSTOMERS -> stringResource(R.string.customers_title)
        AppTab.EXPENSES -> stringResource(R.string.expenses_title)
        AppTab.ANALYTICS -> stringResource(R.string.analytics_title)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::openBackupSheet,
                            modifier = Modifier.testTag("backup_export_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Backup,
                                contentDescription = stringResource(R.string.backup_export_title),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        ThemeSwitchPill(
                            isDarkTheme = isDarkTheme,
                            onSelectLight = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            onSelectDark = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.selectedTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    TabIconWithBadge(
                                        item = item,
                                        selected = selected,
                                        unpaidCount = uiState.totalUnpaidInvoicesCount,
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (uiState.selectedTab) {
                    AppTab.EXPENSES -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAddExpenseSheet() },
                            modifier = Modifier.testTag("add_expense_fab"),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.add_expense),
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.add_expense),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    AppTab.ANALYTICS -> Unit
                    else -> {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.openAddSaleSheet() },
                            modifier = Modifier.testTag("add_sale_fab"),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = stringResource(R.string.add_sale),
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.add_sale),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        navItems.forEach { item ->
                            val selected = uiState.selectedTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(item.tab) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    TabIconWithBadge(
                                        item = item,
                                        selected = selected,
                                        unpaidCount = uiState.totalUnpaidInvoicesCount,
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val constrainedModifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()

                    when (uiState.selectedTab) {
                        AppTab.DAILY -> DailySalesScreen(
                            uiState = uiState,
                            onPreviousDay = viewModel::previousDay,
                            onNextDay = viewModel::nextDay,
                            onGoToToday = viewModel::goToToday,
                            onSelectDay = viewModel::selectDay,
                            onSelectPeriod = viewModel::setDailySalesPeriod,
                            onSelectFilter = viewModel::setDailyFilter,
                            onSearchQueryChange = viewModel::setDailySearchQuery,
                            onSaleClick = { viewModel.openSaleDetail(it.sale.id) },
                            onQuickSettleClick = { viewModel.openSaleDetail(it.sale.id) },
                            modifier = constrainedModifier,
                        )
                        AppTab.UNPAID -> UnpaidDebtsScreen(
                            uiState = uiState,
                            onNavigateBackToDaily = { viewModel.selectTab(AppTab.DAILY) },
                            onViewModeChange = viewModel::setUnpaidViewMode,
                            onSortOrderChange = viewModel::setUnpaidSortOrder,
                            onSearchQueryChange = viewModel::setUnpaidSearchQuery,
                            onSaleClick = { viewModel.openSaleDetail(it.sale.id) },
                            onQuickSettleSale = { viewModel.settleSaleFull(it) },
                            onCustomerClick = { viewModel.openCustomerDebts(it.customerName) },
                            onExportExcelClick = { launchExcelExport() },
                            modifier = constrainedModifier,
                        )
                        AppTab.CUSTOMERS -> CustomersScreen(
                            uiState = uiState,
                            onNavigateBackToDaily = { viewModel.selectTab(AppTab.DAILY) },
                            onSortOrderChange = viewModel::setCustomersSortOrder,
                            onSearchQueryChange = viewModel::setCustomersSearchQuery,
                            onCustomerClick = { viewModel.openCustomerPurchases(it.customerName) },
                            modifier = constrainedModifier,
                        )
                        AppTab.EXPENSES -> ExpensesScreen(
                            uiState = uiState,
                            onNavigateBackToDaily = { viewModel.selectTab(AppTab.DAILY) },
                            onRangeChange = viewModel::setExpenseRange,
                            onPreviousDay = viewModel::previousExpenseDay,
                            onNextDay = viewModel::nextExpenseDay,
                            onGoToToday = viewModel::goToExpenseToday,
                            onSelectDay = viewModel::selectExpenseDay,
                            onCustomStartChange = viewModel::setExpenseCustomStart,
                            onCustomEndChange = viewModel::setExpenseCustomEnd,
                            onCategoryFilterChange = viewModel::setExpenseCategoryFilter,
                            onSearchQueryChange = viewModel::setExpenseSearchQuery,
                            onExpenseClick = { viewModel.openExpenseDetail(it.id) },
                            modifier = constrainedModifier,
                        )
                        AppTab.ANALYTICS -> FinancialAnalyticsScreen(
                            uiState = uiState,
                            onNavigateBackToDaily = { viewModel.selectTab(AppTab.DAILY) },
                            onRangeChange = viewModel::setAnalyticsRange,
                            onCustomStartChange = viewModel::setAnalyticsCustomStart,
                            onCustomEndChange = viewModel::setAnalyticsCustomEnd,
                            onCustomerClick = { viewModel.openCustomerDebts(it.customerName) },
                            modifier = constrainedModifier,
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddSaleSheetVisible) {
        val addSaleDayMillis = when (uiState.dailySalesPeriod) {
            DailySalesPeriod.YESTERDAY, DailySalesPeriod.CUSTOM_DAY -> uiState.selectedDayMillis
            else -> LedgerFormatters.startOfDayMillis(System.currentTimeMillis())
        }
        AddSaleBottomSheet(
            knownCustomers = uiState.allCustomerSummaries,
            selectedDayMillis = addSaleDayMillis,
            initialCustomerName = uiState.prefilledCustomerName,
            onDismiss = viewModel::closeAddSaleSheet,
            onSaveSale = { customerName, totalAmount, initialPayment, note ->
                viewModel.addSale(
                    customerName = customerName,
                    totalAmount = totalAmount,
                    initialPaymentAmount = initialPayment,
                    note = note,
                    dayStartMillis = addSaleDayMillis,
                )
            },
        )
    }

    uiState.selectedCustomerDebt?.let { customerDebt ->
        CustomerDebtDetailSheet(
            customerDebt = customerDebt,
            onDismiss = viewModel::closeCustomerDebts,
            onSelectSale = { saleItem ->
                viewModel.closeCustomerDebts()
                viewModel.openSaleDetail(saleItem.sale.id)
            },
            onRecordAccountPayment = { amount ->
                viewModel.recordCustomerAccountPayment(
                    customerName = customerDebt.customerName,
                    sales = customerDebt.sales,
                    amount = amount,
                )
            },
            onSettleAllCustomerDebts = {
                viewModel.settleCustomerAllDebts(customerDebt)
            },
        )
    }

    uiState.selectedCustomerPurchase?.let { customerSummary ->
        CustomerPurchasesDetailSheet(
            customerSummary = customerSummary,
            onDismiss = viewModel::closeCustomerPurchases,
            onSelectSale = { saleItem ->
                viewModel.closeCustomerPurchases()
                viewModel.openSaleDetail(saleItem.sale.id)
            },
            onAddSaleForCustomer = {
                val name = customerSummary.customerName
                viewModel.closeCustomerPurchases()
                viewModel.openAddSaleSheet(prefilledCustomer = name)
            },
            onRecordAccountPayment = { amount ->
                viewModel.recordCustomerAccountPayment(
                    customerName = customerSummary.customerName,
                    sales = customerSummary.sales,
                    amount = amount,
                )
            },
            onSettleAllDebts = {
                viewModel.settleCustomerPurchaseDebts(customerSummary)
            },
        )
    }

    uiState.selectedSale?.let { saleItem ->
        SaleDetailBottomSheet(
            saleWithPayments = saleItem,
            onDismiss = viewModel::closeSaleDetail,
            onSettleFull = { viewModel.settleSaleFull(saleItem) },
            onRecordPayment = { amount -> viewModel.recordPayment(saleItem, amount) },
            onDeletePayment = { paymentId -> viewModel.deletePayment(paymentId) },
            onUpdateSale = { name, total, note ->
                viewModel.updateSale(saleItem, name, total, note)
            },
            onDeleteSale = { viewModel.deleteSale(saleItem.sale.id) },
        )
    }

    if (uiState.isAddExpenseSheetVisible) {
        AddExpenseBottomSheet(
            recentTitles = uiState.recentExpenseTitles,
            initialDayMillis = uiState.expenseDayMillis,
            onDismiss = viewModel::closeAddExpenseSheet,
            onSaveExpense = { title, category, amount, note, dayStartMillis ->
                viewModel.addExpense(
                    title = title,
                    category = category,
                    amount = amount,
                    note = note,
                    dayStartMillis = dayStartMillis,
                )
            },
        )
    }

    uiState.selectedExpense?.let { expenseItem ->
        ExpenseDetailBottomSheet(
            expense = expenseItem,
            onDismiss = viewModel::closeExpenseDetail,
            onUpdateExpense = { title, category, amount, note ->
                viewModel.updateExpense(
                    existing = expenseItem,
                    title = title,
                    category = category,
                    amount = amount,
                    note = note,
                )
            },
            onDeleteExpense = { viewModel.deleteExpense(expenseItem.id) },
        )
    }

    if (uiState.isBackupSheetVisible) {
        BackupAndExportBottomSheet(
            totalSalesCount = uiState.totalAllSalesCount,
            totalExpensesCount = uiState.totalAllExpensesCount,
            unpaidInvoicesCount = uiState.totalUnpaidInvoicesCount,
            unpaidTotalAmount = uiState.totalUnpaidBalance,
            onDismiss = viewModel::closeBackupSheet,
            onExportJsonClick = { launchJsonExport() },
            onImportJsonClick = {
                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onExportUnpaidExcelClick = { launchExcelExport() },
        )
    }

    uiState.pendingImportBackup?.let { backupData ->
        ImportBackupConfirmationDialog(
            backupData = backupData,
            onDismiss = viewModel::dismissImportConfirmation,
            onConfirmMerge = { viewModel.confirmJsonImport(replaceExisting = false) },
            onConfirmReplace = { viewModel.confirmJsonImport(replaceExisting = true) },
        )
    }
}

@Composable
private fun ThemeSwitchPill(
    isDarkTheme: Boolean,
    onSelectLight: () -> Unit,
    onSelectDark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSelectLight)
                    .testTag("theme_light_button"),
                shape = RoundedCornerShape(50),
                color = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LightMode,
                        contentDescription = stringResource(R.string.theme_light),
                        modifier = Modifier.size(16.dp),
                        tint = if (!isDarkTheme) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = stringResource(R.string.theme_light).removePrefix("الوضع "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!isDarkTheme) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSelectDark)
                    .testTag("theme_dark_button"),
                shape = RoundedCornerShape(50),
                color = if (isDarkTheme) MaterialTheme.colorScheme.primary else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = stringResource(R.string.theme_dark),
                        modifier = Modifier.size(16.dp),
                        tint = if (isDarkTheme) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(
                        text = stringResource(R.string.theme_dark).removePrefix("الوضع "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkTheme) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    item: TabNavItem,
    selected: Boolean,
    unpaidCount: Int,
) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    val label = stringResource(item.labelRes)
    if (item.tab == AppTab.UNPAID && unpaidCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (unpaidCount > 99) "99+" else unpaidCount.toString())
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}
