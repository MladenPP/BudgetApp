package com.example.budgetapp.ui.report

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.domain.model.BudgetPeriod
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.domain.model.ReportKind
import com.example.budgetapp.ui.AppViewModelProvider
import com.example.budgetapp.ui.util.formatDate
import com.example.budgetapp.ui.util.formatMoney
import java.time.LocalDate

private val GoodColor = Color(0xFF2E7D32)

private fun periodLabel(period: BudgetPeriod): String =
    "${formatDate(period.start)} – ${formatDate(period.endInclusive)} ${period.endInclusive.year}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val report = state.report
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Izveštaj") },
                actions = {
                    Box {
                        TextButton(onClick = { menuOpen = true }) { Text("Istorija") }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tekući period") },
                                onClick = {
                                    viewModel.showCurrent()
                                    menuOpen = false
                                }
                            )
                            HorizontalDivider()
                            if (state.history.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Još nema sačuvanih izveštaja") },
                                    enabled = false,
                                    onClick = {}
                                )
                            } else {
                                state.history.forEach { saved ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${periodLabel(saved.period)} · " +
                                                        formatMoney(saved.actualSaved)
                                            )
                                        },
                                        onClick = {
                                            viewModel.showArchived(saved.period.key)
                                            menuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            report == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Prvo unesi glavno primanje na ekranu Pregled, pa će se ovde pojaviti izveštaj.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { ReportHeader(report, state.kind, state.nextReportDate) }
                item { VerdictCard(report, state.kind) }
                if (!report.metGoal) {
                    item { WhereCard(report) }
                }
                item { DetailsCard(report) }
            }
        }
    }
}

@Composable
private fun ReportHeader(report: PeriodReport, kind: ReportKind, nextReportDate: LocalDate?) {
    val title = when (kind) {
        ReportKind.LAST_DAY -> "Izveštaj za period"
        ReportKind.FINISHED -> "Izveštaj za završen period"
        ReportKind.PRELIMINARY -> "Preliminarno stanje tekućeg perioda"
    }
    val note = when (kind) {
        ReportKind.LAST_DAY ->
            "Period se završava danas. Izveštaj uključuje sve što je do sada uneto."
        ReportKind.FINISHED ->
            "Ovaj izveštaj je sačuvan i ne menja se kad izmeniš primanja ili troškove."
        ReportKind.PRELIMINARY ->
            if (nextReportDate != null) {
                "Period traje do ${formatDate(nextReportDate)}. " +
                        "Konačan izveštaj stiže poslednjeg dana perioda."
            } else {
                ""
            }
    }
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(periodLabel(report.period), style = MaterialTheme.typography.titleMedium)
        if (note.isNotEmpty()) {
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VerdictCard(report: PeriodReport, kind: ReportKind) {
    val preliminary = kind == ReportKind.PRELIMINARY
    val message = when {
        report.difference > 0 && preliminary ->
            "Super! Trenutno si ${formatMoney(report.difference)} iznad plana."
        report.difference > 0 ->
            "Super! Uštedeo si ${formatMoney(report.difference)} više od plana."
        report.difference == 0L -> "Tačno po planu."
        preliminary ->
            "Trenutno si ${formatMoney(-report.difference)} ispod plana."
        else ->
            "Uštedeo si ${formatMoney(-report.difference)} manje od plana."
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (preliminary) "Trenutna ušteda" else "Ušteđeno",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                formatMoney(report.actualSaved),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Planirana štednja: ${formatMoney(report.plannedSavings)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                message,
                style = MaterialTheme.typography.titleSmall,
                color = if (report.metGoal) GoodColor else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun WhereCard(report: PeriodReport) {
    val overspent = report.overspentProgressive
    val last = report.lastInstantExpense

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Gde si preterao", style = MaterialTheme.typography.titleMedium)

            overspent.forEach { line ->
                Text(
                    "${line.name}: planirano ${formatMoney(line.planned)}, " +
                            "potrošeno ${formatMoney(line.spent)}. " +
                            "Možeš da smanjiš za ${formatMoney(line.over)}.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (overspent.isNotEmpty() && last != null) {
                Text(
                    "Ali ni to ne bi bilo dovoljno da dostigneš planiranu štednju " +
                            "(nedostaje ${formatMoney(-report.difference)}).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (last != null) {
                val intro = if (overspent.isEmpty()) {
                    "Progresivni troškovi su u okviru rezerve. Poslednji trenutni trošak koji " +
                            "je smanjio štednju:"
                } else {
                    "Poslednji trenutni trošak:"
                }
                Text(
                    "$intro ${last.name} (${formatDate(last.date)}), ${formatMoney(last.amount)}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Da li je ovaj trošak bio neophodan?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }

            if (overspent.isEmpty() && last == null) {
                Text(
                    "Štednja je pala ispod plana zbog ukupne potrošnje.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DetailsCard(report: PeriodReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Detalji", style = MaterialTheme.typography.titleMedium)
            StatRow("Primanja", formatMoney(report.incomeTotal))
            if (report.bonusTotal > 0) {
                StatRow("Bonusi", "+ ${formatMoney(report.bonusTotal)}")
            }
            StatRow("Stalni troškovi", "− ${formatMoney(report.fixedTotal)}")
            report.progressiveLines.forEach { line ->
                StatRow(
                    "${line.name} (potrošeno / rezerva)",
                    "${formatMoney(line.spent)} / ${formatMoney(line.reserved)}",
                    if (line.overflow > 0) MaterialTheme.colorScheme.error else Color.Unspecified
                )
            }
            StatRow("Trenutni troškovi", "− ${formatMoney(report.instantSpent)}")

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            StatRow("Planirana štednja", formatMoney(report.plannedSavings))
            if (report.savingsEaten > 0) {
                StatRow(
                    "Pojedeno iz štednje",
                    "− ${formatMoney(report.savingsEaten)}",
                    MaterialTheme.colorScheme.error
                )
            }
            StatRow("Slobodan novac koji je ostao", formatMoney(report.unallocatedLeft))
            StatRow("Neiskorišćene rezerve", formatMoney(report.remainingReserve))

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            StatRow("Ukupno ušteđeno", formatMoney(report.actualSaved), bold = true)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}