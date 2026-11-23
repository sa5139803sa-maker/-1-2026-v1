package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PaymentStatus
import com.example.ui.CustomerPurchaseSummary
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSaleBottomSheet(
    knownCustomers: List<CustomerPurchaseSummary>,
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

    val normalizedQuery = remember(customerName) { LedgerFormatters.normalizeArabicText(customerName) }

    val registeredMatch = remember(normalizedQuery, knownCustomers) {
        if (normalizedQuery.isEmpty()) null
        else knownCustomers.find { LedgerFormatters.normalizeArabicText(it.customerName) == normalizedQuery }
    }

    val matchingSuggestions = remember(normalizedQuery, customerName, knownCustomers, registeredMatch) {
        if (normalizedQuery.isEmpty()) {
            knownCustomers.take(6)
        } else {
            knownCustomers
                .filter { customer ->
                    customer != registeredMatch &&
                        LedgerFormatters.matchesAllNameParts(customer.customerName, customerName)
                }
                .sortedByDescending {
                    LedgerFormatters.normalizeArabicText(it.customerName).startsWith(normalizedQuery)
                }
                .take(6)
        }
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

            // Instant Customer Recognition Card (Registered vs New)
            AnimatedVisibility(
                visible = normalizedQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                if (registeredMatch != null) {
                    val hasDebt = registeredMatch.totalRemaining > 0.0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registered_customer_badge"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (hasDebt) ledgerColors.partialContainer else ledgerColors.paidContainer,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (hasDebt) {
                                ledgerColors.partial.copy(alpha = 0.4f)
                            } else {
                                ledgerColors.paid.copy(alpha = 0.4f)
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
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
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (hasDebt) ledgerColors.onPartialContainer else ledgerColors.onPaidContainer,
                                    )
                                    Text(
                                        text = "زبون مسجل مسبقاً (${registeredMatch.customerName})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (hasDebt) ledgerColors.onPartialContainer else ledgerColors.onPaidContainer,
                                    )
                                }
                                Text(
                                    text = "${registeredMatch.purchasesCount} عملية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasDebt) ledgerColors.onPartialContainer else ledgerColors.onPaidContainer,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "إجمالي مشترياته: ${LedgerFormatters.formatShekel(registeredMatch.totalPurchases)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasDebt) ledgerColors.onPartialContainer else ledgerColors.onPaidContainer,
                                )
                                Text(
                                    text = if (hasDebt) {
                                        "عليه ذمة: ${LedgerFormatters.formatShekel(registeredMatch.totalRemaining)}"
                                    } else {
                                        "لا توجد ذمم"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (hasDebt) ledgerColors.unpaid else ledgerColors.onPaidContainer,
                                )
                            }

                            // If user typed slightly different spelling (e.g., احمد vs أحمد), offer 1-tap canonical sync
                            if (customerName.trim() != registeredMatch.customerName) {
                                TextButton(
                                    onClick = { customerName = registeredMatch.customerName },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text("استخدام الاسم المسجل: ${registeredMatch.customerName}")
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_customer_badge"),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (matchingSuggestions.isNotEmpty()) {
                                    "زبون جديد — أو اختر من الزبائن المسجلين أدناه"
                                } else {
                                    "زبون جديد — سيتم إضافته لقائمة الزبائن"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Smart Customer Autocomplete Suggestions with Unpaid Debt Hint
            if (matchingSuggestions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    matchingSuggestions.forEach { suggestion ->
                        val hasDebt = suggestion.totalRemaining > 0.0
                        Surface(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    customerName = suggestion.customerName
                                    customerError = null
                                }
                                .testTag("customer_suggestion_${suggestion.customerName}"),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                if (hasDebt) {
                                    ledgerColors.partial.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = suggestion.customerName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (hasDebt) {
                                    Text(
                                        text = "(${LedgerFormatters.formatShekel(suggestion.totalRemaining)})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ledgerColors.partial,
                                    )
                                }
                            }
                        }
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

            QuickAmountSelector(
                currentText = totalAmountText,
                onAmountChange = {
                    totalAmountText = it
                    amountError = null
                },
                testTagPrefix = "sale_total_quick",
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

                    QuickAmountSelector(
                        currentText = partialPaymentText,
                        onAmountChange = {
                            partialPaymentText = it
                            partialError = null
                        },
                        maxAmount = parsedTotal?.takeIf { it > 0.0 },
                        testTagPrefix = "sale_partial_quick",
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
