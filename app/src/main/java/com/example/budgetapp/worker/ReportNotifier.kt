package com.example.budgetapp.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.budgetapp.MainActivity
import com.example.budgetapp.domain.model.PeriodReport
import com.example.budgetapp.navigation.Destination
import com.example.budgetapp.ui.util.formatDate
import com.example.budgetapp.ui.util.formatMoney


object ReportNotifier {

    const val EXTRA_OPEN_ROUTE = "open_route"

    private const val CHANNEL_ID = "period_report"
    private const val NOTIFICATION_ID = 1001

    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, report: PeriodReport): Boolean {
        if (!canNotify(context)) return false
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_OPEN_ROUTE, Destination.REPORT.route)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = message(report)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(
                "Izveštaj: ${formatDate(report.period.start)} – ${formatDate(report.period.endInclusive)}"
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun message(report: PeriodReport): String {
        val saved = formatMoney(report.actualSaved)
        return when {
            report.difference > 0 ->
                "Uštedeo si $saved, ${formatMoney(report.difference)} više od plana. Bravo!"
            report.difference == 0L ->
                "Uštedeo si $saved, tačno koliko si planirao."
            else ->
                "Uštedeo si $saved, ${formatMoney(-report.difference)} manje od plana. " +
                        "Otvori izveštaj da vidiš gde možeš bolje."
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Izveštaj o budžetu",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Izveštaj o štednji na kraju budžetskog perioda"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}