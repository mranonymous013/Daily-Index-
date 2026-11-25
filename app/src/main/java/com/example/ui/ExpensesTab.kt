package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DateKeyUtils
import com.example.data.Expense
import com.example.data.ExpenseCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesTabContent(
    expenses: List<Expense>,
    selectedDate: String,
    selectedCategoryFilter: String,
    currencySymbol: String,
    onSelectCategoryFilter: (String) -> Unit,
    onAddExpense: (title: String, amountText: String, category: ExpenseCategory) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    var titleInput by rememberSaveable { mutableStateOf("") }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(ExpenseCategory.FOOD.id) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val selectedCategory = ExpenseCategory.fromId(selectedCategoryId)

    fun submitExpense() {
        val trimmedTitle = titleInput.trim()
        val parsedAmount = amountInput.trim().toDoubleOrNull()
        onAddExpense(trimmedTitle, amountInput, selectedCategory)
        if (trimmedTitle.isNotEmpty() && parsedAmount != null && parsedAmount > 0.0) {
            titleInput = ""
            amountInput = ""
        }
    }

    val dayExpenses = remember(expenses, selectedDate, selectedCategoryFilter) {
        expenses
            .filter { it.date == selectedDate }
            .filter { selectedCategoryFilter == "All" || it.category == selectedCategoryFilter }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Add Expense Form Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { if (it.length <= 60) titleInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.expense_title_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("expense_title_input"),
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 12) {
                                amountInput = filtered
                            }
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.expense_amount_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submitExpense() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_amount_input"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.weight(1f),
                    ) {
                        OutlinedTextField(
                            value = "${selectedCategory.emoji} ${selectedCategory.displayLabel}",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("expense_category_dropdown"),
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                        ) {
                            ExpenseCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.emoji} ${cat.displayLabel}") },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { submitExpense() },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("add_expense_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.action_add),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val filterOptions = listOf("All") + ExpenseCategory.entries.map { it.id }
            filterOptions.forEach { catId ->
                val selected = selectedCategoryFilter == catId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategoryFilter(catId) },
                    label = {
                        Text(
                            text = catId,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.testTag("filter_chip_$catId"),
                )
            }
        }

        // Expense List or Empty State
        if (dayExpenses.isEmpty()) {
            EmptyStateCard(
                emoji = "🧾",
                title = stringResource(R.string.expense_empty_title),
                subtitle = if (selectedCategoryFilter == "All") {
                    stringResource(R.string.expense_empty_all)
                } else {
                    stringResource(R.string.expense_empty_category, selectedCategoryFilter)
                },
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                dayExpenses.forEach { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        currencySymbol = currencySymbol,
                        onDelete = { onDeleteExpense(expense) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRowCard(
    expense: Expense,
    currencySymbol: String,
    onDelete: () -> Unit,
) {
    val category = ExpenseCategory.fromId(expense.category)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(category.badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = category.emoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = category.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = DateKeyUtils.formatCurrency(expense.amount, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("expense_delete_${expense.id}"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_delete_expense, expense.title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
