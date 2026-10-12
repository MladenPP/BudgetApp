package com.example.budgetapp.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetapp.domain.calc.SavingsRules
import com.example.budgetapp.ui.AppViewModelProvider
import com.example.budgetapp.ui.components.ConfirmDialog
import com.example.budgetapp.worker.ReportNotifier
import com.example.budgetapp.worker.ReportScheduler
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onDataReset: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val percent by viewModel.savingsPercent.collectAsStateWithLifecycle()
    var sliderValue by remember(percent) { mutableStateOf(percent.toFloat()) }
    var showResetDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) ReportScheduler.sendTest(context)
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Postavke") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Procenat štednje", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${sliderValue.roundToInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.setSavingsPercent(sliderValue.roundToInt())
                        },
                        valueRange = SavingsRules.MIN_PERCENT.toFloat()..SavingsRules.MAX_PERCENT.toFloat(),
                        steps = SavingsRules.MAX_PERCENT - SavingsRules.MIN_PERCENT - 1
                    )
                    Text(
                        "Od osnovice (primanja minus stalni troškovi i rezervacije) " +
                                "odvaja se ovaj procenat kao cilj štednje. Dozvoljeno je od " +
                                "${SavingsRules.MIN_PERCENT}% do ${SavingsRules.MAX_PERCENT}%.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Promena važi za tekući period i za nove bonuse. Bonusi koji su već " +
                                "uneseni zadržavaju podelu iz trenutka unosa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Obaveštenja", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Poslednjeg dana budžetskog perioda, oko 18:00, stiže obaveštenje " +
                                "sa izveštajem o štednji.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            if (ReportNotifier.canNotify(context)) {
                                ReportScheduler.sendTest(context)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) { Text("Pošalji probno obaveštenje") }
                    Text(
                        "Probno obaveštenje koristi stanje tekućeg perioda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Resetovanje", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Briše sve podatke i vraća aplikaciju na početak, kao da je tek instalirana.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text("Resetuj aplikaciju") }
                }
            }
        }
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Resetovati aplikaciju?",
            text = "Biće obrisano sve: glavno i stalna primanja, stalni i progresivni " +
                    "troškovi, bonusi, kupovine, svi sačuvani izveštaji i podešavanja. " +
                    "Ovo se ne može poništiti.",
            confirmLabel = "Obriši sve",
            onConfirm = {
                showResetDialog = false
                viewModel.resetApp(onDone = onDataReset)
            },
            onDismiss = { showResetDialog = false }
        )
    }
}