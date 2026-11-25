package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateKeyUtils
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.Habit
import com.example.ui.theme.EmeraldAccent
import kotlin.math.roundToInt

@Composable
fun SummaryTabContent(
    habits: List<Habit>,
    expenses: List<Expense>,
    selectedDate: String,
    dailyBudget: Double,
    currencySymbol: String,
    isWideScreen: Boolean,
    isBusy: Boolean,
    onSelectDate: (String) -> Unit,
    onSaveDailyBudget: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRequestClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val last7Dates = remember(selectedDate) { DateKeyUtils.last7Days(selectedDate) }
    val weekSum = remember(expenses, last7Dates) {
        val last7Set = last7Dates.toSet()
        expenses.filter { it.date in last7Set }.sumOf { it.amount }
    }

    val categoryBreakdown = remember(expenses, last7Dates) {
        val last7Set = last7Dates.toSet()
        expenses
            .filter { it.date in last7Set }
            .groupBy { ExpenseCategory.fromId(it.category) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
    }

    if (isWideScreen) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Last7DaysExpenseChartCard(
                    expenses = expenses,
                    selectedDate = selectedDate,
                    dailyBudget = dailyBudget,
                    currencySymbol = currencySymbol,
                    onSelectDate = onSelectDate,
                    modifier = Modifier.weight(1f),
                )
                CategoryBreakdownCard(
                    categoryBreakdown = categoryBreakdown,
                    weekSum = weekSum,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                HabitConsistencyCard(
                    habits = habits,
                    last7Dates = last7Dates,
                    modifier = Modifier.weight(1f),
                )
                SettingsAndBackupCard(
                    dailyBudget = dailyBudget,
                    isBusy = isBusy,
                    onSaveDailyBudget = onSaveDailyBudget,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    onRequestClearHistory = onRequestClearHistory,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Last7DaysExpenseChartCard(
                expenses = expenses,
                selectedDate = selectedDate,
                dailyBudget = dailyBudget,
                currencySymbol = currencySymbol,
                onSelectDate = onSelectDate,
            )
            CategoryBreakdownCard(
                categoryBreakdown = categoryBreakdown,
                weekSum = weekSum,
                currencySymbol = currencySymbol,
            )
            HabitConsistencyCard(
                habits = habits,
                last7Dates = last7Dates,
            )
            SettingsAndBackupCard(
                dailyBudget = dailyBudget,
                isBusy = isBusy,
                onSaveDailyBudget = onSaveDailyBudget,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onRequestClearHistory = onRequestClearHistory,
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categoryBreakdown: List<Map.Entry<ExpenseCategory, Double>>,
    weekSum: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.summary_category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (categoryBreakdown.isEmpty()) {
                Text(
                    text = stringResource(R.string.summary_category_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                categoryBreakdown.forEach { (category, sum) ->
                    val fraction = if (weekSum > 0.0) (sum / weekSum).toFloat().coerceIn(0f, 1f) else 0f
                    val pct = (fraction * 100f).roundToInt()
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.emoji} ${category.id}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${DateKeyUtils.formatCurrency(sum, currencySymbol)} ($pct%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(category.barColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitConsistencyCard(
    habits: List<Habit>,
    last7Dates: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.summary_habit_consistency),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (habits.isEmpty()) {
                Text(
                    text = stringResource(R.string.summary_habit_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                habits.forEach { habit ->
                    val doneSet = remember(habit.completedDates) { habit.completedDates.toSet() }
                    val doneCount = last7Dates.count { it in doneSet }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${habit.icon.ifEmpty { "⭐" }} ${habit.name}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            last7Dates.forEach { dateKey ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (dateKey in doneSet) EmeraldAccent
                                            else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                )
                            }
                        }

                        Text(
                            text = "$doneCount/7",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(28.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAndBackupCard(
    dailyBudget: Double,
    isBusy: Boolean,
    onSaveDailyBudget: (String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRequestClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var budgetField by remember {
        mutableStateOf(
            if (dailyBudget % 1.0 == 0.0) dailyBudget.toLong().toString() else dailyBudget.toString()
        )
    }

    LaunchedEffect(dailyBudget) {
        budgetField = if (dailyBudget % 1.0 == 0.0) {
            dailyBudget.toLong().toString()
        } else {
            dailyBudget.toString()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.summary_settings_backup),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = budgetField,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1 && filtered.length <= 12) {
                            budgetField = filtered
                        }
                    },
                    label = { Text(stringResource(R.string.summary_daily_budget_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSaveDailyBudget(budgetField) }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("daily_budget_input"),
                )

                Button(
                    onClick = { onSaveDailyBudget(budgetField) },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("save_budget_button"),
                ) {
                    Text(
                        text = stringResource(R.string.action_save),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onExportBackup,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_backup_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_export_backup),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledTonalButton(
                    onClick = onImportBackup,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_backup_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_import_backup),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            TextButton(
                onClick = onRequestClearHistory,
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_history_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_clear_history),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
