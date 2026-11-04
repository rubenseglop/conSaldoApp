package com.example.saldoandalucia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.saldoandalucia.MainActivity
import com.example.saldoandalucia.R
import com.example.saldoandalucia.data.LocalStore

class SaldoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateAll(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, SaldoWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            val cards = LocalStore(context).cards()
            val card = cards.firstOrNull()
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_saldo)
                views.setTextViewText(R.id.widget_balance, card?.let { String.format(java.util.Locale("es", "ES"), "%.2f €", it.balance) } ?: "— €")
                views.setTextViewText(R.id.widget_card_name, card?.name ?: "Añade una tarjeta")
                views.setTextViewText(R.id.widget_updated, card?.let { "Última lectura guardada" } ?: "Abre conSaldo para empezar")
                val intent = Intent(context, MainActivity::class.java)
                val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
