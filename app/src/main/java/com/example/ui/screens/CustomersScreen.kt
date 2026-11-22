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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.CustomerPurchaseSummary
import com.example.ui.CustomerSortOrder
import com.example.ui.SalesUiState
import com.example.ui.components.EmptyLedgerState
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@Composable
fun CustomersScreen(
    uiState: SalesUiState,
    onNavigateBackToDaily: () -> Unit,
    onSortOrderChange: (CustomerSortOrder) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCustomerClick: (CustomerPurchaseSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onNavigateBackToDaily() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Top Buyer & Customers Overview Banner
        item {
            CustomersHeroCard(
                totalCustomersCount = uiState.totalCustomersCount,
                allCustomersTotalPurchases = uiState.allCustomersTotalPurchases,
                topBuyer = uiState.topBuyer,
                onTopBuyerClick = { top -> onCustomerClick(top) },
            )
        }

        // Search & Sort Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.customersSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.customer_search_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.customersSearchQuery.isNotEmpty()) {
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
                        .testTag("customers_search_input"),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val sorts = listOf(
                        CustomerSortOrder.MOST_PURCHASES to stringResource(R.string.sort_most_purchases),
                        CustomerSortOrder.MOST_ORDERS to stringResource(R.string.sort_most_orders),
                        CustomerSortOrder.HIGHEST_DEBT to stringResource(R.string.sort_most_debt),
                        CustomerSortOrder.RECENT_ACTIVITY to stringResource(R.string.sort_recent_activity),
                    )
                    sorts.forEach { (sort, label) ->
                        FilterChip(
                            selected = uiState.customersSortOrder == sort,
                            onClick = { onSortOrderChange(sort) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("customers_sort_${sort.name.lowercase()}"),
                        )
                    }
                }
            }
        }

        if (uiState.totalCustomersCount == 0) {
            item {
                EmptyLedgerState(
                    title = stringResource(R.string.empty_customers_title),
                    subtitle = stringResource(R.string.empty_customers_subtitle),
                    showIllustration = true,
                )
            }
        } else if (uiState.customersList.isEmpty()) {
            item {
                EmptyLedgerState(
                    title = stringResource(R.string.empty_filter_title),
                    subtitle = stringResource(R.string.empty_filter_subtitle),
                    showIllustration = false,
                )
            }
        } else {
            items(
                items = uiState.customersList,
                key = { it.customerName.lowercase() },
            ) { summary ->
                CustomerPurchaseCard(
                    summary = summary,
                    onClick = { onCustomerClick(summary) },
                )
            }
        }
    }
}

@Composable
private fun CustomersHeroCard(
    totalCustomersCount: Int,
    allCustomersTotalPurchases: Double,
    topBuyer: CustomerPurchaseSummary?,
    onTopBuyerClick: (CustomerPurchaseSummary) -> Unit,
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.total_purchases_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        text = LedgerFormatters.formatShekel(allCustomersTotalPurchases),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.customers_count, totalCustomersCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                        )
                    }
                }
            }

            if (topBuyer != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onTopBuyerClick(topBuyer) }
                        .testTag("top_buyer_banner"),
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.14f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDE68A).copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFFDE68A),
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.top_buyer_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFDE68A),
                                )
                                Text(
                                    text = topBuyer.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = LedgerFormatters.formatShekel(topBuyer.totalPurchases),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF99F6E4),
                            )
                            Text(
                                text = "${topBuyer.purchasesCount} عملية",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerPurchaseCard(
    summary: CustomerPurchaseSummary,
    onClick: () -> Unit,
) {
    val ledgerColors = LocalLedgerColors.current
    val isTopThree = summary.rank in 1..3

    val (rankBg, rankFg) = when (summary.rank) {
        1 -> ledgerColors.partialContainer to ledgerColors.onPartialContainer
        2 -> ledgerColors.paidContainer to ledgerColors.onPaidContainer
        3 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_purchase_card_${summary.customerName}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (summary.rank == 1) 1.5.dp else 1.dp,
            color = if (summary.rank == 1) {
                ledgerColors.partial.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTopThree) 2.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(rankBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "#${summary.rank}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = rankFg,
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
                        text = "${summary.purchasesCount} عملية • آخر شراء ${LedgerFormatters.formatShortDate(summary.lastPurchaseMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = LedgerFormatters.formatShekel(summary.totalPurchases),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.total_purchases_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { summary.paidProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = ledgerColors.paid,
                trackColor = if (summary.totalRemaining > 0.0) {
                    ledgerColors.partialContainer
                } else {
                    ledgerColors.paidContainer
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "مسدد: ${LedgerFormatters.formatShekel(summary.totalPaid)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ledgerColors.paid,
                )

                if (summary.totalRemaining > 0.0) {
                    Text(
                        text = "غير مسدد: ${LedgerFormatters.formatShekel(summary.totalRemaining)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ledgerColors.unpaid,
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ledgerColors.paidContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.all_paid_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ledgerColors.onPaidContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}
