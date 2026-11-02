package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TreasurerWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                val database = AppDatabase.getInstance(context)
                val cotisationDao = database.cotisationDao()
                val memberDao = database.memberDao()

                val cotisations = cotisationDao.getAllCotisationsFlow().firstOrNull() ?: emptyList()
                val members = memberDao.getAllMembersFlow().firstOrNull() ?: emptyList()

                val totalSolde = cotisations.sumOf { it.montant }
                val membersWithArrears = members.count { it.soldeArrieres > 0 }

                val formatter = NumberFormat.getNumberInstance(Locale.FRANCE)
                val formattedBalance = "${formatter.format(totalSolde.toLong())} FCFA"
                val pendingText = if (membersWithArrears > 0) {
                    "$membersWithArrears impayé${if (membersWithArrears > 1) "s" else ""}"
                } else {
                    "✓ Comptes à jour"
                }

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = timeFormat.format(Date())

                val views = RemoteViews(context.packageName, R.layout.widget_treasurer).apply {
                    setTextViewText(R.id.widget_treasury_balance, formattedBalance)
                    setTextViewText(R.id.widget_pending_count, pendingText)
                    setTextViewText(R.id.widget_timestamp, "Mis à jour à $timeStr")

                    // Clic pour ouvrir l'application
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_root_layout, pendingIntent)
                    setOnClickPendingIntent(R.id.widget_btn_open, pendingIntent)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // Fallback avec vue par défaut
                val views = RemoteViews(context.packageName, R.layout.widget_treasurer).apply {
                    setTextViewText(R.id.widget_treasury_balance, "RAVA Trésor")
                    setTextViewText(R.id.widget_pending_count, "Toucher pour ouvrir")
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TreasurerWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TreasurerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
}
