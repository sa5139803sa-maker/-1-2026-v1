package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.ChartBucket
import com.example.ui.theme.LocalLedgerColors
import com.example.util.LedgerFormatters
import kotlin.math.max

@Composable
fun FinancialBarChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return

    val ledgerColors = LocalLedgerColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    var selectedIndex by remember(buckets) {
        mutableIntStateOf(buckets.indexOfLast { it.total > 0.0 }.takeIf { it >= 0 } ?: buckets.lastIndex)
    }

    val animProgress = remember(buckets) { Animatable(0f) }
    LaunchedEffect(buckets) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        )
    }

    val maxTotal = remember(buckets) {
        max(1.0, buckets.maxOfOrNull { it.total } ?: 1.0)
    }

    val activeBucket = buckets.getOrNull(selectedIndex)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Legend & Selected Bucket Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendDot(
                    color = ledgerColors.chartCollected,
                    label = stringResource(R.string.collected_amount),
                )
                ChartLegendDot(
                    color = ledgerColors.chartRemaining,
                    label = stringResource(R.string.remaining_amount),
                )
            }

            if (activeBucket != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "${activeBucket.label}: ${LedgerFormatters.formatShekel(activeBucket.total)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (activeBucket != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "محصّل: ${LedgerFormatters.formatShekel(activeBucket.collected)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ledgerColors.paid,
                )
                Text(
                    text = "متبقي: ${LedgerFormatters.formatShekel(activeBucket.remaining)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ledgerColors.partial,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(buckets, isRtl) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width.toFloat() / buckets.size.coerceAtLeast(1)
                        val visualIndex = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
                        selectedIndex = if (isRtl) buckets.lastIndex - visualIndex else visualIndex
                    }
                },
        ) {
            val chartHeight = size.height
            val slotWidth = size.width / buckets.size.coerceAtLeast(1)
            val barWidth = (slotWidth * 0.44f).coerceIn(14.dp.toPx(), 34.dp.toPx())
            val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // Horizontal guidelines
            for (step in 0..3) {
                val y = chartHeight * (step / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (step == 3) null else dashEffect,
                )
            }

            buckets.forEachIndexed { index, bucket ->
                val visualIndex = if (isRtl) buckets.lastIndex - index else index
                val slotLeft = visualIndex * slotWidth
                val barLeft = slotLeft + (slotWidth - barWidth) / 2f

                if (index == selectedIndex) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(slotLeft + 4.dp.toPx(), 0f),
                        size = Size(slotWidth - 8.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    )
                }

                val totalHeight = ((bucket.total / maxTotal).toFloat() * (chartHeight - 10.dp.toPx())) * animProgress.value
                val collectedHeight = if (bucket.total > 0.0) {
                    (bucket.collected / bucket.total).toFloat() * totalHeight
                } else {
                    0f
                }
                val remainingHeight = (totalHeight - collectedHeight).coerceAtLeast(0f)

                if (collectedHeight > 1f) {
                    drawRoundRect(
                        color = ledgerColors.chartCollected,
                        topLeft = Offset(barLeft, chartHeight - collectedHeight),
                        size = Size(barWidth, collectedHeight),
                        cornerRadius = cornerRadius,
                    )
                }

                if (remainingHeight > 1f) {
                    val gap = if (collectedHeight > 1f) 2.dp.toPx() else 0f
                    val drawRemHeight = (remainingHeight - gap).coerceAtLeast(2f)
                    drawRoundRect(
                        color = ledgerColors.chartRemaining,
                        topLeft = Offset(barLeft, chartHeight - collectedHeight - gap - drawRemHeight),
                        size = Size(barWidth, drawRemHeight),
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }

        // Labels Row aligned with visual slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buckets.forEachIndexed { index, bucket ->
                val isSelected = index == selectedIndex
                Text(
                    text = bucket.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (isSelected) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.labelSmall
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun PaymentStatusDonutChart(
    paidCount: Int,
    partialCount: Int,
    unpaidCount: Int,
    collectionRate: Float,
    modifier: Modifier = Modifier,
) {
    val ledgerColors = LocalLedgerColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val totalCount = (paidCount + partialCount + unpaidCount).coerceAtLeast(1)

    val animProgress = remember(paidCount, partialCount, unpaidCount) { Animatable(0f) }
    LaunchedEffect(paidCount, partialCount, unpaidCount) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    val segments = listOf(
        Triple(paidCount, ledgerColors.paid, stringResource(R.string.filter_paid)),
        Triple(partialCount, ledgerColors.partial, stringResource(R.string.filter_partial)),
        Triple(unpaidCount, ledgerColors.unpaid, stringResource(R.string.filter_unpaid)),
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(124.dp)) {
                val strokeWidth = 18.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )

                var startAngle = -90f
                val gapDegrees = if (segments.count { it.first > 0 } > 1) 4f else 0f
                segments.forEach { (count, color, _) ->
                    if (count > 0) {
                        val sweep = (count.toFloat() / totalCount.toFloat()) * 360f * animProgress.value
                        val drawnSweep = (sweep - gapDegrees).coerceAtLeast(1f)
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = drawnSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = LedgerFormatters.formatPercent(collectionRate),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.collection_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            segments.forEach { (count, color, label) ->
                val pct = (count.toFloat() / totalCount.toFloat())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "$count (${LedgerFormatters.formatPercent(pct)})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
