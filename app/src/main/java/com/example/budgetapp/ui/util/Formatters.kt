package com.example.budgetapp.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale


fun formatMoney(amount: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
    return DecimalFormat("#,##0", symbols).format(amount) + " RSD"
}

private val MONTHS = listOf(
    "jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec"
)

fun formatDate(date: LocalDate): String = "${date.dayOfMonth}. ${MONTHS[date.monthValue - 1]}"

fun parseAmount(text: String): Long? =
    text.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toLongOrNull()

fun isSingularSerbian(count: Long): Boolean = count % 10 == 1L && count % 100 != 11L

fun formatDays(count: Long): String =
    if (isSingularSerbian(count)) "$count dan" else "$count dana"