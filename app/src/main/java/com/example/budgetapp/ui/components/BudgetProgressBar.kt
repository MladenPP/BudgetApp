package com.example.budgetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.budgetapp.domain.model.BudgetSummary
import com.example.budgetapp.ui.util.formatMoney

private val SavingsColor = Color(0xFF2E7D32)

private data class BarSegment(val label: String, val amount: Long, val color: Color)

@Composable
fun BudgetProgressBar(summary: BudgetSummary, modifier: Modifier = Modifier) {
    val segments = listOf(
        BarSegment(
            "Stalni troškovi i rezervacije",
            summary.fixedTotal + summary.progressiveReserved,
            MaterialTheme.colorScheme.outline
        ),
        BarSegment("Štednja", summary.savingsCurrent.coerceAtLeast(0), SavingsColor),
        BarSegment(
            "Potrošeno",
            summary.instantSpent + summary.progressiveOverflow,
            MaterialTheme.colorScheme.error
        ),
        BarSegment("Slobodno", summary.unallocatedLeft, MaterialTheme.colorScheme.primary)
    )
    val visible = segments.filter { it.amount > 0 }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (summary.base < 0) {
            Text(
                "Primanja ne pokrivaju stalne troškove i rezervacije " +
                        "(nedostaje ${formatMoney(-summary.base)}).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            visible.forEach { segment ->
                Box(
                    modifier = Modifier
                        .weight(segment.amount.toFloat())
                        .fillMaxHeight()
                        .background(segment.color)
                )
            }
        }

        segments.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(segment.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMoney(segment.amount),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}