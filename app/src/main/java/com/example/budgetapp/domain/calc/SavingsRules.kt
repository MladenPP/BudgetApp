package com.example.budgetapp.domain.calc

object SavingsRules {
    const val MIN_PERCENT = 0
    const val MAX_PERCENT = 50
    const val DEFAULT_PERCENT = 20

    fun clampPercent(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun savingsPart(amount: Long, percent: Int): Long =
        amount * clampPercent(percent) / 100

    fun splitBonus(amount: Long, percent: Int): Pair<Long, Long> {
        val savings = savingsPart(amount, percent)
        return savings to (amount - savings)
    }
}