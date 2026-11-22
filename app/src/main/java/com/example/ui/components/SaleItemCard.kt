package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PaymentStatus
import com.example.data.SaleWithPayments
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters

@Composable
fun SaleItemCard(
    item: SaleWithPayments,
    showFullDate: Boolean,
    onCardClick: () -> Unit,
    onQuickSettleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    val (accentColor, containerColor, onContainerColor) = when (item.status) {
        PaymentStatus.PAID -> Triple(
            ledgerColors.paid,
            ledgerColors.paidContainer,
            ledgerColors.onPaidContainer,
        )
        PaymentStatus.PARTIAL -> Triple(
            ledgerColors.partial,
            ledgerColors.partialContainer,
            ledgerColors.onPartialContainer,
        )
        PaymentStatus.UNPAID -> Triple(
            ledgerColors.unpaid,
            ledgerColors.unpaidContainer,
            ledgerColors.onUnpaidContainer,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sale_item_card_${item.sale.id}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.status == PaymentStatus.PAID) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                accentColor.copy(alpha = 0.35f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                // Customer Avatar Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.sale.customerName.trim().take(1).ifEmpty { "ع" },
                        style = MaterialTheme.typography.titleMedium,
                        color = onContainerColor,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = item.sale.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (showFullDate) {
                                "${LedgerFormatters.formatShortDate(item.sale.createdAtMillis)} • ${LedgerFormatters.formatDaysAgo(item.sale.createdAtMillis)}"
                            } else {
                                LedgerFormatters.formatTime(item.sale.createdAtMillis)
                            },
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
                        text = LedgerFormatters.formatShekel(item.sale.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    PaymentStatusBadge(status = item.status)
                }
            }

            if (item.sale.note.isNotBlank()) {
                Text(
                    text = item.sale.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.remainingAmount > 0.0) {
                if (item.status == PaymentStatus.PARTIAL) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50)),
                        color = ledgerColors.paid,
                        trackColor = containerColor,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (item.paidAmount > 0.0) {
                            Text(
                                text = "مدفوع: ${LedgerFormatters.formatShekel(item.paidAmount)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ledgerColors.paid,
                            )
                        }
                        Text(
                            text = "متبقي: ${LedgerFormatters.formatShekel(item.remainingAmount)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                        )
                    }

                    FilledTonalButton(
                        onClick = onQuickSettleClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_settle_button_${item.sale.id}"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = stringResource(R.string.pay_now),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.pay_now),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentStatusBadge(
    status: PaymentStatus,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    val (label, bgColor, textColor) = when (status) {
        PaymentStatus.PAID -> Triple(
            stringResource(R.string.filter_paid),
            ledgerColors.paidContainer,
            ledgerColors.onPaidContainer,
        )
        PaymentStatus.PARTIAL -> Triple(
            stringResource(R.string.filter_partial),
            ledgerColors.partialContainer,
            ledgerColors.onPartialContainer,
        )
        PaymentStatus.UNPAID -> Triple(
            stringResource(R.string.filter_unpaid),
            ledgerColors.unpaidContainer,
            ledgerColors.onUnpaidContainer,
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (status == PaymentStatus.PAID) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = textColor,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor,
            )
        }
    }
}

@Composable
fun EmptyLedgerState(
    title: String,
    subtitle: String,
    showIllustration: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showIllustration) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_ledger_1795357930240),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    valueColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = valueColor.copy(alpha = 0.85f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = valueColor,
            )
        }
    }
}
