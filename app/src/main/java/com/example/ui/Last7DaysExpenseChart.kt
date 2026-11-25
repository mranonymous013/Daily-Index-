package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DateKeyUtils
import com.example.data.Expense
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.RoseBarLight
import com.example.ui.theme.RoseError
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealVibrant
import kotlin.math.ceil
import kotlin.math.max

data class DailyExpensePoint(
    val dateKey: String,
    val weekdayShort: String,
    val fullDateLabel: String,
    val totalAmount: Double,
    val itemCount: Int,
)

@Composable
fun Last7DaysExpenseChartCard(
    expenses: List<Expense>,
    selectedDate: String,
    dailyBudget: Double,
    currencySymbol: String,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val points = remember(expenses, selectedDate) {
        val last7 = DateKeyUtils.last7Days(selectedDate)
        last7.map { dateKey ->
            val dayList = expenses.filter { it.date == dateKey }
            DailyExpensePoint(
                dateKey = dateKey,
                weekdayShort = DateKeyUtils.shortWeekday(dateKey),
                fullDateLabel = "${DateKeyUtils.formatPrimaryDateLabel(dateKey)}, ${DateKeyUtils.formatSubDateLabel(dateKey)}",
                totalAmount = dayList.sumOf { it.amount },
                itemCount = dayList.size,
            )
        }
    }

    var inspectedDateKey by remember(selectedDate) { mutableStateOf(selectedDate) }
    val inspectedPoint = points.firstOrNull { it.dateKey == inspectedDateKey } ?: points.last()

    val weekSum = remember(points) { points.sumOf { it.totalAmount } }
    val dailyAvg = remember(weekSum) { weekSum / 7.0 }
    val yAxisMax = remember(points, dailyBudget) {
        val rawMax = max(
            points.maxOfOrNull { it.totalAmount } ?: 0.0,
            if (dailyBudget > 0.0) dailyBudget else 100.0,
        ).coerceAtLeast(100.0)
        niceUpperBound(rawMax)
    }

    val animatedFractions = points.map { point ->
        val target = (point.totalAmount / yAxisMax).toFloat().coerceIn(0f, 1f)
        val animated by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
            label = "bar_${point.dateKey}",
        )
        animated
    }

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedAxisLabelColor = MaterialTheme.colorScheme.primary
    val cursorHighlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    val zeroBarColor = MaterialTheme.colorScheme.surfaceVariant
    val normalBarTop = TealVibrant.copy(alpha = 0.72f)
    val normalBarBottom = TealPrimary.copy(alpha = 0.42f)
    val selectedBarTop = TealVibrant
    val selectedBarBottom = TealPrimary
    val overBudgetBarTop = RoseBarLight
    val overBudgetBarBottom = RoseError

    val textMeasurer = rememberTextMeasurer()
    val axisTextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = axisLabelColor,
    )
    val selectedAxisTextStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = selectedAxisLabelColor,
    )
    val valueTextStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = axisLabelColor,
    )
    val budgetTagTextStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = AmberAccent,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("last_7_days_expense_chart_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Card Title & Budget Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.summary_7day_spending),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Total: ${DateKeyUtils.formatCurrency(weekSum, currencySymbol)}  •  Avg: ${DateKeyUtils.formatCurrency(dailyAvg, currencySymbol)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = if (dailyBudget > 0.0) {
                            "Budget: ${DateKeyUtils.formatCurrency(dailyBudget, currencySymbol)}"
                        } else {
                            stringResource(R.string.summary_no_daily_limit)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            // Interactive Recharts-style Tooltip Callout
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_chart_tooltip"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                AnimatedContent(
                    targetState = inspectedPoint,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "chart_tooltip_transition",
                ) { point ->
                    val isOver = dailyBudget > 0.0 && point.totalAmount > dailyBudget
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = point.fullDateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = DateKeyUtils.formatCurrency(point.totalAmount, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "• ${point.itemCount} item${if (point.itemCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (point.dateKey != selectedDate) {
                            TextButton(
                                onClick = { onSelectDate(point.dateKey) },
                                modifier = Modifier.testTag("chart_jump_to_day_button"),
                            ) {
                                Text(
                                    text = stringResource(R.string.chart_open_day),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // Native Jetpack Compose Canvas Bar Chart with Cartesian Grid, Y-Axis & Budget Line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val leftGutterPx = 42.dp.toPx()
                            val rightPadPx = 6.dp.toPx()
                            val plotWidth = (size.width - leftGutterPx - rightPadPx).coerceAtLeast(1f)
                            val relativeX = (tapOffset.x - leftGutterPx).coerceIn(0f, plotWidth - 1f)
                            val colWidth = plotWidth / points.size
                            val tappedIndex = (relativeX / colWidth).toInt().coerceIn(0, points.lastIndex)
                            inspectedDateKey = points[tappedIndex].dateKey
                        }
                    }
                    .testTag("last_7_days_bar_chart_canvas"),
            ) {
                val leftGutter = 42.dp.toPx()
                val rightPad = 6.dp.toPx()
                val topPad = 18.dp.toPx()
                val bottomPad = 26.dp.toPx()
                val plotWidth = (size.width - leftGutter - rightPad).coerceAtLeast(1f)
                val plotHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val baselineY = topPad + plotHeight

                val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

                // 1. Horizontal Cartesian gridlines + Y-axis labels (0, 33%, 66%, 100%)
                val tickSteps = 3
                for (step in 0..tickSteps) {
                    val fraction = step.toFloat() / tickSteps.toFloat()
                    val y = baselineY - (plotHeight * fraction)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(leftGutter, y),
                        end = Offset(leftGutter + plotWidth, y),
                        strokeWidth = if (step == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (step == 0) null else dashedEffect,
                    )

                    val tickValue = yAxisMax * fraction
                    val labelText = if (step == 0) "0" else DateKeyUtils.formatCompactAmount(tickValue)
                    val measured = textMeasurer.measure(labelText, style = axisTextStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = (leftGutter - measured.size.width - 8.dp.toPx()).coerceAtLeast(0f),
                            y = y - measured.size.height / 2f,
                        ),
                    )
                }

                // 2. Daily Budget Reference Line (Recharts ReferenceLine equivalent)
                if (dailyBudget > 0.0 && dailyBudget <= yAxisMax) {
                    val budgetFraction = (dailyBudget / yAxisMax).toFloat().coerceIn(0f, 1f)
                    val budgetY = baselineY - (plotHeight * budgetFraction)
                    drawLine(
                        color = AmberAccent.copy(alpha = 0.85f),
                        start = Offset(leftGutter, budgetY),
                        end = Offset(leftGutter + plotWidth, budgetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f),
                    )
                    val budgetTag = textMeasurer.measure("Budget", style = budgetTagTextStyle)
                    drawText(
                        textLayoutResult = budgetTag,
                        topLeft = Offset(
                            x = leftGutter + plotWidth - budgetTag.size.width,
                            y = (budgetY - budgetTag.size.height - 2.dp.toPx()).coerceAtLeast(0f),
                        ),
                    )
                }

                // 3. Bars, Recharts-style hover/tap cursor backdrop, value labels & X-axis ticks
                val slotWidth = plotWidth / points.size
                val barWidth = (slotWidth * 0.56f).coerceIn(14.dp.toPx(), 36.dp.toPx())
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                points.forEachIndexed { idx, point ->
                    val slotLeft = leftGutter + idx * slotWidth
                    val centerX = slotLeft + slotWidth / 2f
                    val isInspected = point.dateKey == inspectedDateKey
                    val isOver = dailyBudget > 0.0 && point.totalAmount > dailyBudget

                    // Active column cursor backdrop
                    if (isInspected) {
                        drawRoundRect(
                            color = cursorHighlightColor,
                            topLeft = Offset(slotLeft + 2.dp.toPx(), topPad - 4.dp.toPx()),
                            size = Size(slotWidth - 4.dp.toPx(), plotHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                        )
                    }

                    val fraction = animatedFractions[idx]
                    val minZeroHeight = 4.dp.toPx()
                    val barHeight = if (point.totalAmount > 0.0) {
                        (plotHeight * fraction).coerceAtLeast(8.dp.toPx())
                    } else {
                        minZeroHeight
                    }
                    val barTop = baselineY - barHeight
                    val barLeft = centerX - barWidth / 2f

                    if (point.totalAmount <= 0.0) {
                        drawRoundRect(
                            color = zeroBarColor,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                    } else {
                        val barBrush = when {
                            isOver -> Brush.verticalGradient(
                                colors = listOf(overBudgetBarTop, overBudgetBarBottom),
                                startY = barTop,
                                endY = baselineY,
                            )
                            isInspected -> Brush.verticalGradient(
                                colors = listOf(selectedBarTop, selectedBarBottom),
                                startY = barTop,
                                endY = baselineY,
                            )
                            else -> Brush.verticalGradient(
                                colors = listOf(normalBarTop, normalBarBottom),
                                startY = barTop,
                                endY = baselineY,
                            )
                        }
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius,
                        )

                        // Compact value label above bar
                        val compactVal = DateKeyUtils.formatCompactAmount(point.totalAmount)
                        if (compactVal.isNotEmpty()) {
                            val valMeasured = textMeasurer.measure(compactVal, style = valueTextStyle)
                            drawText(
                                textLayoutResult = valMeasured,
                                topLeft = Offset(
                                    x = centerX - valMeasured.size.width / 2f,
                                    y = (barTop - valMeasured.size.height - 3.dp.toPx()).coerceAtLeast(0f),
                                ),
                            )
                        }
                    }

                    // X-axis weekday label
                    val dayMeasured = textMeasurer.measure(
                        text = point.weekdayShort,
                        style = if (isInspected) selectedAxisTextStyle else axisTextStyle,
                    )
                    drawText(
                        textLayoutResult = dayMeasured,
                        topLeft = Offset(
                            x = centerX - dayMeasured.size.width / 2f,
                            y = baselineY + 8.dp.toPx(),
                        ),
                    )
                }
            }

            // Compact Legend Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendItem(
                    color = TealPrimary,
                    label = stringResource(R.string.chart_legend_selected),
                )
                ChartLegendItem(
                    color = RoseError,
                    label = stringResource(R.string.chart_legend_over_budget),
                )
                if (dailyBudget > 0.0) {
                    ChartLegendItem(
                        color = AmberAccent,
                        label = stringResource(R.string.chart_legend_budget_line),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun niceUpperBound(rawMax: Double): Double {
    if (rawMax <= 100.0) return 100.0
    val step = when {
        rawMax <= 500.0 -> 100.0
        rawMax <= 2000.0 -> 250.0
        rawMax <= 5000.0 -> 500.0
        rawMax <= 20000.0 -> 2000.0
        else -> 5000.0
    }
    return ceil(rawMax / step) * step
}
