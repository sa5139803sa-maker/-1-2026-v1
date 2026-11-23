package com.example.ui.components

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
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailBottomSheet(
    saleWithPayments: SaleWithPayments,
    onDismiss: () -> Unit,
    onSettleFull: () -> Unit,
    onRecordPayment: (Double) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onUpdateSale: (customerName: String, totalAmount: Double, note: String) -> Unit,
    onDeleteSale: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledgerColors = LocalLedgerColors.current

    var paymentText by remember(saleWithPayments.sale.id, saleWithPayments.remainingAmount) {
        mutableStateOf("")
    }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var editName by remember(saleWithPayments.sale.id) { mutableStateOf(saleWithPayments.sale.customerName) }
    var editTotal by remember(saleWithPayments.sale.id) {
        mutableStateOf(LedgerFormatters.formatShekel(saleWithPayments.sale.totalAmount, showSymbol = false).replace(",", ""))
    }
    var editNote by remember(saleWithPayments.sale.id) { mutableStateOf(saleWithPayments.sale.note) }
    var editError by remember { mutableStateOf<String?>(null) }

    val errorInvalidAmount = stringResource(R.string.error_invalid_amount)
    val errorPaymentExceeds = stringResource(R.string.error_payment_exceeds)
    val errorEmptyCustomer = stringResource(R.string.error_empty_customer)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_sale_confirm_title)) },
            text = { Text(stringResource(R.string.delete_sale_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSale()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_delete_sale_button"),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = saleWithPayments.sale.customerName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${LedgerFormatters.formatFullDate(saleWithPayments.sale.createdAtMillis)} • ${LedgerFormatters.formatTime(saleWithPayments.sale.createdAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row {
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.testTag("edit_sale_icon_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_sale_icon_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (isEditing) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.edit_sale_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it; editError = null },
                            label = { Text(stringResource(R.string.customer_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = editTotal,
                            onValueChange = { editTotal = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; editError = null },
                            label = { Text(stringResource(R.string.total_amount_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text(stringResource(R.string.note_optional_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (editError != null) {
                            Text(
                                text = editError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { isEditing = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val newTotal = editTotal.replace(",", ".").toDoubleOrNull() ?: -1.0
                                    if (editName.trim().isEmpty()) {
                                        editError = errorEmptyCustomer
                                    } else if (newTotal <= 0.0) {
                                        editError = errorInvalidAmount
                                    } else {
                                        onUpdateSale(editName.trim(), newTotal, editNote.trim())
                                        isEditing = false
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.total_sales),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = LedgerFormatters.formatShekel(saleWithPayments.sale.totalAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        PaymentStatusBadge(status = saleWithPayments.status)
                    }

                    LinearProgressIndicator(
                        progress = { saleWithPayments.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ledgerColors.paid,
                        trackColor = ledgerColors.unpaidContainer,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MetricPill(
                            label = stringResource(R.string.collected_amount),
                            value = LedgerFormatters.formatShekel(saleWithPayments.paidAmount),
                            valueColor = ledgerColors.onPaidContainer,
                            containerColor = ledgerColors.paidContainer,
                            modifier = Modifier.weight(1f),
                        )
                        MetricPill(
                            label = stringResource(R.string.remaining_amount),
                            value = LedgerFormatters.formatShekel(saleWithPayments.remainingAmount),
                            valueColor = if (saleWithPayments.remainingAmount > 0.0) {
                                ledgerColors.onPartialContainer
                            } else {
                                ledgerColors.onPaidContainer
                            },
                            containerColor = if (saleWithPayments.remainingAmount > 0.0) {
                                ledgerColors.partialContainer
                            } else {
                                ledgerColors.paidContainer
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (saleWithPayments.sale.note.isNotBlank()) {
                        Text(
                            text = "ملاحظة: ${saleWithPayments.sale.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Record Payment Controls (if remaining > 0)
            if (saleWithPayments.remainingAmount > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSettleFull,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settle_full_button"),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.settle_remaining_button,
                                LedgerFormatters.formatShekel(saleWithPayments.remainingAmount),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        OutlinedTextField(
                            value = paymentText,
                            onValueChange = {
                                paymentText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
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
                                .testTag("record_payment_input"),
                            shape = MaterialTheme.shapes.medium,
                        )

                        OutlinedButton(
                            onClick = {
                                val amount = paymentText.replace(",", ".").toDoubleOrNull() ?: -1.0
                                if (amount <= 0.0) {
                                    paymentError = errorInvalidAmount
                                } else if (amount > saleWithPayments.remainingAmount + 0.01) {
                                    paymentError = errorPaymentExceeds
                                } else {
                                    onRecordPayment(amount)
                                    paymentText = ""
                                }
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .height(54.dp)
                                .testTag("record_payment_button"),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddCard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add_payment))
                        }
                    }

                    QuickAmountSelector(
                        currentText = paymentText,
                        onAmountChange = {
                            paymentText = it
                            paymentError = null
                        },
                        maxAmount = saleWithPayments.remainingAmount,
                        testTagPrefix = "detail_payment_quick",
                    )
                }
            }

            // Payment History
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.payment_history),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (saleWithPayments.payments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_payments_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    saleWithPayments.payments
                        .sortedByDescending { it.paidAtMillis }
                        .forEach { payment ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = LedgerFormatters.formatShekel(payment.amount),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ledgerColors.paid,
                                        )
                                        Text(
                                            text = "${LedgerFormatters.formatShortDate(payment.paidAtMillis)} • ${LedgerFormatters.formatTime(payment.paidAtMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeletePayment(payment.id) },
                                        modifier = Modifier.testTag("delete_payment_${payment.id}"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
