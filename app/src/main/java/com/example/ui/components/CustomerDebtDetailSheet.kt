package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SaleWithPayments
import com.example.ui.CustomerDebtSummary
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDebtDetailSheet(
    customerDebt: CustomerDebtSummary,
    onDismiss: () -> Unit,
    onSelectSale: (SaleWithPayments) -> Unit,
    onSettleAllCustomerDebts: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledgerColors = LocalLedgerColors.current
    var showConfirmSettleAll by remember { mutableStateOf(false) }

    if (showConfirmSettleAll) {
        AlertDialog(
            onDismissRequest = { showConfirmSettleAll = false },
            title = { Text("سداد جميع ذمم ${customerDebt.customerName}؟") },
            text = {
                Text(
                    "سيتم تسجيل سداد كامل لـ ${customerDebt.openSalesCount} فاتورة بإجمالي ${LedgerFormatters.formatShekel(customerDebt.totalRemaining)}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmSettleAll = false
                        onSettleAllCustomerDebts()
                    },
                    modifier = Modifier.testTag("confirm_settle_customer_button"),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSettleAll = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customerDebt.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.open_invoices_count, customerDebt.openSalesCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                MetricPill(
                    label = stringResource(R.string.remaining_amount),
                    value = LedgerFormatters.formatShekel(customerDebt.totalRemaining),
                    valueColor = ledgerColors.onPartialContainer,
                    containerColor = ledgerColors.partialContainer,
                )
            }

            Button(
                onClick = { showConfirmSettleAll = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settle_all_customer_button"),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سداد كامل الذمة (${LedgerFormatters.formatShekel(customerDebt.totalRemaining)})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            HorizontalDivider()

            Text(
                text = "الفواتير المفتوحة (اضغط للتفاصيل أو الدفع الجزئي)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            customerDebt.sales.forEach { saleItem ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelectSale(saleItem) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "${LedgerFormatters.formatShortDate(saleItem.sale.createdAtMillis)} • ${LedgerFormatters.formatDaysAgo(saleItem.sale.createdAtMillis)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "الإجمالي: ${LedgerFormatters.formatShekel(saleItem.sale.totalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (saleItem.sale.note.isNotBlank()) {
                                Text(
                                    text = saleItem.sale.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = LedgerFormatters.formatShekel(saleItem.remainingAmount),
                                style = MaterialTheme.typography.titleMedium,
                                color = ledgerColors.unpaid,
                            )
                            PaymentStatusBadge(status = saleItem.status)
                        }
                    }
                }
            }
        }
    }
}
