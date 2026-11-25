package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DateKeyUtils
import com.example.data.HABIT_ICON_CHOICES
import com.example.data.Habit
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldBorder
import com.example.ui.theme.EmeraldSoftBg

@Composable
fun HabitsTabContent(
    habits: List<Habit>,
    selectedDate: String,
    onAddHabit: (name: String, icon: String) -> Unit,
    onToggleHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var habitInput by rememberSaveable { mutableStateOf("") }
    var selectedIcon by rememberSaveable { mutableStateOf(HABIT_ICON_CHOICES.first()) }

    fun submitHabit() {
        val trimmed = habitInput.trim()
        if (trimmed.isNotEmpty()) {
            onAddHabit(trimmed, selectedIcon)
            habitInput = ""
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Add Habit Card
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = habitInput,
                        onValueChange = { if (it.length <= 60) habitInput = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.habit_input_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitHabit() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("habit_name_input"),
                    )

                    Button(
                        onClick = { submitHabit() },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("add_habit_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.action_add),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                // Icon Picker Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.label_icon),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    HABIT_ICON_CHOICES.forEach { emoji ->
                        val isSelected = selectedIcon == emoji
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedIcon = emoji }
                                .testTag("habit_icon_$emoji"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // Habit List or Empty State
        if (habits.isEmpty()) {
            EmptyStateCard(
                emoji = "🌱",
                title = stringResource(R.string.habit_empty_title),
                subtitle = stringResource(R.string.habit_empty_subtitle),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                habits.forEach { habit ->
                    HabitRowCard(
                        habit = habit,
                        selectedDate = selectedDate,
                        onToggle = { onToggleHabit(habit) },
                        onDelete = { onDeleteHabit(habit) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitRowCard(
    habit: Habit,
    selectedDate: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDone = selectedDate in habit.completedDates
    val streak = DateKeyUtils.computeStreak(habit, selectedDate)

    val containerColor by animateColorAsState(
        targetValue = if (isDone) EmeraldSoftBg else MaterialTheme.colorScheme.surface,
        label = "habit_card_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDone) EmeraldBorder else MaterialTheme.colorScheme.outlineVariant,
        label = "habit_card_border",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "habit_check_scale",
    )

    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isDone) 0.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Check circle inside a 48dp minimum touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle)
                    .testTag("habit_toggle_${habit.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(if (isDone) EmeraldAccent else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (isDone) EmeraldAccent else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.cd_toggle_habit, habit.name),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = habit.icon.ifEmpty { "⭐" }, fontSize = 18.sp)
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDone) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (streak > 0) "🔥 $streak day streak" else stringResource(R.string.habit_tap_to_complete),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (streak > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (streak > 0) FontWeight.Medium else FontWeight.Normal,
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("habit_delete_${habit.id}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_habit, habit.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = emoji, fontSize = 34.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
