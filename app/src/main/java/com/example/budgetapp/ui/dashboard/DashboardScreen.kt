package com.example.budgetapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.calc.SavingsRules
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.domain.model.ProgressiveLine
import com.example.budgetapp.ui.AppViewModelProvider
import com.example.budgetapp.ui.components.BonusDialog
import com.example.budgetapp.ui.components.BudgetProgressBar
import com.example.budgetapp.ui.components.InstantExpenseDialog
import com.example.budgetapp.ui.components.MainIncomeDialog
import com.example.budgetapp.ui.components.PurchaseDialog
import com.example.budgetapp.ui.util.formatDate
import com.example.budgetapp.ui.util.formatDays
import com.example.budgetapp.ui.util.formatMoney
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SavingsColor = Color(0xFF2E7D32)

private sealed interface DashboardDialog {
    data object AddMainIncome : DashboardDialog
    data object AddInstant : DashboardDialog
    data object AddBonus : DashboardDialog
    data class AddPurchase(val expenseId: Long?) : DashboardDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<DashboardDialog?>(null) }
    val summary = state.summary

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Budžet") }) },
        floatingActionButton = {
            if (summary != null) {
                AddMenuFab(
                    hasProgressive = summary.progressiveLines.isNotEmpty(),
                    onInstant = { dialog = DashboardDialog.AddInstant },
                    onPurchase = { dialog = DashboardDialog.AddPurchase(null) },
                    onBonus = { dialog = DashboardDialog.AddBonus }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            summary == null -> OnboardingContent(
                modifier = Modifier.padding(padding),
                onStart = { dialog = DashboardDialog.AddMainIncome }
            )

            else -> DashboardContent(
                summary = summary,
                today = state.today,
                modifier = Modifier.padding(padding),
                onProgressiveClick = { id -> dialog = DashboardDialog.AddPurchase(id) }
            )
        }
    }

    when (val current = dialog) {
        null -> Unit

        DashboardDialog.AddMainIncome -> MainIncomeDialog(
            onDismiss = { dialog = null },
            onSave = { name, amount, day ->
                viewModel.saveMainIncome(name, amount, day)
                dialog = null
            }
        )

        DashboardDialog.AddInstant -> InstantExpenseDialog(
            onDismiss = { dialog = null },
            onSave = { name, amount ->
                viewModel.addInstantExpense(name, amount)
                dialog = null
            }
        )

        DashboardDialog.AddBonus -> BonusDialog(
            savingsPercent = summary?.savingsPercent ?: SavingsRules.DEFAULT_PERCENT,
            onDismiss = { dialog = null },
            onSave = { name, amount ->
                viewModel.addBonus(name, amount)
                dialog = null
            }
        )

        is DashboardDialog.AddPurchase -> {
            val lines = summary?.progressiveLines.orEmpty()
            if (lines.isNotEmpty()) {
                PurchaseDialog(
                    lines = lines,
                    preselectedId = current.expenseId,
                    onDismiss = { dialog = null },
                    onSave = { expenseId, amount, note ->
                        viewModel.addPurchase(expenseId, amount, note)
                        dialog = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AddMenuFab(
    hasProgressive: Boolean,
    onInstant: () -> Unit,
    onPurchase: () -> Unit,
    onBonus: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ExtendedFloatingActionButton(onClick = { expanded = true }) {
            Text("+ Dodaj")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Trenutni trošak") },
                onClick = {
                    expanded = false
                    onInstant()
                }
            )
            if (hasProgressive) {
                DropdownMenuItem(
                    text = { Text("Kupovina (progresivni trošak)") },
                    onClick = {
                        expanded = false
                        onPurchase()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Bonus") },
                onClick = {
                    expanded = false
                    onBonus()
                }
            )
        }
    }
}

@Composable
private fun OnboardingContent(modifier: Modifier = Modifier, onStart: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dobrodošao!", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Počni unosom glavnog primanja (npr. plate). Od njega se računa " +
                    "budžetski period i koliko možeš da uštediš.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        TextButton(onClick = onStart) { Text("Unesi glavno primanje") }
    }
}

@Composable
private fun DashboardContent(
    summary: BudgetSummary,
    today: LocalDate,
    modifier: Modifier = Modifier,
    onProgressiveClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PeriodHeader(summary, today) }
        item { BalanceCard(summary) }
        item { OverviewCard(summary) }
        if (summary.progressiveLines.isNotEmpty()) {
            item { ProgressiveCard(summary.progressiveLines, onProgressiveClick) }
        }
        if (summary.instantExpenses.isNotEmpty()) {
            item { RecentExpensesCard(summary) }
        }
    }
}

@Composable
private fun PeriodHeader(summary: BudgetSummary, today: LocalDate) {
    val daysLeft = ChronoUnit.DAYS.between(today, summary.period.endExclusive).coerceAtLeast(0)
    Column {
        Text(
            "${formatDate(summary.period.start)} – ${formatDate(summary.period.endInclusive)}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Do kraja perioda: ${formatDays(daysLeft)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BalanceCard(summary: BudgetSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Slobodno za trošenje", style = MaterialTheme.typography.labelLarge)
            Text(
                formatMoney(summary.unallocatedLeft),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Ušteđeno: ${formatMoney(summary.savingsCurrent)} " +
                        "od planiranih ${formatMoney(summary.plannedSavings)} " +
                        "(${summary.savingsPercent}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else SavingsColor
            )
            if (summary.savingsEaten > 0) {
                Text(
                    "Slobodnog novca je ponestalo, potrošeno je " +
                            "${formatMoney(summary.savingsEaten)} iz štednje.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            BudgetProgressBar(summary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun OverviewCard(summary: BudgetSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pregled perioda", style = MaterialTheme.typography.titleMedium)
            StatRow("Primanja", formatMoney(summary.incomeTotal))
            StatRow("Stalni troškovi", "− ${formatMoney(summary.fixedTotal)}")
            StatRow("Rezervisano za progresivne", "− ${formatMoney(summary.progressiveReserved)}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            StatRow("Osnovica za štednju", formatMoney(summary.base))
            val bonusTotal = summary.bonuses.sumOf { it.amount }
            if (bonusTotal > 0) {
                StatRow("Bonusi", "+ ${formatMoney(bonusTotal)}")
            }
            summary.incomeLines.filter { !it.arrived }.forEach { line ->
                val date = PeriodCalculator.occurrenceDate(line.income.dayOfMonth, summary.period)
                StatRow(
                    "Još stiže: ${line.income.name} (${formatDate(date)})",
                    "+ ${formatMoney(line.amount)}"
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProgressiveCard(lines: List<ProgressiveLine>, onClick: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Progresivni troškovi", style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                val fraction = if (line.reserved > 0) {
                    (line.spent.toFloat() / line.reserved.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val over = line.overflow > 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(line.expense.id) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(line.expense.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${formatMoney(line.spent)} / ${formatMoney(line.reserved)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (over) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    if (over) {
                        Text(
                            "Preko rezerve: ${formatMoney(line.overflow)} (skida se sa slobodnog novca)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentExpensesCard(summary: BudgetSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Poslednji troškovi", style = MaterialTheme.typography.titleMedium)
            summary.instantExpenses.take(5).forEach { expense ->
                StatRow(
                    "${expense.name} (${formatDate(expense.date)})",
                    "− ${formatMoney(expense.amount)}"
                )
            }
        }
    }
}