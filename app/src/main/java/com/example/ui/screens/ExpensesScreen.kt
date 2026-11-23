package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.ui.ExpenseCategoryBreakdown
import com.example.ui.ExpenseRange
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.iconVector
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

private enum class ExpensePickerTarget {
    SINGLE_DAY,
    CUSTOM_START,
    CUSTOM_END,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    uiState: SalesUiState,
    onNavigateBackToDaily: () -> Unit,
    onRangeChange: (ExpenseRange) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onCustomStartChange: (Long) -> Unit,
    onCustomEndChange: (Long) -> Unit,
    onCategoryFilterChange: (ExpenseCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExpenseClick: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onNavigateBackToDaily() }

    var pickerTarget by remember { mutableStateOf<ExpensePickerTarget?>(null) }

    pickerTarget?.let { target ->
        val initialMillis = when (target) {
            ExpensePickerTarget.SINGLE_DAY -> uiState.expenseDayMillis
            ExpensePickerTarget.CUSTOM_START -> uiState.expenseCustomStartMillis
            ExpensePickerTarget.CUSTOM_END -> uiState.expenseCustomEndMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { chosen ->
                            when (target) {
                                ExpensePickerTarget.SINGLE_DAY -> onSelectDay(chosen)
                                ExpensePickerTarget.CUSTOM_START -> onCustomStartChange(chosen)
                                ExpensePickerTarget.CUSTOM_END -> onCustomEndChange(chosen)
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
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Period Range Switcher
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val ranges = listOf(
                    ExpenseRange.DAY to stringResource(R.string.today),
                    ExpenseRange.THIS_MONTH to stringResource(R.string.range_month),
                    ExpenseRange.ALL_TIME to stringResource(R.string.range_all),
                    ExpenseRange.CUSTOM to stringResource(R.string.range_custom),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ranges.forEachIndexed { index, (range, label) ->
                        SegmentedButton(
                            selected = uiState.expenseRange == range,
                            onClick = { onRangeChange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                            modifier = Modifier.testTag("expense_range_${range.name.lowercase()}"),
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
                    visible = uiState.expenseRange == ExpenseRange.DAY,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onPreviousDay,
                                    modifier = Modifier.testTag("expense_prev_day"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.previous_day),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { pickerTarget = ExpensePickerTarget.SINGLE_DAY }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                        .testTag("expense_pick_day"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = stringResource(R.string.pick_date),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = LedgerFormatters.formatDayLabel(uiState.expenseDayMillis),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                IconButton(
                                    onClick = onNextDay,
                                    modifier = Modifier.testTag("expense_next_day"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.next_day),
                                    )
                                }
                            }
                        }

                        if (!uiState.isExpenseTodaySelected) {
                            AssistChip(
                                onClick = onGoToToday,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Today,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text(stringResource(R.string.today)) },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.expenseRange == ExpenseRange.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExpenseDateFieldButton(
                            label = stringResource(R.string.custom_start_date),
                            dateText = LedgerFormatters.formatShortDate(uiState.expenseCustomStartMillis),
                            onClick = { pickerTarget = ExpensePickerTarget.CUSTOM_START },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("expense_custom_start_button"),
                        )
                        ExpenseDateFieldButton(
                            label = stringResource(R.string.custom_end_date),
                            dateText = LedgerFormatters.formatShortDate(uiState.expenseCustomEndMillis),
                            onClick = { pickerTarget = ExpensePickerTarget.CUSTOM_END },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("expense_custom_end_button"),
                        )
                    }
                }
            }
        }

        // Expense Hero Summary Card
        item {
            ExpensesHeroCard(
                totalExpenses = uiState.expensesSummary.totalExpenses,
                expensesCount = uiState.expensesSummary.expensesCount,
                collectedSales = uiState.expensesSummary.periodCollectedSales,
                netCash = uiState.expensesSummary.netCash,
            )
        }

        // Category Breakdown Card (if there are expenses in the range)
        if (uiState.expensesSummary.categoryBreakdown.isNotEmpty()) {
            item {
                ExpenseCategoryBreakdownCard(
                    breakdown = uiState.expensesSummary.categoryBreakdown,
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.expenseSearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.expense_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.expenseSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_search_input"),
            )
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.expenseCategoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier.testTag("expense_filter_all"),
                )
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.expenseCategoryFilter == cat,
                        onClick = {
                            onCategoryFilterChange(
                                if (uiState.expenseCategoryFilter == cat) null else cat
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = cat.iconVector(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text(cat.labelAr) },
                        modifier = Modifier.testTag("expense_filter_${cat.name.lowercase()}"),
                    )
                }
            }
        }

        // Expense List or Empty State
        if (uiState.expensesList.isEmpty()) {
            item {
                val isFilteredEmpty = uiState.allRangeExpensesCount > 0
                EmptyLedgerState(
                    title = stringResource(
                        if (isFilteredEmpty) R.string.empty_filter_title else R.string.empty_expenses_title
                    ),
                    subtitle = stringResource(
                        if (isFilteredEmpty) R.string.empty_filter_subtitle else R.string.empty_expenses_subtitle
                    ),
                    showIllustration = false,
                )
            }
        } else {
            items(
                items = uiState.expensesList,
                key = { it.id },
            ) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    onClick = { onExpenseClick(expense) },
                )
            }
        }
    }
}

@Composable
private fun ExpensesHeroCard(
    totalExpenses: Double,
    expensesCount: Int,
    collectedSales: Double,
    netCash: Double,
) {
    val ledgerColors = LocalLedgerColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ledgerColors.expenseHeroStart, ledgerColors.expenseHeroEnd),
                ),
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.total_expenses_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        text = LedgerFormatters.formatShekel(totalExpenses),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "$expensesCount مصروف",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.14f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "محصّل المبيعات",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text(
                            text = LedgerFormatters.formatShekel(collectedSales),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.14f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.net_cash_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text(
                            text = LedgerFormatters.formatShekel(netCash),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (netCash >= 0.0) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoryBreakdownCard(
    breakdown: List<ExpenseCategoryBreakdown>,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.expenses_by_category),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            breakdown.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = item.category.iconVector(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ledgerColors.expense,
                            )
                            Text(
                                text = "${item.category.labelAr} (${item.count})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "${LedgerFormatters.formatShekel(item.amount)} • ${LedgerFormatters.formatPercent(item.share)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = ledgerColors.expense,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { item.share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ledgerColors.expense,
                        trackColor = ledgerColors.expenseContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseDateFieldButton(
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
