package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SaleWithPayments
import com.example.ui.CustomerPurchaseSummary
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPurchasesDetailSheet(
    customerSummary: CustomerPurchaseSummary,
    onDismiss: () -> Unit,
    onSelectSale: (SaleWithPayments) -> Unit,
    onAddSaleForCustomer: () -> Unit,
    onRecordAccountPayment: (Double) -> Unit,
    onSettleAllDebts: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledgerColors = LocalLedgerColors.current
    var showConfirmSettle by remember { mutableStateOf(false) }

    var accountPaymentText by remember(customerSummary.customerName, customerSummary.totalRemaining) {
        mutableStateOf("")
    }
    var paymentError by remember { mutableStateOf<String?>(null) }

    val errorInvalidAmount = stringResource(R.string.error_invalid_amount)
    val errorPaymentExceeds = stringResource(R.string.error_payment_exceeds)

    val parsedAmount = accountPaymentText.replace(",", ".").toDoubleOrNull()
    val fifoPreview = remember(parsedAmount, customerSummary.sales) {
        val amt = parsedAmount ?: 0.0
        if (amt <= 0.0) emptyList()
        else computeFifoPreview(customerSummary.sales, amt)
    }

    if (showConfirmSettle) {
        AlertDialog(
            onDismissRequest = { showConfirmSettle = false },
            title = { Text("سداد جميع ذمم ${customerSummary.customerName}؟") },
            text = {
                Text(
                    "سيتم تسجيل سداد كامل للمبلغ المتبقي بقيمة ${LedgerFormatters.formatShekel(customerSummary.totalRemaining)}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmSettle = false
                        onSettleAllDebts()
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSettle = false }) {
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
                .imePadding()
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
                        text = customerSummary.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "المركز #${customerSummary.rank} • ${customerSummary.purchasesCount} عملية شراء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                MetricPill(
                    label = stringResource(R.string.total_purchases_label),
                    value = LedgerFormatters.formatShekel(customerSummary.totalPurchases),
                    valueColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            LinearProgressIndicator(
                progress = { customerSummary.paidProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = ledgerColors.paid,
                trackColor = if (customerSummary.totalRemaining > 0.0) {
                    ledgerColors.partialContainer
                } else {
                    ledgerColors.paidContainer
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricPill(
                    label = stringResource(R.string.collected_amount),
                    value = LedgerFormatters.formatShekel(customerSummary.totalPaid),
                    valueColor = ledgerColors.onPaidContainer,
                    containerColor = ledgerColors.paidContainer,
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.remaining_amount),
                    value = LedgerFormatters.formatShekel(customerSummary.totalRemaining),
                    valueColor = if (customerSummary.totalRemaining > 0.0) {
                        ledgerColors.onUnpaidContainer
                    } else {
                        ledgerColors.onPaidContainer
                    },
                    containerColor = if (customerSummary.totalRemaining > 0.0) {
                        ledgerColors.unpaidContainer
                    } else {
                        ledgerColors.paidContainer
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Account-level FIFO payment when customer has remaining balance
            if (customerSummary.totalRemaining > 0.0) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "دفعة على الحساب (توزيع تلقائي من الأقدم للأحدث)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            OutlinedTextField(
                                value = accountPaymentText,
                                onValueChange = {
                                    accountPaymentText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                                    paymentError = null
                                },
                                label = { Text(stringResource(R.string.new_payment_amount)) },
                                suffix = { Text(stringResource(R.string.currency_symbol)) },
                                isError = paymentError != null,
                                supportingText = paymentError?.let { msg -> { Text(msg) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_purchases_account_payment_input"),
                                shape = MaterialTheme.shapes.medium,
                            )

                            OutlinedButton(
                                onClick = {
                                    val amount = parsedAmount ?: -1.0
                                    if (amount <= 0.0) {
                                        paymentError = errorInvalidAmount
                                    } else if (amount > customerSummary.totalRemaining + 0.01) {
                                        paymentError = errorPaymentExceeds
                                    } else {
                                        onRecordAccountPayment(amount)
                                        accountPaymentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(54.dp)
                                    .testTag("customer_purchases_account_payment_button"),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCard,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توزيع")
                            }
                        }

                        QuickAmountSelector(
                            currentText = accountPaymentText,
                            onAmountChange = {
                                accountPaymentText = it
                                paymentError = null
                            },
                            maxAmount = customerSummary.totalRemaining,
                            testTagPrefix = "purchases_account_quick",
                        )

                        AnimatedVisibility(visible = fifoPreview.isNotEmpty()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ledgerColors.paidContainer.copy(alpha = 0.7f),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "معاينة التوزيع التلقائي:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ledgerColors.onPaidContainer,
                                    )
                                    fifoPreview.forEach { step ->
                                        Text(
                                            text = "• فاتورة ${LedgerFormatters.formatShortDate(step.saleMillis)}: " +
                                                "${LedgerFormatters.formatShekel(step.appliedAmount)} " +
                                                if (step.willClose) "(تُسدد بالكامل)" else "(يتبقى ${LedgerFormatters.formatShekel(step.newRemaining)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ledgerColors.onPaidContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onAddSaleForCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("customer_new_sale_button"),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.add_sale),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (customerSummary.totalRemaining > 0.0) {
                    Button(
                        onClick = { showConfirmSettle = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("customer_settle_all_button"),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settle_full),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "سجل جميع المشتريات (${customerSummary.purchasesCount})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            customerSummary.sales.forEach { saleItem ->
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
                                text = "${LedgerFormatters.formatShortDate(saleItem.sale.createdAtMillis)} • ${LedgerFormatters.formatTime(saleItem.sale.createdAtMillis)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (saleItem.remainingAmount > 0.0) {
                                Text(
                                    text = "متبقي: ${LedgerFormatters.formatShekel(saleItem.remainingAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ledgerColors.unpaid,
                                )
                            }
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
                                text = LedgerFormatters.formatShekel(saleItem.sale.totalAmount),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            PaymentStatusBadge(status = saleItem.status)
                        }
                    }
                }
            }
        }
    }
}
