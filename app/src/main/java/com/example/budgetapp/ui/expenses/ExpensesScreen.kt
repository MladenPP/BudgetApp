package com.example.budgetapp.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.domain.model.Expense
import com.example.budgetapp.domain.model.ExpenseKind
import com.example.budgetapp.domain.model.FixedLine
import com.example.budgetapp.domain.model.InstantExpense
import com.example.budgetapp.domain.model.ProgressiveLine
import com.example.budgetapp.domain.model.Purchase
import com.example.budgetapp.ui.AppViewModelProvider
import com.example.budgetapp.ui.components.ConfirmDialog
import com.example.budgetapp.ui.components.EmptyHint
import com.example.budgetapp.ui.components.ItemCard
import com.example.budgetapp.ui.components.NameAmountDialog
import com.example.budgetapp.ui.components.OverrideDialog
import com.example.budgetapp.ui.components.SectionHeader
import com.example.budgetapp.ui.util.formatDate
import com.example.budgetapp.ui.util.formatMoney

private sealed interface ExpensesDialog {
    data object AddFixed : ExpensesDialog
    data object AddProgressive : ExpensesDialog
    data class Edit(val expense: Expense) : ExpensesDialog
    data class SetOverride(
        val expense: Expense,
        val amount: Long,
        val isOverridden: Boolean
    ) : ExpensesDialog
    data class Delete(val expense: Expense) : ExpensesDialog
    data class DeletePurchase(val purchase: Purchase) : ExpensesDialog
    data class DeleteInstant(val item: InstantExpense) : ExpensesDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<ExpensesDialog?>(null) }
    val summary = state.summary

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Troškovi") }) }
    ) { padding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
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

                item {
                    SectionHeader(
                        title = "Stalni troškovi",
                        actionLabel = "+ Dodaj",
                        onAction = { dialog = ExpensesDialog.AddFixed }
                    )
                }
                if (summary.fixedLines.isEmpty()) {
                    item { EmptyHint("Još nema stalnih troškova (struja, voda, kirija...).") }
                } else {
                    items(summary.fixedLines, key = { "fixed-${it.expense.id}" }) { line ->
                        FixedItem(
                            line = line,
                            onEdit = { dialog = ExpensesDialog.Edit(line.expense) },
                            onOverride = {
                                dialog = ExpensesDialog.SetOverride(
                                    line.expense, line.amount, line.isOverridden
                                )
                            },
                            onDelete = { dialog = ExpensesDialog.Delete(line.expense) }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "Progresivni troškovi",
                        actionLabel = "+ Dodaj",
                        onAction = { dialog = ExpensesDialog.AddProgressive }
                    )
                }
                if (summary.progressiveLines.isEmpty()) {
                    item { EmptyHint("Još nema progresivnih troškova (npr. hrana, gorivo).") }
                } else {
                    items(summary.progressiveLines, key = { "progressive-${it.expense.id}" }) { line ->
                        ProgressiveItem(
                            line = line,
                            onEdit = { dialog = ExpensesDialog.Edit(line.expense) },
                            onOverride = {
                                dialog = ExpensesDialog.SetOverride(
                                    line.expense, line.reserved, line.isOverridden
                                )
                            },
                            onDelete = { dialog = ExpensesDialog.Delete(line.expense) }
                        )
                    }
                }

                if (state.purchases.isNotEmpty()) {
                    item { SectionHeader("Kupovine u ovom periodu") }
                    items(state.purchases, key = { "purchase-${it.id}" }) { purchase ->
                        val expenseName = summary.progressiveLines
                            .firstOrNull { it.expense.id == purchase.expenseId }
                            ?.expense?.name ?: "Kupovina"
                        ItemCard(
                            title = expenseName,
                            subtitle = buildString {
                                append(formatDate(purchase.date))
                                if (purchase.note.isNotBlank()) append(" · ${purchase.note}")
                            },
                            amount = formatMoney(purchase.amount),
                            actions = {
                                TextButton(
                                    onClick = { dialog = ExpensesDialog.DeletePurchase(purchase) }
                                ) { Text("Obriši") }
                            }
                        )
                    }
                }

                if (summary.instantExpenses.isNotEmpty()) {
                    item { SectionHeader("Trenutni troškovi u ovom periodu") }
                    items(summary.instantExpenses, key = { "instant-${it.id}" }) { item ->
                        ItemCard(
                            title = item.name,
                            subtitle = formatDate(item.date),
                            amount = "− ${formatMoney(item.amount)}",
                            actions = {
                                TextButton(
                                    onClick = { dialog = ExpensesDialog.DeleteInstant(item) }
                                ) { Text("Obriši") }
                            }
                        )
                    }
                }

                item {
                    EmptyHint(
                        "Nove kupovine i trenutne troškove dodaješ na ekranu Pregled (+ Dodaj)."
                    )
                }
            }
        }
    }

    if (summary != null) {
        when (val current = dialog) {
            null -> Unit

            ExpensesDialog.AddFixed -> NameAmountDialog(
                title = "Novi stalni trošak",
                description = "Cela suma se oduzima od budžeta na početku perioda " +
                        "(npr. struja, voda, kirija).",
                amountLabel = "Mesečni iznos (RSD)",
                onDismiss = { dialog = null },
                onSave = { name, amount ->
                    viewModel.addExpense(name, amount, ExpenseKind.FIXED)
                    dialog = null
                }
            )

            ExpensesDialog.AddProgressive -> NameAmountDialog(
                title = "Novi progresivni trošak",
                description = "Suma se odmah rezerviše, a pojedinačne kupovine (npr. hrana) " +
                        "troše tu rezervu. Tek ono preko rezerve skida se sa slobodnog novca.",
                amountLabel = "Mesečna rezerva (RSD)",
                onDismiss = { dialog = null },
                onSave = { name, amount ->
                    viewModel.addExpense(name, amount, ExpenseKind.PROGRESSIVE)
                    dialog = null
                }
            )

            is ExpensesDialog.Edit -> NameAmountDialog(
                title = "Izmena: ${current.expense.name}",
                description = "Menja se standardni iznos za sve buduće periode.",
                amountLabel = if (current.expense.kind == ExpenseKind.FIXED) {
                    "Mesečni iznos (RSD)"
                } else {
                    "Mesečna rezerva (RSD)"
                },
                initialName = current.expense.name,
                initialAmount = current.expense.amount,
                onDismiss = { dialog = null },
                onSave = { name, amount ->
                    viewModel.updateExpense(current.expense.copy(name = name, amount = amount))
                    dialog = null
                }
            )

            is ExpensesDialog.SetOverride -> OverrideDialog(
                title = current.expense.name,
                description = "Iznos važi samo za ovaj period (npr. drugačiji račun). " +
                        "Sledeći period se vraća na standardnih ${formatMoney(current.expense.amount)}.",
                currentAmount = current.amount,
                isOverridden = current.isOverridden,
                onDismiss = { dialog = null },
                onSave = { amount ->
                    viewModel.setExpenseOverride(current.expense.id, summary.period, amount)
                    dialog = null
                },
                onReset = {
                    viewModel.clearExpenseOverride(current.expense.id, summary.period)
                    dialog = null
                }
            )

            is ExpensesDialog.Delete -> ConfirmDialog(
                title = "Brisanje troška",
                text = "Obrisati „${current.expense.name}“? Biće uklonjen iz svih perioda, " +
                        "zajedno sa kupovinama i izmenama za pojedinačne mesece.",
                onConfirm = {
                    viewModel.deleteExpense(current.expense.id)
                    dialog = null
                },
                onDismiss = { dialog = null }
            )

            is ExpensesDialog.DeletePurchase -> ConfirmDialog(
                title = "Brisanje kupovine",
                text = "Obrisati kupovinu od ${formatMoney(current.purchase.amount)}?",
                onConfirm = {
                    viewModel.deletePurchase(current.purchase.id)
                    dialog = null
                },
                onDismiss = { dialog = null }
            )

            is ExpensesDialog.DeleteInstant -> ConfirmDialog(
                title = "Brisanje troška",
                text = "Obrisati „${current.item.name}“ (${formatMoney(current.item.amount)})?",
                onConfirm = {
                    viewModel.deleteInstantExpense(current.item.id)
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
        }
    }
}

@Composable
private fun FixedItem(
    line: FixedLine,
    onEdit: () -> Unit,
    onOverride: () -> Unit,
    onDelete: () -> Unit
) {
    ItemCard(
        title = line.expense.name,
        badge = if (line.isOverridden) {
            "Izmenjeno samo za ovaj period (standardno: ${formatMoney(line.expense.amount)})"
        } else {
            null
        },
        amount = "− ${formatMoney(line.amount)}",
        actions = {
            TextButton(onClick = onEdit) { Text("Izmeni") }
            TextButton(onClick = onOverride) { Text("Ovaj period") }
            TextButton(onClick = onDelete) { Text("Obriši") }
        }
    )
}

@Composable
private fun ProgressiveItem(
    line: ProgressiveLine,
    onEdit: () -> Unit,
    onOverride: () -> Unit,
    onDelete: () -> Unit
) {
    val fraction = if (line.reserved > 0) {
        (line.spent.toFloat() / line.reserved.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val over = line.overflow > 0

    ItemCard(
        title = line.expense.name,
        subtitle = "Potrošeno / rezervisano",
        badge = if (line.isOverridden) {
            "Izmenjeno samo za ovaj period (standardno: ${formatMoney(line.expense.amount)})"
        } else {
            null
        },
        amount = "${formatMoney(line.spent)} / ${formatMoney(line.reserved)}",
        content = {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp, end = 8.dp),
                color = if (over) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            if (over) {
                Text(
                    "Preko rezerve: ${formatMoney(line.overflow)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    "Ostalo u rezervi: ${formatMoney(line.remaining)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        actions = {
            TextButton(onClick = onEdit) { Text("Izmeni") }
            TextButton(onClick = onOverride) { Text("Ovaj period") }
            TextButton(onClick = onDelete) { Text("Obriši") }
        }
    )
}