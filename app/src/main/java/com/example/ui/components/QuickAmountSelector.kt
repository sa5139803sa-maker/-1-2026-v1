package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.LedgerFormatters
import kotlin.math.abs
import kotlin.math.roundToLong

private val QuickSetAmounts = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 50, 100)
private val QuickAddAmounts = listOf(1, 2, 5, 10, 20, 50, 100)

@Composable
fun QuickAmountSelector(
    currentText: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxAmount: Double? = null,
    testTagPrefix: String = "quick_amount",
) {
    val currentVal = currentText.replace(",", ".").toDoubleOrNull() ?: 0.0

    fun formatInputNumber(value: Double): String {
        val clamped = if (maxAmount != null) value.coerceIn(0.0, maxAmount) else value.coerceAtLeast(0.0)
        val rounded = (clamped * 100.0).roundToLong() / 100.0
        return if (abs(rounded - rounded.roundToLong()) < 0.005) {
            rounded.roundToLong().toString()
        } else {
            LedgerFormatters.formatShekel(rounded, showSymbol = false).replace(",", "")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1: Direct quick amounts 1..10 (+ 20, 50, 100)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickSetAmounts.forEach { amt ->
                val enabled = maxAmount == null || amt <= maxAmount + 0.01
                QuickChipButton(
                    label = "$amt ₪",
                    isPrimary = false,
                    enabled = enabled,
                    onClick = {
                        onAmountChange(formatInputNumber(amt.toDouble()))
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_set_$amt"),
                )
            }
        }

        // Row 2: Increment chips (+1, +2, +5, +10, +20, +50, +100) + Half/Full/Clear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (maxAmount != null && maxAmount >= 2.0) {
                QuickChipButton(
                    label = "النصف",
                    isPrimary = true,
                    enabled = true,
                    onClick = { onAmountChange(formatInputNumber(maxAmount / 2.0)) },
                    modifier = Modifier.testTag("${testTagPrefix}_half"),
                )
                QuickChipButton(
                    label = "الكل",
                    isPrimary = true,
                    enabled = true,
                    onClick = { onAmountChange(formatInputNumber(maxAmount)) },
                    modifier = Modifier.testTag("${testTagPrefix}_full"),
                )
            }

            QuickAddAmounts.forEach { delta ->
                val canAdd = maxAmount == null || currentVal < maxAmount - 0.009
                QuickChipButton(
                    label = "+$delta",
                    isPrimary = true,
                    enabled = canAdd,
                    onClick = {
                        onAmountChange(formatInputNumber(currentVal + delta.toDouble()))
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_add_$delta"),
                )
            }

            if (currentText.isNotEmpty()) {
                QuickChipButton(
                    label = "مسح",
                    isPrimary = false,
                    enabled = true,
                    onClick = { onAmountChange("") },
                    modifier = Modifier.testTag("${testTagPrefix}_clear"),
                )
            }
        }
    }
}

@Composable
private fun QuickChipButton(
    label: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        isPrimary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        isPrimary -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
            )
        }
    }
}
