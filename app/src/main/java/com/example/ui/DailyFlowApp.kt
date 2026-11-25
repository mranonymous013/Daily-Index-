package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.AuthScreen
import com.example.auth.signOutUser
import com.example.data.DailyFlowRepository
import com.example.data.DateKeyUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DailyFlowApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            DailyFlowMainScreen(
                currentUserId = user.uid,
                userEmail = user.email,
                onSignedOut = { currentUser = null },
            )
        }
    }
}

private data class NavDestination(
    val tab: DailyFlowTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(
        tab = DailyFlowTab.HABITS,
        labelRes = R.string.nav_habits,
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        testTag = "nav_tab_habits",
    ),
    NavDestination(
        tab = DailyFlowTab.EXPENSES,
        labelRes = R.string.nav_expenses,
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        testTag = "nav_tab_expenses",
    ),
    NavDestination(
        tab = DailyFlowTab.SUMMARY,
        labelRes = R.string.nav_summary,
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        testTag = "nav_tab_summary",
    ),
)

@Composable
private fun DailyFlowMainScreen(
    currentUserId: String,
    userEmail: String?,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: DailyFlowViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DailyFlowViewModel(
                    repository = DailyFlowRepository(db),
                    currentUserId = currentUserId,
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Return to Habits tab before exiting on system back press
    BackHandler(enabled = activeTab != DailyFlowTab.HABITS) {
        viewModel.selectTab(DailyFlowTab.HABITS)
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    // Zero-permission Storage Access Framework launchers for JSON Backup Export & Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = viewModel.buildBackupJson()
        if (json == null) {
            viewModel.showMessage("No data ready to export yet")
            return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            }
            viewModel.showMessage("Backup exported")
        } catch (_: Exception) {
            viewModel.showMessage("Could not save backup file")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            if (raw.isNullOrBlank()) {
                viewModel.showMessage("Backup file is empty")
            } else {
                viewModel.importBackupJson(raw)
            }
        } catch (_: Exception) {
            viewModel.showMessage("Could not read backup file")
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        windowInsets = WindowInsets.navigationBars,
                    ) {
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = activeTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = activeTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes),
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val maxContentWidth = if (isWideScreen) 860.dp else 600.dp

                    when (val state = uiState) {
                        is UiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.state_loading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 420.dp)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.state_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        is UiState.Success -> {
                            val snapshot = state.data
                            val doneCount = snapshot.habits.count { selectedDate in it.completedDates }
                            val dayExpenses = snapshot.expenses.filter { it.date == selectedDate }
                            val daySpend = dayExpenses.sumOf { it.amount }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = maxContentWidth)
                                    .then(
                                        if (isWideScreen) {
                                            Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentPadding = PaddingValues(
                                    top = if (isWideScreen) 16.dp else 0.dp,
                                    bottom = 28.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                item(key = "header") {
                                    DailyFlowHeader(
                                        selectedDate = selectedDate,
                                        doneHabits = doneCount,
                                        totalHabits = snapshot.habits.size,
                                        daySpend = daySpend,
                                        dayExpenseCount = dayExpenses.size,
                                        dailyBudget = snapshot.settings.dailyBudget,
                                        currencySymbol = snapshot.settings.currencySymbol,
                                        userEmail = userEmail,
                                        isWideScreen = isWideScreen,
                                        onPrevDay = { viewModel.shiftSelectedDate(-1) },
                                        onNextDay = { viewModel.shiftSelectedDate(1) },
                                        onTodayClick = { viewModel.jumpToToday() },
                                        onSignOutClick = { showSignOutDialog = true },
                                        modifier = Modifier.padding(
                                            horizontal = if (isWideScreen) 20.dp else 0.dp
                                        ),
                                    )
                                }

                                if (activeTab != DailyFlowTab.SUMMARY) {
                                    item(key = "dashboard_expense_chart") {
                                        Last7DaysExpenseChartCard(
                                            expenses = snapshot.expenses,
                                            selectedDate = selectedDate,
                                            dailyBudget = snapshot.settings.dailyBudget,
                                            currencySymbol = snapshot.settings.currencySymbol,
                                            onSelectDate = { dateKey ->
                                                viewModel.jumpToDate(dateKey)
                                                viewModel.selectTab(DailyFlowTab.EXPENSES)
                                            },
                                            modifier = Modifier.padding(
                                                horizontal = if (isWideScreen) 20.dp else 16.dp
                                            ),
                                        )
                                    }
                                }

                                item(key = "tab_content") {
                                    AnimatedContent(
                                        targetState = activeTab,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "tab_transition",
                                        modifier = Modifier.padding(horizontal = if (isWideScreen) 20.dp else 16.dp),
                                    ) { tab ->
                                        when (tab) {
                                            DailyFlowTab.HABITS -> HabitsTabContent(
                                                habits = snapshot.habits,
                                                selectedDate = selectedDate,
                                                onAddHabit = viewModel::addHabit,
                                                onToggleHabit = viewModel::toggleHabit,
                                                onDeleteHabit = viewModel::deleteHabit,
                                            )

                                            DailyFlowTab.EXPENSES -> ExpensesTabContent(
                                                expenses = snapshot.expenses,
                                                selectedDate = selectedDate,
                                                selectedCategoryFilter = selectedCategoryFilter,
                                                currencySymbol = snapshot.settings.currencySymbol,
                                                onSelectCategoryFilter = viewModel::selectCategoryFilter,
                                                onAddExpense = viewModel::addExpense,
                                                onDeleteExpense = viewModel::deleteExpense,
                                            )

                                            DailyFlowTab.SUMMARY -> SummaryTabContent(
                                                habits = snapshot.habits,
                                                expenses = snapshot.expenses,
                                                selectedDate = selectedDate,
                                                dailyBudget = snapshot.settings.dailyBudget,
                                                currencySymbol = snapshot.settings.currencySymbol,
                                                isWideScreen = isWideScreen,
                                                isBusy = isBusy,
                                                onSelectDate = { dateKey ->
                                                    viewModel.jumpToDate(dateKey)
                                                    viewModel.selectTab(DailyFlowTab.EXPENSES)
                                                },
                                                onSaveDailyBudget = viewModel::updateDailyBudget,
                                                onExportBackup = {
                                                    exportLauncher.launch("dailyflow-backup-${DateKeyUtils.todayKey()}.json")
                                                },
                                                onImportBackup = {
                                                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                                },
                                                onRequestClearHistory = { showClearDialog = true },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllExpensesAndHistory()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_clear_button"),
                ) {
                    Text(stringResource(R.string.action_clear_data))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.testTag("cancel_clear_button"),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text(stringResource(R.string.dialog_sign_out_title)) },
            text = { Text(stringResource(R.string.dialog_sign_out_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = onSignedOut,
                        )
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button"),
                ) {
                    Text(stringResource(R.string.action_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
