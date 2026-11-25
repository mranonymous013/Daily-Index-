package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BackupSerializer
import com.example.data.DailyFlowRepository
import com.example.data.DateKeyUtils
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.Habit
import com.example.data.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DailyFlowTab {
    HABITS,
    EXPENSES,
    SUMMARY,
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DailyFlowSnapshot(
    val settings: UserSettings,
    val habits: List<Habit>,
    val expenses: List<Expense>,
)

data class SnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

class DailyFlowViewModel(
    private val repository: DailyFlowRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateKeyUtils.todayKey())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _activeTab = MutableStateFlow(DailyFlowTab.HABITS)
    val activeTab: StateFlow<DailyFlowTab> = _activeTab.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<SnackbarMessage>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<SnackbarMessage> = _snackbarEvents.asSharedFlow()

    val uiState: StateFlow<UiState<DailyFlowSnapshot>> = combine(
        repository.observeSettings(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeExpenses(currentUserId),
    ) { settings, habits, expenses ->
        UiState.Success(
            DailyFlowSnapshot(
                settings = settings,
                habits = habits,
                expenses = expenses,
            )
        ) as UiState<DailyFlowSnapshot>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing DailyFlow data", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync your DailyFlow data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(currentUserId)
                .onFailure { err ->
                    Log.w(TAG, "Initial user seeding warning", err)
                }
        }
    }

    fun selectTab(tab: DailyFlowTab) {
        _activeTab.value = tab
    }

    fun shiftSelectedDate(days: Int) {
        _selectedDate.value = DateKeyUtils.shiftDate(_selectedDate.value, days)
    }

    fun jumpToDate(dateKey: String) {
        if (DateKeyUtils.isValidDateKey(dateKey)) {
            _selectedDate.value = dateKey
        }
    }

    fun jumpToToday() {
        _selectedDate.value = DateKeyUtils.todayKey()
    }

    fun selectCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun addHabit(name: String, icon: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            emitToast("Enter a habit name")
            return
        }
        if (trimmed.length > 60) {
            emitToast("Habit name must be 60 characters or fewer")
            return
        }
        viewModelScope.launch {
            repository.addHabit(currentUserId, trimmed, icon)
                .onSuccess { emitToast("Habit added") }
                .onFailure { emitToast(it.localizedMessage ?: "Could not add habit") }
        }
    }

    fun toggleHabit(habit: Habit) {
        val dateKey = _selectedDate.value
        viewModelScope.launch {
            repository.toggleHabitDate(currentUserId, habit, dateKey)
                .onFailure { emitToast(it.localizedMessage ?: "Could not update habit") }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(currentUserId, habit.id)
                .onSuccess {
                    emitToast(
                        message = "Deleted \"${habit.name}\"",
                        actionLabel = "Undo",
                        onAction = {
                            viewModelScope.launch {
                                repository.restoreHabit(currentUserId, habit)
                                    .onFailure { emitToast("Could not restore habit") }
                            }
                        },
                    )
                }
                .onFailure { emitToast(it.localizedMessage ?: "Could not delete habit") }
        }
    }

    fun addExpense(title: String, amountText: String, category: ExpenseCategory) {
        val trimmed = title.trim()
        val amount = amountText.trim().toDoubleOrNull()
        if (trimmed.isEmpty() || amount == null || amount <= 0.0) {
            emitToast("Enter an item name and valid amount")
            return
        }
        if (trimmed.length > 60) {
            emitToast("Item name must be 60 characters or fewer")
            return
        }
        val dateKey = _selectedDate.value
        val currencySymbol = (uiState.value as? UiState.Success)?.data?.settings?.currencySymbol ?: "৳"
        viewModelScope.launch {
            repository.addExpense(currentUserId, dateKey, trimmed, amount, category)
                .onSuccess {
                    _selectedCategoryFilter.value = "All"
                    emitToast("Added ${DateKeyUtils.formatCurrency(amount, currencySymbol)}")
                }
                .onFailure { emitToast(it.localizedMessage ?: "Could not add expense") }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(currentUserId, expense.id)
                .onSuccess {
                    emitToast(
                        message = "Removed ${expense.title}",
                        actionLabel = "Undo",
                        onAction = {
                            viewModelScope.launch {
                                repository.restoreExpense(currentUserId, expense)
                                    .onFailure { emitToast("Could not restore expense") }
                            }
                        },
                    )
                }
                .onFailure { emitToast(it.localizedMessage ?: "Could not delete expense") }
        }
    }

    fun updateDailyBudget(budgetText: String) {
        val value = budgetText.trim().toDoubleOrNull()
        if (value == null || value < 0.0) {
            emitToast("Enter a valid daily budget (0 or higher)")
            return
        }
        viewModelScope.launch {
            repository.updateDailyBudget(currentUserId, value)
                .onSuccess { emitToast("Daily budget updated") }
                .onFailure { emitToast(it.localizedMessage ?: "Could not save budget") }
        }
    }

    fun clearAllExpensesAndHistory() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.clearExpensesAndHistory(currentUserId)
                .onSuccess { emitToast("Expenses & history cleared") }
                .onFailure { emitToast(it.localizedMessage ?: "Could not clear data") }
            _isBusy.value = false
        }
    }

    fun buildBackupJson(): String? {
        val snapshot = (uiState.value as? UiState.Success)?.data ?: return null
        return BackupSerializer.toJson(
            dailyBudget = snapshot.settings.dailyBudget,
            habits = snapshot.habits,
            expenses = snapshot.expenses,
        )
    }

    fun importBackupJson(rawJson: String) {
        val parsed = BackupSerializer.fromJson(rawJson)
        parsed.onFailure {
            emitToast("Invalid backup file")
            return
        }
        val backup = parsed.getOrThrow()
        viewModelScope.launch {
            _isBusy.value = true
            repository.importBackup(currentUserId, backup)
                .onSuccess { emitToast("Backup restored") }
                .onFailure { emitToast(it.localizedMessage ?: "Could not restore backup") }
            _isBusy.value = false
        }
    }

    fun showMessage(msg: String) {
        emitToast(msg)
    }

    private fun emitToast(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ) {
        _snackbarEvents.tryEmit(
            SnackbarMessage(
                message = message,
                actionLabel = actionLabel,
                onAction = onAction,
            )
        )
    }

    private companion object {
        const val TAG = "DailyFlowVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
