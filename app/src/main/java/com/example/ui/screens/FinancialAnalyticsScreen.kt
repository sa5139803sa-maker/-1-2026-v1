package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AnalyticsRange
import com.example.ui.CustomerDebtSummary
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.FinancialBarChart
import com.example.ui.components.PaymentStatusDonutChart
import com.example.ui.screens.ExpenseCategoryBreakdownCard
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

private enum class CustomPickerTarget {
    START,
    END,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAnalyticsScreen(
    uiState: SalesUiState,
    onNavigateBackToDaily: () -> Unit,
    onRangeChange: (AnalyticsRange) -> Unit,
    onCustomStartChange: (Long) -> Unit,
    onCustomEndChange: (Long) -> Unit,
    onCustomerClick: (CustomerDebtSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onNavigateBackToDaily() }

    val ledgerColors = LocalLedgerColors.current
    val summary = uiState.analyticsSummary
    var pickerTarget by remember { mutableStateOf<CustomPickerTarget?>(null) }

    pickerTarget?.let { target ->
        val initialMillis = if (target == CustomPickerTarget.START) {
            uiState.analyticsCustomStartMillis
        } else {
            uiState.analyticsCustomEndMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { chosen ->
                            if (target == CustomPickerTarget.START) {
                                onCustomStartChange(chosen)
                            } else {
                                onCustomEndChange(chosen)
                            }
                        }
                        pickerTarget = null
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = stringResource(
                            if (target == CustomPickerTarget.START) {
                                R.string.select_start_date
                            } else {
                                R.string.select_end_date
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Range Selector (4 options including Custom)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val ranges = listOf(
                    AnalyticsRange.LAST_7_DAYS to stringResource(R.string.range_7_days),
                    AnalyticsRange.THIS_MONTH to stringResource(R.string.range_month),
                    AnalyticsRange.ALL_TIME to stringResource(R.string.range_all),
                    AnalyticsRange.CUSTOM to stringResource(R.string.range_custom),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ranges.forEachIndexed { index, (range, label) ->
                        SegmentedButton(
                            selected = uiState.analyticsRange == range,
                            onClick = { onRangeChange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                            modifier = Modifier.testTag("analytics_range_${range.name.lowercase()}"),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.analyticsRange == AnalyticsRange.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CustomDateFieldButton(
                            label = stringResource(R.string.custom_start_date),
                            dateText = LedgerFormatters.formatShortDate(uiState.analyticsCustomStartMillis),
                            onClick = { pickerTarget = CustomPickerTarget.START },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_start_date_button"),
                        )
                        CustomDateFieldButton(
                            label = stringResource(R.string.custom_end_date),
                            dateText = LedgerFormatters.formatShortDate(uiState.analyticsCustomEndMillis),
                            onClick = { pickerTarget = CustomPickerTarget.END },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_end_date_button"),
                        )
                    }
                }
            }
        }

        if (summary.salesCount == 0 && summary.expensesCount == 0) {
            item {
                EmptyLedgerState(
                    title = if (uiState.analyticsRange == AnalyticsRange.CUSTOM) {
                        "لا توجد بيانات في هذه الفترة المحددة"
                    } else {
                        stringResource(R.string.empty_analytics_title)
                    },
                    subtitle = if (uiState.analyticsRange == AnalyticsRange.CUSTOM) {
                        "جرّب تعديل تاريخ البداية أو النهاية لعرض الإحصاءات"
                    } else {
                        stringResource(R.string.empty_analytics_subtitle)
                    },
                    showIllustration = uiState.analyticsRange != AnalyticsRange.CUSTOM,
                )
            }
        } else {
            // 2x2 KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KpiCard(
                            title = stringResource(R.string.total_sales),
                            value = LedgerFormatters.formatShekel(summary.totalSales),
                            subtitle = "${summary.salesCount} عملية",
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            title = stringResource(R.string.collection_rate),
                            value = LedgerFormatters.formatPercent(summary.collectionRate),
                            subtitle = "${summary.paidCount} مسددة بالكامل",
                            icon = Icons.Rounded.Percent,
                            accentColor = ledgerColors.paid,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KpiCard(
                            title = stringResource(R.string.collected_amount),
                            value = LedgerFormatters.formatShekel(summary.totalCollected),
                            subtitle = "مبالغ مستلمة",
                            icon = Icons.Rounded.Paid,
                            accentColor = ledgerColors.paid,
                            modifier = Modifier.weight(1f),
                        )
                        KpiCard(
                            title = stringResource(R.string.remaining_amount),
                            value = LedgerFormatters.formatShekel(summary.totalRemaining),
                            subtitle = "${summary.partialCount + summary.unpaidCount} غير مكتملة",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            accentColor = ledgerColors.partial,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KpiCard(
                            title = stringResource(R.string.total_expenses_label),
                            value = LedgerFormatters.formatShekel(summary.totalExpenses),
                            subtitle = "${summary.expensesCount} مصروف",
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            accentColor = ledgerColors.expense,
                            modifier = Modifier.weight(1f),
                        )
                        val netPositive = summary.netCollectedProfit >= 0.0
                        KpiCard(
                            title = stringResource(R.string.net_cash_label),
                            value = LedgerFormatters.formatShekel(summary.netCollectedProfit),
                            subtitle = "صافي الدفتر: ${LedgerFormatters.formatShekel(summary.netSalesProfit)}",
                            icon = Icons.Rounded.Savings,
                            accentColor = if (netPositive) ledgerColors.paid else ledgerColors.expense,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Stacked Bar Chart Card
            if (summary.salesCount > 0 && summary.chartBuckets.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.chart_collected_vs_remaining),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            FinancialBarChart(buckets = summary.chartBuckets)
                        }
                    }
                }
            }

            // Donut Chart Card
            if (summary.salesCount > 0) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.chart_status_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            PaymentStatusDonutChart(
                                paidCount = summary.paidCount,
                                partialCount = summary.partialCount,
                                unpaidCount = summary.unpaidCount,
                                collectionRate = summary.collectionRate,
                            )
                        }
                    }
                }
            }

            // Expense Category Breakdown Card
            if (summary.expenseCategoryBreakdown.isNotEmpty()) {
                item {
                    ExpenseCategoryBreakdownCard(
                        breakdown = summary.expenseCategoryBreakdown,
                    )
                }
            }

            // Top Indebted Customers Card
            if (summary.topIndebtedCustomers.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.top_unpaid_customers),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            summary.topIndebtedCustomers.forEachIndexed { index, customer ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { onCustomerClick(customer) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = "${index + 1}. ${customer.customerName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = stringResource(R.string.open_invoices_count, customer.openSalesCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = LedgerFormatters.formatShekel(customer.totalRemaining),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = ledgerColors.unpaid,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomDateFieldButton(
    label: String,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.14f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.padding(6.dp),
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
