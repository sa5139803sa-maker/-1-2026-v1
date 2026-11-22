package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.example.data.SaleWithPayments
import com.example.ui.DailyFilter
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.SaleItemCard
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySalesScreen(
    uiState: SalesUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    onSelectDay: (Long) -> Unit,
    onSelectFilter: (DailyFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaleClick: (SaleWithPayments) -> Unit,
    onQuickSettleClick: (SaleWithPayments) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDayMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onSelectDay(it) }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
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
        // Date Navigation Bar
        item {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onPreviousDay,
                            modifier = Modifier.testTag("previous_day_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.previous_day),
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("pick_date_button"),
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
                                text = LedgerFormatters.formatDayLabel(uiState.selectedDayMillis),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        IconButton(
                            onClick = onNextDay,
                            modifier = Modifier.testTag("next_day_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.next_day),
                            )
                        }
                    }
                }

                if (!uiState.isTodaySelected) {
                    AssistChip(
                        onClick = onGoToToday,
                        modifier = Modifier.testTag("back_to_today_chip"),
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

        // Emerald Hero Summary Card
        item {
            DailyHeroSummaryCard(
                totalSales = uiState.dailySummary.totalSales,
                totalCollected = uiState.dailySummary.totalCollected,
                totalRemaining = uiState.dailySummary.totalRemaining,
                salesCount = uiState.dailySummary.salesCount,
                progress = uiState.dailySummary.collectionProgress,
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.dailySearchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_customer_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (uiState.dailySearchQuery.isNotEmpty()) {
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
                    .testTag("daily_search_input"),
            )
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val filters = listOf(
                    DailyFilter.ALL to stringResource(R.string.filter_all),
                    DailyFilter.PAID to stringResource(R.string.filter_paid),
                    DailyFilter.PARTIAL to stringResource(R.string.filter_partial),
                    DailyFilter.UNPAID to stringResource(R.string.filter_unpaid),
                )
                filters.forEach { (filter, label) ->
                    FilterChip(
                        selected = uiState.dailyFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("daily_filter_${filter.name.lowercase()}"),
                    )
                }
            }
        }

        // Sales List or Empty State
        if (uiState.dailySales.isEmpty()) {
            item {
                val isFilteredEmpty = uiState.allDaySalesCount > 0
                EmptyLedgerState(
                    title = stringResource(
                        if (isFilteredEmpty) R.string.empty_filter_title else R.string.empty_daily_title
                    ),
                    subtitle = stringResource(
                        if (isFilteredEmpty) R.string.empty_filter_subtitle else R.string.empty_daily_subtitle
                    ),
                    showIllustration = !isFilteredEmpty,
                )
            }
        } else {
            items(
                items = uiState.dailySales,
                key = { it.sale.id },
            ) { item ->
                SaleItemCard(
                    item = item,
                    showFullDate = false,
                    onCardClick = { onSaleClick(item) },
                    onQuickSettleClick = { onQuickSettleClick(item) },
                )
            }
        }
    }
}

@Composable
private fun DailyHeroSummaryCard(
    totalSales: Double,
    totalCollected: Double,
    totalRemaining: Double,
    salesCount: Int,
    progress: Float,
) {
    val ledgerColors = LocalLedgerColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ledgerColors.heroStart, ledgerColors.heroEnd),
                ),
            )
            .padding(20.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.total_sales),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        text = LedgerFormatters.formatShekel(totalSales),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "$salesCount عملية",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            if (totalSales > 0.0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFF5EEAD4),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
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
                            text = stringResource(R.string.collected_amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text(
                            text = LedgerFormatters.formatShekel(totalCollected),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF99F6E4),
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
                            text = stringResource(R.string.remaining_amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text(
                            text = LedgerFormatters.formatShekel(totalRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (totalRemaining > 0.0) Color(0xFFFDE68A) else Color.White,
                        )
                    }
                }
            }
        }
    }
}
