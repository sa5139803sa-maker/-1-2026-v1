package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

fun ExpenseCategory.iconVector(): ImageVector = when (this) {
    ExpenseCategory.GOODS -> Icons.Rounded.ShoppingBag
    ExpenseCategory.RENT -> Icons.Rounded.Storefront
    ExpenseCategory.UTILITIES -> Icons.Rounded.Bolt
    ExpenseCategory.SALARIES -> Icons.Rounded.Groups
    ExpenseCategory.TRANSPORT -> Icons.Rounded.LocalShipping
    ExpenseCategory.MAINTENANCE -> Icons.Rounded.Build
    ExpenseCategory.OTHER -> Icons.AutoMirrored.Rounded.ReceiptLong
}

@Composable
fun ExpenseCategoryBadge(
    category: ExpenseCategory,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = ledgerColors.expenseContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = category.iconVector(),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = ledgerColors.onExpenseContainer,
            )
            Text(
                text = category.labelAr,
                style = MaterialTheme.typography.labelSmall,
                color = ledgerColors.onExpenseContainer,
            )
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    val category = expense.categoryEnum

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("expense_card_${expense.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.small,
                color = ledgerColors.expenseContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.iconVector(),
                        contentDescription = category.labelAr,
                        modifier = Modifier.size(22.dp),
                        tint = ledgerColors.onExpenseContainer,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = category.labelAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = ledgerColors.expense,
                    )
                    Text(
                        text = "• ${LedgerFormatters.formatShortDate(expense.createdAtMillis)} ${LedgerFormatters.formatTime(expense.createdAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (expense.note.isNotBlank()) {
                    Text(
                        text = expense.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = LedgerFormatters.formatShekel(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                color = ledgerColors.expense,
            )
        }
    }
}
