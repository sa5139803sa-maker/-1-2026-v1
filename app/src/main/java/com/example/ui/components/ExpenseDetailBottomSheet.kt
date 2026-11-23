package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailBottomSheet(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onUpdateExpense: (title: String, category: ExpenseCategory, amount: Double, note: String) -> Unit,
    onDeleteExpense: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ledgerColors = LocalLedgerColors.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    var editTitle by remember(expense.id) { mutableStateOf(expense.title) }
    var editCategory by remember(expense.id) { mutableStateOf(expense.categoryEnum) }
    var editAmount by remember(expense.id) {
        mutableStateOf(
            LedgerFormatters.formatShekel(expense.amount, showSymbol = false).replace(",", "")
        )
    }
    var editNote by remember(expense.id) { mutableStateOf(expense.note) }
    var editError by remember { mutableStateOf<String?>(null) }

    val errorEmptyTitle = stringResource(R.string.error_empty_expense_title)
    val errorInvalidAmount = stringResource(R.string.error_invalid_amount)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_expense_confirm_title)) },
            text = { Text(stringResource(R.string.delete_expense_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteExpense()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_delete_expense_button"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${LedgerFormatters.formatFullDate(expense.createdAtMillis)} • ${LedgerFormatters.formatTime(expense.createdAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row {
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier.testTag("edit_expense_icon_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_expense_icon_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.expense_amount_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = LedgerFormatters.formatShekel(expense.amount),
                                style = MaterialTheme.typography.headlineMedium,
                                color = ledgerColors.expense,
                            )
                        }
                        ExpenseCategoryBadge(category = expense.categoryEnum)
                    }

                    if (expense.note.isNotBlank()) {
                        Text(
                            text = "ملاحظة: ${expense.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            text = stringResource(R.string.edit_expense_title),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it; editError = null },
                            label = { Text(stringResource(R.string.expense_title_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExpenseCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = editCategory == cat,
                                    onClick = { editCategory = cat },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = cat.iconVector(),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    label = { Text(cat.labelAr) },
                                )
                            }
                        }

                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = {
                                editAmount = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                                editError = null
                            },
                            label = { Text(stringResource(R.string.expense_amount_label)) },
                            suffix = { Text(stringResource(R.string.currency_symbol)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        QuickAmountSelector(
                            currentText = editAmount,
                            onAmountChange = {
                                editAmount = it
                                editError = null
                            },
                            testTagPrefix = "edit_expense_quick",
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
                                    val amt = editAmount.replace(",", ".").toDoubleOrNull() ?: -1.0
                                    if (editTitle.trim().isEmpty()) {
                                        editError = errorEmptyTitle
                                    } else if (amt <= 0.0) {
                                        editError = errorInvalidAmount
                                    } else {
                                        onUpdateExpense(editTitle.trim(), editCategory, amt, editNote.trim())
                                        isEditing = false
                                    }
                                },
                                modifier = Modifier.testTag("save_edit_expense_button"),
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}
