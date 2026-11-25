package com.example.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CatBillsBar
import com.example.ui.theme.CatBillsBg
import com.example.ui.theme.CatBillsFg
import com.example.ui.theme.CatFoodBar
import com.example.ui.theme.CatFoodBg
import com.example.ui.theme.CatFoodFg
import com.example.ui.theme.CatGroceriesBar
import com.example.ui.theme.CatGroceriesBg
import com.example.ui.theme.CatGroceriesFg
import com.example.ui.theme.CatHealthBar
import com.example.ui.theme.CatHealthBg
import com.example.ui.theme.CatHealthFg
import com.example.ui.theme.CatOtherBar
import com.example.ui.theme.CatOtherBg
import com.example.ui.theme.CatOtherFg
import com.example.ui.theme.CatShoppingBar
import com.example.ui.theme.CatShoppingBg
import com.example.ui.theme.CatShoppingFg
import com.example.ui.theme.CatTransportBar
import com.example.ui.theme.CatTransportBg
import com.example.ui.theme.CatTransportFg
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Keep
@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dailyBudget: Double = 1000.0,
    val currencySymbol: String = "৳",
    val defaultsSeeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val icon: String = "💧",
    val completedDates: List<String> = emptyList(),
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "Food",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class ExpenseCategory(
    val id: String,
    val displayLabel: String,
    val emoji: String,
    val badgeBg: Color,
    val badgeFg: Color,
    val barColor: Color,
) {
    FOOD("Food", "Food & Dining", "🍔", CatFoodBg, CatFoodFg, CatFoodBar),
    TRANSPORT("Transport", "Transport", "🚌", CatTransportBg, CatTransportFg, CatTransportBar),
    GROCERIES("Groceries", "Groceries", "🛒", CatGroceriesBg, CatGroceriesFg, CatGroceriesBar),
    BILLS("Bills", "Bills & Utilities", "💡", CatBillsBg, CatBillsFg, CatBillsBar),
    SHOPPING("Shopping", "Shopping", "🛍️", CatShoppingBg, CatShoppingFg, CatShoppingBar),
    HEALTH("Health", "Health", "🩺", CatHealthBg, CatHealthFg, CatHealthBar),
    OTHER("Other", "Other", "📦", CatOtherBg, CatOtherFg, CatOtherBar);

    companion object {
        fun fromId(raw: String?): ExpenseCategory {
            return entries.firstOrNull { it.id == raw } ?: OTHER
        }
    }
}

val HABIT_ICON_CHOICES = listOf("💧", "📚", "🏃", "🧘", "💊", "✍️", "🥗", "⭐")

data class DailyFlowBackup(
    val dailyBudget: Double,
    val habits: List<BackupHabit>,
    val expenses: List<BackupExpense>,
)

data class BackupHabit(
    val name: String,
    val icon: String,
    val completedDates: List<String>,
)

data class BackupExpense(
    val date: String,
    val title: String,
    val amount: Double,
    val category: String,
)

object DateKeyUtils {
    private val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    fun isValidDateKey(key: String): Boolean = DATE_KEY_REGEX.matches(key)

    fun todayKey(): String = toDateKey(Date())

    fun toDateKey(date: Date): String {
        val cal = Calendar.getInstance().apply { time = date }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun parseDateKey(key: String): Calendar {
        val parts = key.split("-")
        val cal = Calendar.getInstance()
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
            val d = parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
            cal.set(y, m, d, 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return cal
    }

    fun shiftDate(key: String, days: Int): String {
        val cal = parseDateKey(key)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return toDateKey(cal.time)
    }

    fun last7Days(anchorDateKey: String): List<String> {
        return (6 downTo 0).map { offset -> shiftDate(anchorDateKey, -offset) }
    }

    fun formatPrimaryDateLabel(selectedKey: String): String {
        val today = todayKey()
        val yesterday = shiftDate(today, -1)
        val tomorrow = shiftDate(today, 1)
        return when (selectedKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            tomorrow -> "Tomorrow"
            else -> {
                val fmt = SimpleDateFormat("EEEE", Locale.US)
                fmt.format(parseDateKey(selectedKey).time)
            }
        }
    }

    fun formatSubDateLabel(selectedKey: String): String {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return fmt.format(parseDateKey(selectedKey).time)
    }

    fun shortWeekday(dateKey: String): String {
        val fmt = SimpleDateFormat("EEE", Locale.US)
        return fmt.format(parseDateKey(dateKey).time).take(2)
    }

    fun computeStreak(habit: Habit, referenceDateKey: String): Int {
        val completedSet = habit.completedDates.toSet()
        var streak = 0
        var cursor = referenceDateKey
        if (cursor !in completedSet) {
            cursor = shiftDate(cursor, -1)
        }
        while (cursor in completedSet) {
            streak += 1
            cursor = shiftDate(cursor, -1)
        }
        return streak
    }

    fun formatCurrency(amount: Double, symbol: String = "৳"): String {
        val safeValue = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
        val hasDecimals = abs(safeValue % 1.0) > 0.001
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = if (hasDecimals) {
            DecimalFormat("#,##0.00", symbols)
        } else {
            DecimalFormat("#,##0", symbols)
        }
        return "$symbol${formatter.format(safeValue)}"
    }

    fun formatCompactAmount(amount: Double): String {
        return when {
            amount <= 0.0 -> ""
            amount >= 1000.0 -> String.format(Locale.US, "%.1fk", amount / 1000.0)
            else -> amount.toInt().toString()
        }
    }
}

object BackupSerializer {
    fun toJson(dailyBudget: Double, habits: List<Habit>, expenses: List<Expense>): String {
        val root = JSONObject()
        root.put("dailyBudget", dailyBudget)

        val habitsArr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("name", h.name)
            obj.put("icon", h.icon)
            val datesArr = JSONArray()
            h.completedDates.forEach { datesArr.put(it) }
            obj.put("completedDates", datesArr)
            habitsArr.put(obj)
        }
        root.put("habits", habitsArr)

        val expensesArr = JSONArray()
        expenses.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("date", e.date)
            obj.put("title", e.title)
            obj.put("amount", e.amount)
            obj.put("category", e.category)
            expensesArr.put(obj)
        }
        root.put("expenses", expensesArr)
        return root.toString(2)
    }

    fun fromJson(rawJson: String): Result<DailyFlowBackup> {
        return try {
            val root = JSONObject(rawJson)
            if (!root.has("habits") || !root.has("expenses")) {
                return Result.failure(IllegalArgumentException("Invalid backup file format"))
            }
            val budget = root.optDouble("dailyBudget", 1000.0)
                .takeIf { !it.isNaN() && it >= 0.0 && it <= 100_000_000.0 } ?: 1000.0

            val habitsArr = root.getJSONArray("habits")
            val habits = mutableListOf<BackupHabit>()
            for (i in 0 until habitsArr.length()) {
                val obj = habitsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name", "").trim().take(60)
                if (name.isEmpty()) continue
                val icon = obj.optString("icon", "⭐").trim().take(16).ifEmpty { "⭐" }
                val datesJson = obj.optJSONArray("completedDates")
                val dates = mutableListOf<String>()
                if (datesJson != null) {
                    for (j in 0 until minOf(datesJson.length(), 1500)) {
                        val d = datesJson.optString(j, "").trim()
                        if (DateKeyUtils.isValidDateKey(d)) {
                            dates.add(d)
                        }
                    }
                }
                habits.add(BackupHabit(name = name, icon = icon, completedDates = dates.distinct()))
            }

            val expensesArr = root.getJSONArray("expenses")
            val expenses = mutableListOf<BackupExpense>()
            for (i in 0 until expensesArr.length()) {
                val obj = expensesArr.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim().take(60)
                val date = obj.optString("date", "").trim()
                val amount = obj.optDouble("amount", -1.0)
                val category = ExpenseCategory.fromId(obj.optString("category", "Other")).id
                if (title.isNotEmpty() && DateKeyUtils.isValidDateKey(date) && !amount.isNaN() && amount > 0.0 && amount <= 100_000_000.0) {
                    expenses.add(
                        BackupExpense(
                            date = date,
                            title = title,
                            amount = amount,
                            category = category,
                        )
                    )
                }
            }

            Result.success(DailyFlowBackup(dailyBudget = budget, habits = habits, expenses = expenses))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
