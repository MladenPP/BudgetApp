package com.example.budgetapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.budgetapp.domain.calc.SavingsRules
import com.example.budgetapp.domain.model.ProgressiveLine
import com.example.budgetapp.ui.util.formatMoney
import com.example.budgetapp.ui.util.parseAmount

@Composable
fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(40)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxDigits: Int = 12
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }.take(maxDigits)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MainIncomeDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long, dayOfMonth: Int) -> Unit
) {
    var name by remember { mutableStateOf("Plata") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }

    val amountValue = parseAmount(amount)
    val dayValue = parseAmount(day)?.toInt()
    val valid = name.isNotBlank() && amountValue != null && amountValue > 0 &&
            dayValue != null && dayValue in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Glavno primanje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Dan u mesecu kada primaš ovo primanje određuje početak perioda " +
                            "(npr. od 5. do 5.).",
                    style = MaterialTheme.typography.bodyMedium
                )
                NameField(name, { name = it }, "Naziv")
                AmountField(amount, { amount = it }, "Iznos (RSD)")
                AmountField(day, { day = it }, "Dan u mesecu (1-31)", maxDigits = 2)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountValue!!, dayValue!!) },
                enabled = valid
            ) { Text("Sačuvaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}

@Composable
fun InstantExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val amountValue = parseAmount(amount)
    val valid = name.isNotBlank() && amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trenutni trošak") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NameField(name, { name = it }, "Na šta si potrošio")
                AmountField(amount, { amount = it }, "Iznos (RSD)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountValue!!) },
                enabled = valid
            ) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}

@Composable
fun BonusDialog(
    savingsPercent: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long) -> Unit
) {
    var name by remember { mutableStateOf("Bonus") }
    var amount by remember { mutableStateOf("") }

    val amountValue = parseAmount(amount)
    val valid = name.isNotBlank() && amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bonus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NameField(name, { name = it }, "Naziv")
                AmountField(amount, { amount = it }, "Iznos (RSD)")
                if (amountValue != null && amountValue > 0) {
                    val (toSavings, toFree) = SavingsRules.splitBonus(amountValue, savingsPercent)
                    Text(
                        "Podela ($savingsPercent% štednja): " +
                                "${formatMoney(toSavings)} u štednju, ${formatMoney(toFree)} slobodno.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountValue!!) },
                enabled = valid
            ) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}

@Composable
fun PurchaseDialog(
    lines: List<ProgressiveLine>,
    preselectedId: Long?,
    onDismiss: () -> Unit,
    onSave: (expenseId: Long, amount: Long, note: String) -> Unit
) {
    var selectedId by remember {
        mutableStateOf(preselectedId ?: lines.first().expense.id)
    }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amountValue = parseAmount(amount)
    val valid = amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova kupovina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lines.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedId = line.expense.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedId == line.expense.id,
                            onClick = { selectedId = line.expense.id }
                        )
                        Text(
                            "${line.expense.name} (ostalo ${formatMoney(line.remaining)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                AmountField(amount, { amount = it }, "Iznos (RSD)")
                NameField(note, { note = it }, "Napomena (opciono)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedId, amountValue!!, note.trim()) },
                enabled = valid
            ) { Text("Dodaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}