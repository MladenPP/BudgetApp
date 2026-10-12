package com.example.budgetapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.example.budgetapp.ui.util.parseAmount

@Composable
fun NameAmountDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long) -> Unit,
    description: String? = null,
    nameLabel: String = "Naziv",
    amountLabel: String = "Iznos (RSD)",
    initialName: String = "",
    initialAmount: Long? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf(initialAmount?.toString() ?: "") }

    val amountValue = parseAmount(amount)
    val valid = name.isNotBlank() && amountValue != null && amountValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
                NameField(name, { name = it }, nameLabel)
                AmountField(amount, { amount = it }, amountLabel)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), amountValue!!) },
                enabled = valid
            ) { Text("Sačuvaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}

@Composable
fun IncomeDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Long, dayOfMonth: Int) -> Unit,
    description: String? = null,
    initialName: String = "",
    initialAmount: Long? = null,
    initialDay: Int? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var day by remember { mutableStateOf(initialDay?.toString() ?: "") }

    val amountValue = parseAmount(amount)
    val dayValue = parseAmount(day)?.toInt()
    val valid = name.isNotBlank() && amountValue != null && amountValue > 0 &&
            dayValue != null && dayValue in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
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
fun OverrideDialog(
    title: String,
    description: String,
    currentAmount: Long,
    isOverridden: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: Long) -> Unit,
    onReset: () -> Unit
) {
    var amount by remember { mutableStateOf(currentAmount.toString()) }
    val amountValue = parseAmount(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                AmountField(amount, { amount = it }, "Iznos za ovaj period (RSD)")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amountValue!!) },
                enabled = amountValue != null
            ) { Text("Sačuvaj") }
        },
        dismissButton = {
            Row {
                if (isOverridden) {
                    TextButton(onClick = onReset) { Text("Vrati standardno") }
                }
                TextButton(onClick = onDismiss) { Text("Otkaži") }
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Obriši"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Otkaži") } }
    )
}