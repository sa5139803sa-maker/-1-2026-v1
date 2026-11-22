package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SaleWithPayments
import com.example.ui.CustomerDebtSummary
import com.example.ui.SalesUiState
import com.example.ui.UnpaidSortOrder
import com.example.ui.UnpaidViewMode
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.SaleItemCard
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@Composable
fun UnpaidDebtsScreen(
    uiState: SalesUiState,
    onNavigateBackToDaily: () -> Unit,
    onViewModeChange: (UnpaidViewMode) -> Unit,
    onSortOrderChange: (UnpaidSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaleClick: (SaleWithPayments) -> Unit,
    onQuickSettleSale: (SaleWithPayments) -> Unit,
    onCustomerClick: (CustomerDebtSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onNavigateBackToDaily() }

    val ledgerColors = LocalLedgerColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Total Unpaid Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (uiState.totalUnpaidBalance > 0.0) {
                    ledgerColors.partialContainer
                } else {
                    ledgerColors.paidContainer
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.total_unpaid_balance),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (uiState.totalUnpaidBalance > 0.0) {
                                ledgerColors.onPartialContainer
                            } else {
                                ledgerColors.onPaidContainer
                            },
                        )
                        Text(
                            text = LedgerFormatters.formatShekel(uiState.totalUnpaidBalance),
                            style = MaterialTheme.typography.displaySmall,
                            color = if (uiState.totalUnpaidBalance > 0.0) {
                                ledgerColors.onPartialContainer
                            } else {
                                ledgerColors.onPaidContainer
                            },
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.open_invoices_count, uiState.totalUnpaidInvoicesCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.totalUnpaidBalance > 0.0) {
                                    ledgerColors.onPartialContainer.copy(alpha = 0.85f)
                                } else {
                                    ledgerColors.onPaidContainer.copy(alpha = 0.85f)
                                },
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.totalUnpaidBalance > 0.0) {
                                    ledgerColors.onPartialContainer
                                } else {
                                    ledgerColors.onPaidContainer
                                },
                            )
                            Text(
                                text = stringResource(R.string.customers_count, uiState.totalUnpaidCustomersCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.totalUnpaidBalance > 0.0) {
                                    ledgerColors.onPartialContainer.copy(alpha = 0.85f)
                                } else {
                                    ledgerColors.onPaidContainer.copy(alpha = 0.85f)
                                },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.totalUnpaidBalance > 0.0) {
                                    ledgerColors.partial.copy(alpha = 0.16f)
                                } else {
                                    ledgerColors.paid.copy(alpha = 0.16f)
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (uiState.totalUnpaidBalance > 0.0) {
                                Icons.Rounded.AccountBalanceWallet
                            } else {
                                Icons.Rounded.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (uiState.totalUnpaidBalance > 0.0) {
                                ledgerColors.partial
                            } else {
                                ledgerColors.paid
                            },
                        )
                    }
                }
            }
        }

        // View Mode Toggle (By Sale / By Customer)
        item {
            val modes = listOf(
                UnpaidViewMode.BY_SALE to stringResource(R.string.view_by_sale),
                UnpaidViewMode.BY_CUSTOMER to stringResource(R.string.view_by_customer),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = uiState.unpaidViewMode == mode,
                        onClick = { onViewModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        modifier = Modifier.testTag("unpaid_mode_${mode.name.lowercase()}"),
                        label = { Text(label) },
                    )
                }
            }
        }

        // Search & Sort Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.unpaidSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.search_customer_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.unpaidSearchQuery.isNotEmpty()) {
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
                        .testTag("unpaid_search_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sorts = listOf(
                        UnpaidSortOrder.HIGHEST_AMOUNT to stringResource(R.string.sort_highest),
                        UnpaidSortOrder.OLDEST_FIRST to stringResource(R.string.sort_oldest),
                        UnpaidSortOrder.NEWEST_FIRST to stringResource(R.string.sort_newest),
                    )
                    sorts.forEach { (sort, label) ->
                        FilterChip(
                            selected = uiState.unpaidSortOrder == sort,
                            onClick = { onSortOrderChange(sort) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("unpaid_sort_${sort.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        // Content List
        if (uiState.totalUnpaidInvoicesCount == 0) {
            item {
                EmptyLedgerState(
                    title = stringResource(R.string.empty_unpaid_title),
                    subtitle = stringResource(R.string.empty_unpaid_subtitle),
                    showIllustration = true,
                )
            }
        } else if (uiState.unpaidViewMode == UnpaidViewMode.BY_SALE) {
            if (uiState.unpaidSales.isEmpty()) {
                item {
                    EmptyLedgerState(
                        title = stringResource(R.string.empty_filter_title),
                        subtitle = stringResource(R.string.empty_filter_subtitle),
                        showIllustration = false,
                    )
                }
            } else {
                items(
                    items = uiState.unpaidSales,
                    key = { it.sale.id },
                ) { item ->
                    SaleItemCard(
                        item = item,
                        showFullDate = true,
                        onCardClick = { onSaleClick(item) },
                        onQuickSettleClick = { onQuickSettleSale(item) },
                    )
                }
            }
        } else {
            if (uiState.unpaidByCustomer.isEmpty()) {
                item {
                    EmptyLedgerState(
                        title = stringResource(R.string.empty_filter_title),
                        subtitle = stringResource(R.string.empty_filter_subtitle),
                        showIllustration = false,
                    )
                }
            } else {
                items(
                    items = uiState.unpaidByCustomer,
                    key = { it.customerName },
                ) { customerSummary ->
                    CustomerDebtCard(
                        summary = customerSummary,
                        onClick = { onCustomerClick(customerSummary) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerDebtCard(
    summary: CustomerDebtSummary,
    onClick: () -> Unit,
) {
    val ledgerColors = LocalLedgerColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_debt_card_${summary.customerName}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ledgerColors.partial.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ledgerColors.partialContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = summary.customerName.trim().take(1).ifEmpty { "ع" },
                    style = MaterialTheme.typography.titleMedium,
                    color = ledgerColors.onPartialContainer,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = summary.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${stringResource(R.string.open_invoices_count, summary.openSalesCount)} • أقدمها ${LedgerFormatters.formatDaysAgo(summary.oldestSaleMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = LedgerFormatters.formatShekel(summary.totalRemaining),
                    style = MaterialTheme.typography.titleLarge,
                    color = ledgerColors.unpaid,
                )
                Text(
                    text = "اضغط للسداد",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
