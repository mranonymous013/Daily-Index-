package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DateKeyUtils
import com.example.ui.theme.AmberBarLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseBarLight
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealVibrant
import kotlin.math.roundToInt

@Composable
fun DailyFlowHeader(
    selectedDate: String,
    doneHabits: Int,
    totalHabits: Int,
    daySpend: Double,
    dayExpenseCount: Int,
    dailyBudget: Double,
    currencySymbol: String,
    userEmail: String?,
    isWideScreen: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habitFraction = if (totalHabits > 0) doneHabits.toFloat() / totalHabits.toFloat() else 0f
    val habitPct = (habitFraction * 100f).roundToInt()
    val animatedHabitBar by animateFloatAsState(
        targetValue = habitFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f),
        label = "habit_progress",
    )

    val isOverBudget = dailyBudget > 0.0 && daySpend > dailyBudget
    val budgetFraction = if (dailyBudget > 0.0) {
        (daySpend / dailyBudget).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val budgetPct = (budgetFraction * 100f).roundToInt()
    val animatedBudgetBar by animateFloatAsState(
        targetValue = budgetFraction,
        animationSpec = spring(stiffness = 380f),
        label = "budget_progress",
    )

    val budgetStatusText = when {
        dailyBudget <= 0.0 -> "$dayExpenseCount item${if (dayExpenseCount == 1) "" else "s"}"
        isOverBudget -> stringResource(R.string.kpi_over_budget)
        else -> "$budgetPct% of ${DateKeyUtils.formatCurrency(dailyBudget, currencySymbol)}"
    }

    val headerShape = if (isWideScreen) {
        RoundedCornerShape(28.dp)
    } else {
        RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(headerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(TealPrimary, TealVibrant, TealDeep)
                )
            )
            .then(if (!isWideScreen) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Brand Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                        Text(
                            text = userEmail ?: stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        onClick = onTodayClick,
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.testTag("today_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.action_today),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }

                    IconButton(
                        onClick = onSignOutClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = stringResource(R.string.action_sign_out),
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Date Switcher Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevDay,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_day_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_previous_day),
                        tint = Color.White,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateKeyUtils.formatPrimaryDateLabel(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.testTag("selected_date_label"),
                    )
                    Text(
                        text = DateKeyUtils.formatSubDateLabel(selectedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.80f),
                    )
                }

                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_day_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_day),
                        tint = Color.White,
                    )
                }
            }

            // Quick KPI Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeaderKpiCard(
                    label = stringResource(R.string.kpi_habits_done),
                    badge = "$habitPct%",
                    value = "$doneHabits / $totalHabits",
                    progress = animatedHabitBar,
                    barColor = Color(0xFF6EE7B7),
                    modifier = Modifier.weight(1f),
                )
                HeaderKpiCard(
                    label = stringResource(R.string.kpi_days_spend),
                    badge = budgetStatusText,
                    value = DateKeyUtils.formatCurrency(daySpend, currencySymbol),
                    progress = animatedBudgetBar,
                    barColor = if (isOverBudget) RoseBarLight else AmberBarLight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeaderKpiCard(
    label: String,
    badge: String,
    value: String,
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}
