package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DailyFlowRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchingUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun userDocRef(userId: String) = db.collection("users").document(userId)

    private fun habitsColRef(userId: String) = userDocRef(userId).collection("habits")

    private fun expensesColRef(userId: String) = userDocRef(userId).collection("expenses")

    fun observeSettings(userId: String): Flow<UserSettings> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid"
        emitAll(
            userDocRef(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: UserSettings(userId = uid, dailyBudget = 1000.0, currencySymbol = "৳")
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeHabits(userId: String): Flow<List<Habit>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid/habits"
        emitAll(
            habitsColRef(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Habit::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareBy<Habit> { it.sortOrder }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeExpenses(userId: String): Flow<List<Expense>> = flow {
        val uid = requireMatchingUserId(userId)
        val path = "users/$uid/expenses"
        emitAll(
            expensesColRef(uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Expense::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserInitialized(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            val snapshot = userDocRef(uid).get().await()
            if (!snapshot.exists()) {
                val batch = db.batch()
                batch.set(
                    userDocRef(uid),
                    mapOf(
                        "userId" to uid,
                        "dailyBudget" to 1000.0,
                        "currencySymbol" to "৳",
                        "defaultsSeeded" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                val starterHabits = listOf(
                    Triple("Drink 8 glasses of water", "💧", 1L),
                    Triple("Read for 20 minutes", "📚", 2L),
                    Triple("Walk 30 minutes", "🏃", 3L),
                )
                starterHabits.forEach { (name, icon, order) ->
                    val habitRef = habitsColRef(uid).document()
                    batch.set(
                        habitRef,
                        mapOf(
                            "userId" to uid,
                            "name" to name,
                            "icon" to icon,
                            "completedDates" to emptyList<String>(),
                            "sortOrder" to order,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addHabit(userId: String, rawName: String, rawIcon: String): Result<String> {
        var docPath = "users/$userId/habits"
        return try {
            val uid = requireMatchingUserId(userId)
            val name = rawName.trim()
            require(name.length in 1..60) { "Habit name must be between 1 and 60 characters." }
            val icon = rawIcon.trim().take(16).ifEmpty { "⭐" }
            val docRef = habitsColRef(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to name,
                "icon" to icon,
                "completedDates" to emptyList<String>(),
                "sortOrder" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun restoreHabit(userId: String, habit: Habit): Result<Unit> {
        var docPath = "users/$userId/habits"
        return try {
            val uid = requireMatchingUserId(userId)
            val name = habit.name.trim().take(60)
            require(name.isNotEmpty()) { "Habit name cannot be blank." }
            val icon = habit.icon.trim().take(16).ifEmpty { "⭐" }
            val safeDates = habit.completedDates.filter { DateKeyUtils.isValidDateKey(it) }.distinct().take(1500)
            val docRef = if (habit.id.isNotBlank() && habit.id.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                habitsColRef(uid).document(habit.id)
            } else {
                habitsColRef(uid).document()
            }
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to name,
                "icon" to icon,
                "completedDates" to safeDates,
                "sortOrder" to habit.sortOrder.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitDate(userId: String, habit: Habit, dateKey: String): Result<Unit> {
        val docPath = "users/$userId/habits/${habit.id}"
        return try {
            val uid = requireMatchingUserId(userId)
            require(habit.id.isNotBlank()) { "Habit ID must not be blank." }
            require(DateKeyUtils.isValidDateKey(dateKey)) { "Invalid date key." }
            val updatedDates = habit.completedDates.toMutableList()
            if (dateKey in updatedDates) {
                updatedDates.removeAll { it == dateKey }
            } else {
                if (updatedDates.size >= 1500) {
                    updatedDates.removeAt(0)
                }
                updatedDates.add(dateKey)
            }
            habitsColRef(uid).document(habit.id).update(
                mapOf(
                    "completedDates" to updatedDates,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(userId: String, habitId: String): Result<Unit> {
        val docPath = "users/$userId/habits/$habitId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(habitId.isNotBlank()) { "Habit ID must not be blank." }
            habitsColRef(uid).document(habitId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        userId: String,
        dateKey: String,
        rawTitle: String,
        amount: Double,
        category: ExpenseCategory,
    ): Result<String> {
        var docPath = "users/$userId/expenses"
        return try {
            val uid = requireMatchingUserId(userId)
            val title = rawTitle.trim()
            require(title.length in 1..60) { "Expense title must be between 1 and 60 characters." }
            require(DateKeyUtils.isValidDateKey(dateKey)) { "Invalid expense date." }
            require(!amount.isNaN() && amount > 0.0 && amount <= 100_000_000.0) {
                "Amount must be greater than 0."
            }
            val docRef = expensesColRef(uid).document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "date" to dateKey,
                "title" to title,
                "amount" to amount,
                "category" to category.id,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun restoreExpense(userId: String, expense: Expense): Result<Unit> {
        var docPath = "users/$userId/expenses"
        return try {
            val uid = requireMatchingUserId(userId)
            val title = expense.title.trim().take(60)
            require(title.isNotEmpty()) { "Expense title cannot be blank." }
            require(DateKeyUtils.isValidDateKey(expense.date)) { "Invalid date key." }
            require(expense.amount > 0.0 && expense.amount <= 100_000_000.0) { "Invalid amount." }
            val safeCategory = ExpenseCategory.fromId(expense.category).id
            val docRef = if (expense.id.isNotBlank() && expense.id.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) {
                expensesColRef(uid).document(expense.id)
            } else {
                expensesColRef(uid).document()
            }
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "date" to expense.date,
                "title" to title,
                "amount" to expense.amount,
                "category" to safeCategory,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        val docPath = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(expenseId.isNotBlank()) { "Expense ID must not be blank." }
            expensesColRef(uid).document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDailyBudget(userId: String, dailyBudget: Double): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            require(!dailyBudget.isNaN() && dailyBudget >= 0.0 && dailyBudget <= 100_000_000.0) {
                "Budget must be 0 or greater."
            }
            val docRef = userDocRef(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "dailyBudget" to dailyBudget,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dailyBudget" to dailyBudget,
                        "currencySymbol" to "৳",
                        "defaultsSeeded" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearExpensesAndHistory(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            val expensesSnap = expensesColRef(uid).get().await()
            val habitsSnap = habitsColRef(uid).get().await()

            val operations = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()
            expensesSnap.documents.forEach { doc ->
                operations.add { batch -> batch.delete(doc.reference) }
            }
            habitsSnap.documents.forEach { doc ->
                operations.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "completedDates" to emptyList<String>(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
            operations.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { op -> op(batch) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun importBackup(userId: String, backup: DailyFlowBackup): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireMatchingUserId(userId)
            val existingHabits = habitsColRef(uid).get().await()
            val existingExpenses = expensesColRef(uid).get().await()
            val settingsSnap = userDocRef(uid).get().await()

            val operations = mutableListOf<(com.google.firebase.firestore.WriteBatch) -> Unit>()

            if (settingsSnap.exists()) {
                operations.add { batch ->
                    batch.update(
                        userDocRef(uid),
                        mapOf(
                            "dailyBudget" to backup.dailyBudget,
                            "defaultsSeeded" to true,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            } else {
                operations.add { batch ->
                    batch.set(
                        userDocRef(uid),
                        mapOf(
                            "userId" to uid,
                            "dailyBudget" to backup.dailyBudget,
                            "currencySymbol" to "৳",
                            "defaultsSeeded" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }

            existingHabits.documents.forEach { doc ->
                operations.add { batch -> batch.delete(doc.reference) }
            }
            existingExpenses.documents.forEach { doc ->
                operations.add { batch -> batch.delete(doc.reference) }
            }

            backup.habits.forEachIndexed { idx, habit ->
                val ref = habitsColRef(uid).document()
                operations.add { batch ->
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "name" to habit.name,
                            "icon" to habit.icon,
                            "completedDates" to habit.completedDates,
                            "sortOrder" to idx.toLong(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }

            backup.expenses.forEach { exp ->
                val ref = expensesColRef(uid).document()
                operations.add { batch ->
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "date" to exp.date,
                            "title" to exp.title,
                            "amount" to exp.amount,
                            "category" to exp.category,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }

            operations.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { op -> op(batch) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
