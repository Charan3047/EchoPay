package com.example.upiexpensetracker

import android.os.Bundle
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.Secure
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.example.upiexpensetracker.ui.theme.UPIExpenseTrackerTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Canvas: Color
    @Composable get() = MaterialTheme.colorScheme.background
private val Ink: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground
private val Forest: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val Mint: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer
private val Quiet: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Line: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val Coral: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val transactions = AppDatabase.getDatabase(applicationContext)
            .transactionDao()
            .getAllTransactions()
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        val profilePreferences = getSharedPreferences("profile", MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
            var userName by remember { mutableStateOf(profilePreferences.getString("user_name", "").orEmpty()) }
            var notificationAccess by remember { mutableStateOf(hasNotificationAccess()) }
            var requestedSettings by remember { mutableStateOf(false) }
            var returnedWithoutAccess by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notificationAccess = hasNotificationAccess()
                        if (requestedSettings && !notificationAccess) returnedWithoutAccess = true
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
            UPIExpenseTrackerTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                if (!notificationAccess) {
                    NotificationAccessGate(
                        returnedWithoutAccess = returnedWithoutAccess,
                        onContinue = {
                            requestedSettings = true
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onExit = { finish() }
                    )
                } else if (userName.isBlank()) {
                    NameSetupScreen { name ->
                        profilePreferences.edit().putString("user_name", name).apply()
                        userName = name
                    }
                } else {
                    TransactionHistoryScreen(
                        transactions = transactions,
                        isDarkTheme = isDarkTheme,
                        userName = userName,
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                            preferences.edit().putBoolean("dark_theme", isDarkTheme).apply()
                        }
                    )
                }
            }
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val component = ComponentName(this, TransactionNotificationListener::class.java).flattenToString()
        val enabled = Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }
}

@Composable
private fun NameSetupScreen(onContinue: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Canvas).padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
        Text("Make EchoPay yours", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("What should we call you? Your name stays on this device.", color = Quiet)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, modifier = Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onContinue(name.trim()) }, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

@Composable
private fun NotificationAccessGate(
    returnedWithoutAccess: Boolean,
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("EchoPay needs Notification access", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("This access lets EchoPay detect payment notifications and update your expense tracker. Notification text is processed on this device and isn't saved.", color = Quiet)
        }
        AlertDialog(
            onDismissRequest = onExit,
            title = { Text(if (returnedWithoutAccess) "Notification access is required" else "Set up EchoPay") },
            text = { Text(if (returnedWithoutAccess) "Allow Notification access to use EchoPay. The app will close if you leave it off." else "Enable Notification access to start tracking transactions. You can turn it off later in Android Settings.") },
            confirmButton = { TextButton(onClick = onContinue) { Text(if (returnedWithoutAccess) "Try again" else "Continue") } },
            dismissButton = { TextButton(onClick = onExit) { Text("Exit") } }
        )
    }
}

private enum class DashboardTab(val label: String, val mark: String) {
    OVERVIEW("Overview", "◈"),
    ACTIVITY("Activity", "≡"),
    INSIGHTS("Insights", "⌁"),
    BUDGET("Budget", "₹")
}

private enum class HistoryFilter(val label: String) {
    ALL("All"),
    EXPENSES("Spent"),
    INCOME("Received")
}

private enum class DateFilter(val label: String) {
    ALL("All time"),
    MONTH("This month"),
    DAYS_30("30 days")
}

private data class DaySpend(val label: String, val amount: Double)

@Composable
private fun TransactionHistoryScreen(
    transactions: Flow<List<TransactionEntity>>,
    isDarkTheme: Boolean,
    userName: String,
    onToggleTheme: () -> Unit
) {
    val transactionList by transactions.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val exportScope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { destination ->
        if (destination != null) {
            val snapshot = transactionList
            exportScope.launch {
                val exported = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = context.contentResolver.openOutputStream(destination)
                            ?: error("Unable to open the selected file")
                        output.use { TransactionSpreadsheetExporter.write(it, snapshot) }
                    }
                }.isSuccess
                Toast.makeText(
                    context,
                    if (exported) "Excel file saved (${snapshot.size} transactions)" else "Could not save the Excel file",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    var selectedType by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedDate by remember { mutableStateOf(DateFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showManualEntry by remember { mutableStateOf(false) }
    val budgetCategories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Other")
    val budgetPrefs = remember { context.getSharedPreferences("category_budgets", android.content.Context.MODE_PRIVATE) }
    var budgets by remember { mutableStateOf(budgetCategories.associateWith { budgetPrefs.getString(it, "")?.toDoubleOrNull() ?: 0.0 }) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var budgetInput by remember { mutableStateOf("") }

    val now = Calendar.getInstance()
    val monthlyTransactions = transactionList.filter { it.timestamp.isInSameMonth(now) }
    val monthSpend = monthlyTransactions.filter { it.transactionType == "DEBIT" }.sumOf { it.amount }
    val weeklySpending = remember(transactionList) { buildWeeklySpending(transactionList) }

    val dateFiltered = when (selectedDate) {
        DateFilter.ALL -> transactionList
        DateFilter.MONTH -> transactionList.filter { it.timestamp.isInSameMonth(now) }
        DateFilter.DAYS_30 -> transactionList.filter { it.timestamp >= System.currentTimeMillis() - MILLIS_IN_30_DAYS }
    }
    val typeFiltered = when (selectedType) {
        HistoryFilter.ALL -> dateFiltered
        HistoryFilter.EXPENSES -> dateFiltered.filter { it.transactionType == "DEBIT" }
        HistoryFilter.INCOME -> dateFiltered.filter { it.transactionType != "DEBIT" }
    }
    val query = searchQuery.trim().lowercase(Locale.getDefault())
    val visibleTransactions = if (query.isBlank()) typeFiltered else typeFiltered.filter { transaction ->
        listOfNotNull(
            transaction.merchant,
            transaction.category,
            transaction.paymentMethod,
            transaction.transactionType
        ).any { it.lowercase(Locale.getDefault()).contains(query) }
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                DashboardTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.mark, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Mint.copy(alpha = 0.7f),
                            unselectedIconColor = Quiet,
                            unselectedTextColor = Quiet
                        )
                    )
                }
            }
        }
    ) { insets ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                DashboardTab.OVERVIEW -> {
                    item { DashboardHeader(userName, isDarkTheme, onToggleTheme) }
                    item { Button(onClick = { showManualEntry = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("＋ Add transaction") } }
                    item {
                        MonthlySpendCard(
                            monthSpend = monthSpend,
                            transactionCount = monthlyTransactions.size,
                            weeklySpending = weeklySpending
                        )
                    }
                    item {
                        ExportTransactionsButton(
                            enabled = transactionList.isNotEmpty(),
                            onClick = {
                                val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                exportLauncher.launch("upi_transactions_$date.xlsx")
                            }
                        )
                    }
                    item {
                        SectionHeading(
                            title = "$userName’s expenses",
                            subtitle = "${transactionList.size} transactions · updates live",
                            action = if (transactionList.isNotEmpty()) "See all →" else null,
                            onAction = { selectedTab = DashboardTab.ACTIVITY }
                        )
                    }
                    if (transactionList.isEmpty()) {
                        item { EmptyHistoryState(hasAnyTransactions = false) }
                    } else {
                        items(transactionList.take(5), key = { "home-${it.id}" }) { transaction ->
                            TransactionHistoryRow(transaction = transaction, onClick = { selectedTransaction = transaction })
                        }
                    }
                }

                DashboardTab.ACTIVITY -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            PageHeading("Activity", "Every payment, in one place", isDarkTheme, onToggleTheme)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(17.dp),
                            placeholder = { Text("Search merchant or category") },
                            leadingIcon = { Text("⌕", color = Forest, fontSize = 23.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Forest
                            )
                        )
                    }
                    item { FilterChipRow(selected = selectedType, onSelect = { selectedType = it }) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateFilter.entries.forEach { filter ->
                                FilterPill(filter.label, selectedDate == filter) { selectedDate = filter }
                            }
                        }
                    }
                    item {
                        Text("${visibleTransactions.size} results", color = Quiet, style = MaterialTheme.typography.labelMedium)
                    }
                    if (visibleTransactions.isEmpty()) {
                        item { EmptyHistoryState(hasAnyTransactions = transactionList.isNotEmpty()) }
                    } else {
                        items(visibleTransactions, key = { "activity-${it.id}" }) { transaction ->
                            TransactionHistoryRow(transaction = transaction, onClick = { selectedTransaction = transaction })
                        }
                    }
                }

                DashboardTab.INSIGHTS -> {
                    val categoryTotals = monthlyTransactions
                        .filter { it.transactionType == "DEBIT" }
                        .groupBy { it.category.ifBlank { "Other" } }
                        .mapValues { (_, items) -> items.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                    val largestCategory = categoryTotals.maxOfOrNull { it.second } ?: 0.0
                    val merchantTotals = monthlyTransactions
                        .filter { it.transactionType == "DEBIT" && !it.merchant.isNullOrBlank() }
                        .groupBy { it.merchant!! }
                        .mapValues { (_, items) -> items.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(5)

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            PageHeading("Insights", "Your spending patterns · this month", isDarkTheme, onToggleTheme)
                        }
                    }
                    item { MonthlySpendCard(monthSpend, monthlyTransactions.size, weeklySpending) }
                    item {
                        InsightsSummaryRow(
                            incoming = monthlyTransactions.filter { it.transactionType != "DEBIT" }.sumOf { it.amount },
                            needsReview = monthlyTransactions.count { it.needsReview }
                        )
                    }
                    item { Text("Spending by category", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                    if (categoryTotals.isEmpty()) {
                        item { EmptyHistoryState(hasAnyTransactions = false) }
                    } else {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(17.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                categoryTotals.forEach { (category, amount) ->
                                    CategoryInsightRow(category, amount, largestCategory)
                                }
                            }
                        }
                    }
                    if (merchantTotals.isNotEmpty()) {
                        item { Text("Top merchants", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 17.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                merchantTotals.forEachIndexed { index, (merchant, amount) ->
                                    MerchantInsightRow(merchant, amount)
                                    if (index != merchantTotals.lastIndex) {
                                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Line))
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardTab.BUDGET -> {
                    item { PageHeading("$userName’s Budget", "Optional monthly category limits", isDarkTheme, onToggleTheme) }
                    item { Text("Set a limit only for categories you want to track. You can leave any budget unset.", color = Quiet) }
                    items(budgetCategories) { category ->
                        val spent = monthlyTransactions.filter { it.transactionType == "DEBIT" && it.category.equals(category, true) }.sumOf { it.amount }
                        val limit = budgets[category] ?: 0.0
                        Card(Modifier.fillMaxWidth().clickable { editingCategory = category; budgetInput = if (limit > 0) limit.toString() else "" }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                        CategoryLineIcon(category)
                                        Text(category, color = Ink, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(if (limit > 0) "₹${"%.0f".format(spent)} / ₹${"%.0f".format(limit)}" else "Set limit", color = Quiet)
                                }
                                if (limit > 0) androidx.compose.material3.LinearProgressIndicator(progress = { (spent / limit).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = if (spent > limit) Coral else Forest)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { transaction ->
        TransactionDetailsDialog(
            transaction = transaction,
            onDismiss = { selectedTransaction = null },
            onCategoryChange = { category ->
                exportScope.launch {
                    AppDatabase.getDatabase(context).transactionDao().updateCategory(transaction.id, category)
                    selectedTransaction = null
                    Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = {
                exportScope.launch {
                    AppDatabase.getDatabase(context).transactionDao().deleteTransaction(transaction.id)
                    selectedTransaction = null
                    Toast.makeText(context, "Entry removed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    editingCategory?.let { category ->
        AlertDialog(onDismissRequest = { editingCategory = null }, title = { Text("$category monthly limit") },
            text = { OutlinedTextField(value = budgetInput, onValueChange = { budgetInput = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount in ₹ (0 removes limit)") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { val amount = budgetInput.toDoubleOrNull() ?: 0.0; budgetPrefs.edit().putString(category, if (amount > 0) amount.toString() else "").apply(); budgets = budgets + (category to amount.coerceAtLeast(0.0)); editingCategory = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("Cancel") } })
    }
    if (showManualEntry) {
        ManualTransactionDialog(
            onDismiss = { showManualEntry = false },
            onSave = { amount, merchant, category, type ->
                exportScope.launch {
                    AppDatabase.getDatabase(context).transactionDao().insertTransaction(
                        TransactionEntity(amount = amount, merchant = merchant, transactionType = type, paymentMethod = null, category = category, confidence = 1.0, needsReview = false, timestamp = System.currentTimeMillis())
                    )
                    showManualEntry = false
                }
            }
        )
    }
}

@Composable
private fun ManualTransactionDialog(onDismiss: () -> Unit, onSave: (Double, String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var debit by remember { mutableStateOf(true) }
    var pasted by remember { mutableStateOf("") }
    val parsed = remember(pasted) { if (pasted.isBlank()) null else TransactionParser().parse(null, pasted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount in ₹") }, singleLine = true)
            OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant or person") }, singleLine = true)
            OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
            OutlinedTextField(pasted, { pasted = it }, label = { Text("Or paste a bank message") }, minLines = 2, maxLines = 4)
            if (parsed != null) TextButton(onClick = { amount = parsed.amount?.toString().orEmpty(); merchant = parsed.merchant.orEmpty(); debit = parsed.transactionType == "DEBIT" }) { Text("Use parsed details") }
            Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { debit = !debit }) { Text(if (debit) "Expense · tap for income" else "Income · tap for expense") } }
        } },
        confirmButton = { TextButton(onClick = { val value = amount.toDoubleOrNull(); if (value != null && value > 0 && merchant.isNotBlank()) onSave(value, merchant.trim(), category.trim().ifBlank { "Other" }, if (debit) "DEBIT" else "CREDIT") }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SectionHeading(title: String, subtitle: String, action: String?, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Quiet, style = MaterialTheme.typography.bodySmall)
        }
        if (action != null) {
            Text(action, modifier = Modifier.clickable(onClick = onAction), color = Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterChipRow(selected: HistoryFilter, onSelect: (HistoryFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryFilter.entries.forEach { filter ->
            FilterPill(filter.label, selected == filter) { onSelect(filter) }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = Ink
        ),
        border = null
    )
}

@Composable
private fun InsightsSummaryRow(incoming: Double, needsReview: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightStatCard("MONEY IN", formatRupees(incoming), Modifier.weight(1f), Color(0xFF25836A))
        InsightStatCard("TO REVIEW", needsReview.toString(), Modifier.weight(1f), if (needsReview > 0) Coral else Forest)
    }
}

@Composable
private fun InsightStatCard(label: String, value: String, modifier: Modifier, accent: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(label, color = Quiet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun CategoryInsightRow(category: String, amount: Double, maximum: Double) {
    val tint = categoryTint(category)
    val fraction = if (maximum > 0) (amount / maximum).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryLineIcon(category)
                Text(category, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(formatRupees(amount), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Canvas)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun MerchantInsightRow(merchant: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(merchant, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatRupees(amount), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TransactionDetailsDialog(transaction: TransactionEntity, onDismiss: () -> Unit, onCategoryChange: (String) -> Unit, onDelete: () -> Unit) {
    val merchant = transaction.merchant?.takeIf { it.isNotBlank() } ?: "Transaction"
    var category by remember(transaction.id) { mutableStateOf(transaction.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(merchant, color = Ink, fontWeight = FontWeight.Bold)
                Text(
                    (if (transaction.transactionType == "DEBIT") "−" else "+") + formatRupees(transaction.amount),
                    color = if (transaction.transactionType == "DEBIT") Forest else Color(0xFF25836A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                DetailLine("Date", formatTimestamp(transaction.timestamp))
                DetailLine("Type", transaction.transactionType)
                DetailLine("Category", transaction.category)
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("FOOD", "OTHERS", "TRANSPORT", "SHOPPING", "BILLS", "ENTERTAINMENT").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (category == option) Mint.copy(alpha = 0.65f) else Color.Transparent)
                                .clickable { category = option }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                CategoryLineIcon(option)
                                Text(option.lowercase().replaceFirstChar { it.uppercase() }, color = Ink, fontSize = 13.sp)
                            }
                            if (category == option) Text("✓", color = Forest, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (category != transaction.category) TextButton(onClick = { onCategoryChange(category) }) { Text("Save category change") }
                DetailLine("Payment method", transaction.paymentMethod ?: "Not detected")
                DetailLine("Category confidence", "${(transaction.confidence * 100).toInt()}%")
                if (transaction.needsReview) {
                    Text("This category may need a quick check.", color = Coral, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Forest) } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Remove entry", color = Coral) } }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Quiet, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Ink, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private const val MILLIS_IN_30_DAYS = 30L * 24 * 60 * 60 * 1000

private fun Long.isInSameMonth(calendar: Calendar): Boolean {
    val date = Calendar.getInstance().apply { timeInMillis = this@isInSameMonth }
    return date.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
        date.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
}

@Composable
private fun ExportTransactionsButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Forest,
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            disabledContentColor = Quiet
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Mint.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("↓", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Forest)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text("Export to Excel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Save all transactions as .xlsx", fontSize = 11.sp, color = Quiet)
            }
            Text("→", fontSize = 19.sp, color = Forest)
        }
    }
}

@Composable
private fun DashboardHeader(userName: String, isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    var titleShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { titleShown = true }
    val titleAlpha by animateFloatAsState(if (titleShown) 1f else 0f, animationSpec = tween(durationMillis = 350), label = "brandTitleFade")
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "EchoPay logo",
                        modifier = Modifier.size(42.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "$userName’s EchoPay",
                        modifier = Modifier.graphicsLayer { alpha = titleAlpha; translationY = (1f - titleAlpha) * 3.dp.toPx() },
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("UPI TRACKER", color = Quiet, fontSize = 9.sp, letterSpacing = 1.2.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF3C9B70)))
                        Text("LIVE", color = Forest, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                }
                ThemeToggleButton(isDarkTheme, onToggleTheme)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())}.",
                color = Quiet,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "Money,\nin motion.",
                color = Ink,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp
            )
        }
    }
}

@Composable
private fun PageHeading(
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Quiet, style = MaterialTheme.typography.bodyMedium)
        }
        ThemeToggleButton(isDarkTheme, onToggleTheme)
    }
}

@Composable
private fun ThemeToggleButton(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggleTheme),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (isDarkTheme) "☼" else "☾",
                color = Forest,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonthlySpendCard(
    monthSpend: Double,
    transactionCount: Int,
    weeklySpending: List<DaySpend>
) {
    var selectedDay by remember { mutableStateOf<DaySpend?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF143E33), MaterialTheme.colorScheme.primaryContainer, Color(0xFF27634A))))
            .padding(horizontal = 20.dp, vertical = 19.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "SPENT THIS MONTH",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    formatRupees(monthSpend),
                    color = Color.White,
                    fontSize = 31.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    "$transactionCount ${if (transactionCount == 1) "transaction" else "transactions"} so far",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("↗", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.16f)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weeklySpending.forEachIndexed { index, day ->
                val maxSpend = weeklySpending.maxOfOrNull { it.amount } ?: 0.0
                val fraction = if (maxSpend > 0.0) (day.amount / maxSpend).toFloat().coerceAtLeast(0.08f) else 0.08f
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { selectedDay = day }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.11f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (index == weeklySpending.lastIndex) MaterialTheme.colorScheme.onPrimaryContainer else Color.White.copy(alpha = 0.56f))
                        )
                    }
                    Text(
                        day.label,
                        color = Color.White.copy(alpha = if (index == weeklySpending.lastIndex) 1f else 0.64f),
                        fontSize = 9.sp,
                        fontWeight = if (index == weeklySpending.lastIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("${day.label} spending") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatRupees(day.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Forest)
                Text("Total recorded spending for this day.", color = Quiet)
            } },
            confirmButton = { TextButton(onClick = { selectedDay = null }) { Text("Done") } }
        )
    }
}

@Composable
private fun TransactionHistoryRow(transaction: TransactionEntity, onClick: () -> Unit = {}) {
    val isExpense = transaction.transactionType == "DEBIT"
    val isRefund = transaction.transactionType == "REFUND"
    val accent = if (isExpense) Forest else Color(0xFF25836A)
    val tint = categoryTint(transaction.category)
    val merchant = transaction.merchant?.takeIf { it.isNotBlank() }
        ?: transaction.paymentMethod?.takeIf { it.isNotBlank() }
        ?: "Transaction"
    val sign = if (isExpense) "−" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(47.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            if (isRefund) Text("↺", color = tint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            else CategoryLineIcon(transaction.category, large = true)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                merchant,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    transaction.category,
                    color = Quiet,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(Modifier.size(3.dp).clip(CircleShape).background(Line))
                Text(formatTimestamp(transaction.timestamp), color = Quiet, fontSize = 11.sp)
            }
            if (transaction.needsReview) {
                Text("CHECK CATEGORY", color = Coral, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$sign${formatRupees(transaction.amount)}",
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            transaction.paymentMethod?.takeIf { it.isNotBlank() }?.let { method ->
                Text(method, color = Quiet, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(hasAnyTransactions: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.48f)),
            contentAlignment = Alignment.Center
        ) {
            Text("₹", color = Forest, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            if (hasAnyTransactions) "Nothing in this view" else "Your story starts here",
            color = Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (hasAnyTransactions) "Try another filter to see more activity."
            else "Your bank and UPI activity will appear here automatically.",
            color = Quiet,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun buildWeeklySpending(transactions: List<TransactionEntity>): List<DaySpend> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (6 downTo 0).map { daysAgo ->
        val day = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val nextDay = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val total = transactions.asSequence()
            .filter { it.transactionType == "DEBIT" && it.timestamp >= day.timeInMillis && it.timestamp < nextDay.timeInMillis }
            .sumOf { it.amount }
        DaySpend(SimpleDateFormat("EE", Locale.getDefault()).format(day.time), total)
    }
}

@Composable
private fun categoryTint(category: String): Color = when (category.lowercase(Locale.getDefault())) {
    "food", "dining", "groceries" -> Color(0xFFDE8A47)
    "transport", "travel" -> Color(0xFF5678C4)
    "shopping" -> Color(0xFFAA6AB3)
    "bills", "utilities" -> Color(0xFF418B88)
    "entertainment" -> Color(0xFFCD7259)
    else -> Forest
}

private fun categoryMark(category: String): String = when (category.uppercase(Locale.getDefault())) {
    "FOOD", "DINING", "GROCERIES" -> "⌁"
    "TRANSPORT", "TRAVEL" -> "↗"
    "SHOPPING" -> "▱"
    "BILLS", "UTILITIES" -> "▤"
    "ENTERTAINMENT" -> "▷"
    else -> "⋯"
}

@Composable
private fun CategoryLineIcon(category: String, large: Boolean = false) {
    val tint = categoryTint(category)
    Box(
        modifier = Modifier
            .size(if (large) 30.dp else 22.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(categoryMark(category), color = tint, fontSize = if (large) 16.sp else 13.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatRupees(amount: Double): String = NumberFormat
    .getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    .apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    .format(amount)

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(timestamp))
