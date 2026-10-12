package com.example.budgetapp.ui.incomes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.domain.calc.PeriodCalculator
import com.example.budgetapp.domain.model.Bonus
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.Income
import com.example.budgetapp.domain.model.IncomeKind
import com.example.budgetapp.domain.model.IncomeLine
import com.example.budgetapp.ui.AppViewModelProvider
import com.example.budgetapp.ui.components.ConfirmDialog
import com.example.budgetapp.ui.components.EmptyHint
import com.example.budgetapp.ui.components.IncomeDialog
import com.example.budgetapp.ui.components.ItemCard
import com.example.budgetapp.ui.components.OverrideDialog
import com.example.budgetapp.ui.components.SectionHeader
import com.example.budgetapp.ui.util.formatDate
import com.example.budgetapp.ui.util.formatMoney

private sealed interface IncomesDialog {
    data object AddMain : IncomesDialog
    data object AddRecurring : IncomesDialog
    data class Edit(val income: Income) : IncomesDialog
    data class SetOverride(val line: IncomeLine) : IncomesDialog
    data class Delete(val income: Income) : IncomesDialog
    data class DeleteBonus(val bonus: Bonus) : IncomesDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(
    modifier: Modifier = Modifier,
    viewModel: IncomesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<IncomesDialog?>(null) }
    val summary = state.summary

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Primanja") }) }
    ) { padding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            val mainLine = summary.incomeLines.firstOrNull { it.income.kind == IncomeKind.MAIN }
            val recurring = summary.incomeLines.filter { it.income.kind == IncomeKind.RECURRING }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Period: ${formatDate(summary.period.start)} – " +
                                formatDate(summary.period.endInclusive),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { SectionHeader("Glavno primanje") }
                if (mainLine == null) {
                    item {
                        TextButton(onClick = { dialog = IncomesDialog.AddMain }) {
                            Text("Unesi glavno primanje")
                        }
                    }
                } else {
                    item {
                        IncomeItem(
                            line = mainLine,
                            period = summary.period,
                            onEdit = { dialog = IncomesDialog.Edit(mainLine.income) },
                            onOverride = { dialog = IncomesDialog.SetOverride(mainLine) },
                            onDelete = { dialog = IncomesDialog.Delete(mainLine.income) }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Stalna primanja",
                        actionLabel = "+ Dodaj",
                        onAction = { dialog = IncomesDialog.AddRecurring }
                    )
                }
                if (recurring.isEmpty()) {
                    item { EmptyHint("Još nema stalnih sekundarnih primanja (npr. dodatni posao).") }
                } else {
                    items(recurring, key = { "recurring-${it.income.id}" }) { line ->
                        IncomeItem(
                            line = line,
                            period = summary.period,
                            onEdit = { dialog = IncomesDialog.Edit(line.income) },
                            onOverride = { dialog = IncomesDialog.SetOverride(line) },
                            onDelete = { dialog = IncomesDialog.Delete(line.income) }
                        )
                    }
                }

                item { SectionHeader("Bonusi u ovom periodu") }
                if (summary.bonuses.isEmpty()) {
                    item { EmptyHint("Bonus dodaješ na ekranu Pregled (+ Dodaj → Bonus).") }
                } else {
                    items(summary.bonuses, key = { "bonus-${it.id}" }) { bonus ->
                        ItemCard(
                            title = bonus.name,
                            subtitle = formatDate(bonus.date),
                            amount = "+ ${formatMoney(bonus.amount)}",
                            content = {
                                Text(
                                    "U štednju: ${formatMoney(bonus.savingsPart)} · " +
                                            "Slobodno: ${formatMoney(bonus.unallocatedPart)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            actions = {
                                TextButton(onClick = { dialog = IncomesDialog.DeleteBonus(bonus) }) {
                                    Text("Obriši")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (summary != null) {
        when (val current = dialog) {
            null -> Unit

            IncomesDialog.AddMain -> IncomeDialog(
                title = "Glavno primanje",
                description = "Dan u mesecu kada primaš ovo primanje određuje početak perioda.",
                onDismiss = { dialog = null },
                onSave = { name, amount, day ->
                    viewModel.saveMainIncome(name, amount, day)
                    dialog = null
                }
            )

            IncomesDialog.AddRecurring -> IncomeDialog(
                title = "Novo stalno primanje",
                description = "Pojavljuje se svakog meseca na izabrani dan.",
                onDismiss = { dialog = null },
                onSave = { name, amount, day ->
                    viewModel.addRecurringIncome(name, amount, day)
                    dialog = null
                }
            )

            is IncomesDialog.Edit -> IncomeDialog(
                title = "Izmena: ${current.income.name}",
                description = if (current.income.kind == IncomeKind.MAIN) {
                    "Menja se standardni iznos za sve periode. Promena dana pomera početak perioda."
                } else {
                    "Menja se standardni iznos za sve buduće periode."
                },
                initialName = current.income.name,
                initialAmount = current.income.amount,
                initialDay = current.income.dayOfMonth,
                onDismiss = { dialog = null },
                onSave = { name, amount, day ->
                    viewModel.updateIncome(
                        current.income.copy(name = name, amount = amount, dayOfMonth = day)
                    )
                    dialog = null
                }
            )

            is IncomesDialog.SetOverride -> OverrideDialog(
                title = current.line.income.name,
                description = "Iznos važi samo za ovaj period. Sledeći period se vraća na " +
                        "standardnih ${formatMoney(current.line.income.amount)}.",
                currentAmount = current.line.amount,
                isOverridden = current.line.isOverridden,
                onDismiss = { dialog = null },
                onSave = { amount ->
                    viewModel.setIncomeOverride(current.line.income.id, summary.period, amount)
                    dialog = null
                },
                onReset = {
                    viewModel.clearIncomeOverride(current.line.income.id, summary.period)
                    dialog = null
                }
            )

            is IncomesDialog.Delete -> ConfirmDialog(
                title = "Brisanje primanja",
                text = "Obrisati „${current.income.name}“? Biće uklonjeno iz svih perioda, " +
                        "zajedno sa izmenama za pojedinačne mesece.",
                onConfirm = {
                    viewModel.deleteIncome(current.income.id)
                    dialog = null
                },
                onDismiss = { dialog = null }
            )

            is IncomesDialog.DeleteBonus -> ConfirmDialog(
                title = "Brisanje bonusa",
                text = "Obrisati bonus „${current.bonus.name}“ (${formatMoney(current.bonus.amount)})?",
                onConfirm = {
                    viewModel.deleteBonus(current.bonus.id)
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }
    }
}

@Composable
private fun IncomeItem(
    line: IncomeLine,
    period: BudgetPeriod,
    onEdit: () -> Unit,
    onOverride: () -> Unit,
    onDelete: () -> Unit
) {
    val income = line.income
    val subtitle = if (income.kind == IncomeKind.MAIN) {
        "${income.dayOfMonth}. u mesecu · određuje početak perioda"
    } else {
        "Svakog ${income.dayOfMonth}. u mesecu"
    }
    val badge = when {
        line.isOverridden ->
            "Izmenjeno samo za ovaj period (standardno: ${formatMoney(income.amount)})"
        !line.arrived ->
            "Stiže ${formatDate(PeriodCalculator.occurrenceDate(income.dayOfMonth, period))}"
        else -> null
    }

    ItemCard(
        title = income.name,
        subtitle = subtitle,
        badge = badge,
        amount = "+ ${formatMoney(line.amount)}",
        actions = {
            TextButton(onClick = onEdit) { Text("Izmeni") }
            TextButton(onClick = onOverride) { Text("Ovaj period") }
            TextButton(onClick = onDelete) { Text("Obriši") }
        }
    )
}