package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PaymentStatus
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSaleBottomSheet(
    recentCustomers: List<String>,
    selectedDayMillis: Long,
    initialCustomerName: String = "",
    onDismiss: () -> Unit,
    onSaveSale: (customerName: String, totalAmount: Double, initialPayment: Double, note: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledgerColors = LocalLedgerColors.current

    var customerName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var totalAmountText by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(PaymentStatus.PAID) }
    var partialPaymentText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }

    var customerError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var partialError by remember { mutableStateOf<String?>(null) }

    val errorEmptyCustomer = stringResource(R.string.error_empty_customer)
    val errorInvalidAmount = stringResource(R.string.error_invalid_amount)
    val errorPartialExceeds = stringResource(R.string.error_partial_exceeds)

    val parsedTotal = totalAmountText.replace(",", ".").toDoubleOrNull()
    val parsedPartial = partialPaymentText.replace(",", ".").toDoubleOrNull()

    val matchingSuggestions = remember(customerName, recentCustomers) {
        val q = customerName.trim()
        if (q.isEmpty()) recentCustomers.take(5)
        else recentCustomers.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }.take(4)
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
                Column {
                    Text(
                        text = stringResource(R.string.new_sale_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = LedgerFormatters.formatDayLabel(selectedDayMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!showNoteField) {
                    TextButton(
                        onClick = { showNoteField = true },
                        modifier = Modifier.testTag("toggle_note_button"),
                    ) {
                        Text("+ ملاحظة")
                    }
                }
            }

            // 1. Customer Name Input
            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    customerError = null
                },
                label = { Text(stringResource(R.string.customer_name_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (customerName.isNotEmpty()) {
                        IconButton(onClick = { customerName = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                isError = customerError != null,
                supportingText = customerError?.let { msg -> { Text(msg) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input"),
                shape = MaterialTheme.shapes.medium,
            )

            // Recent Customer Quick Chips
            if (matchingSuggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    matchingSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                customerName = suggestion
                                customerError = null
                            },
                            label = {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }
            }

            // 2. Total Amount Input (Shekel)
            OutlinedTextField(
                value = totalAmountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                    totalAmountText = filtered
                    amountError = null
                },
                label = { Text(stringResource(R.string.total_amount_label)) },
                suffix = {
                    Text(
                        text = stringResource(R.string.currency_symbol),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                isError = amountError != null,
                supportingText = amountError?.let { msg -> { Text(msg) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (selectedStatus == PaymentStatus.PARTIAL) ImeAction.Next else ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_amount_input"),
                shape = MaterialTheme.shapes.medium,
            )

            // 3. Payment Status Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.payment_status_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val options = listOf(
                    PaymentStatus.PAID to stringResource(R.string.filter_paid),
                    PaymentStatus.PARTIAL to stringResource(R.string.filter_partial),
                    PaymentStatus.UNPAID to stringResource(R.string.filter_unpaid),
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    options.forEachIndexed { index, (status, label) ->
                        SegmentedButton(
                            selected = selectedStatus == status,
                            onClick = {
                                selectedStatus = status
                                partialError = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                            modifier = Modifier.testTag("status_option_${status.name.lowercase()}"),
                            icon = {
                                if (selectedStatus == status) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }

            // 4. Partial Payment Input (animated)
            AnimatedVisibility(
                visible = selectedStatus == PaymentStatus.PARTIAL,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = partialPaymentText,
                        onValueChange = { input ->
                            partialPaymentText = input.filter { it.isDigit() || it == '.' || it == ',' }
                            partialError = null
                        },
                        label = { Text(stringResource(R.string.initial_payment_label)) },
                        suffix = {
                            Text(
                                text = stringResource(R.string.currency_symbol),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        isError = partialError != null,
                        supportingText = partialError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partial_payment_input"),
                        shape = MaterialTheme.shapes.medium,
                    )

                    if (parsedTotal != null && parsedTotal > 0.0) {
                        val paidPreview = (parsedPartial ?: 0.0).coerceIn(0.0, parsedTotal)
                        val remainingPreview = (parsedTotal - paidPreview).coerceAtLeast(0.0)
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ledgerColors.partialContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "المتبقي في الذمة:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ledgerColors.onPartialContainer,
                                )
                                Text(
                                    text = LedgerFormatters.formatShekel(remainingPreview),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ledgerColors.onPartialContainer,
                                )
                            }
                        }
                    }
                }
            }

            // 5. Optional Note
            AnimatedVisibility(visible = showNoteField) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.note_optional_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sale_note_input"),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_add_sale_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        val trimmedName = customerName.trim()
                        val total = parsedTotal ?: -1.0

                        var valid = true
                        if (trimmedName.isEmpty()) {
                            customerError = errorEmptyCustomer
                            valid = false
                        }
                        if (total <= 0.0) {
                            amountError = errorInvalidAmount
                            valid = false
                        }
                        val initialPayment = when (selectedStatus) {
                            PaymentStatus.PAID -> total
                            PaymentStatus.UNPAID -> 0.0
                            PaymentStatus.PARTIAL -> {
                                val p = parsedPartial ?: -1.0
                                if (p <= 0.0) {
                                    partialError = errorInvalidAmount
                                    valid = false
                                    0.0
                                } else if (total > 0.0 && p >= total) {
                                    partialError = errorPartialExceeds
                                    valid = false
                                    0.0
                                } else {
                                    p
                                }
                            }
                        }
                        if (valid) {
                            onSaveSale(trimmedName, total, initialPayment, noteText)
                        }
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("save_sale_button"),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
