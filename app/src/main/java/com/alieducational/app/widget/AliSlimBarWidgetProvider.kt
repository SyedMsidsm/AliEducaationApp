package com.alieducational.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.alieducational.app.MainActivity
import com.alieducational.app.R

class AliSlimBarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        updateAllWidgets(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    companion object {
        const val ACTION_WIDGET_CLICK = "com.alieducational.app.action.WIDGET_CLICK"
        const val EXTRA_WIDGET_ACTION = "extra_widget_action"

        const val ACTION_CASH_IN = "CASH_IN"
        const val ACTION_CASH_OUT = "CASH_OUT"
        const val ACTION_NEW_BILL = "NEW_BILL"
        const val ACTION_ADD_STOCK = "ADD_STOCK"
        const val ACTION_HOME = "HOME"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val myProvider = ComponentName(context, AliSlimBarWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(myProvider)
                if (ids != null && ids.isNotEmpty()) {
                    for (id in ids) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (_: Exception) {
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_slim_bar)

            // Setup pending intents for each of the 4 actions + home title
            views.setOnClickPendingIntent(
                R.id.widget_title,
                createActionPendingIntent(context, ACTION_HOME, 100)
            )
            views.setOnClickPendingIntent(
                R.id.widget_action_cash_in,
                createActionPendingIntent(context, ACTION_CASH_IN, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_action_cash_out,
                createActionPendingIntent(context, ACTION_CASH_OUT, 102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_action_new_bill,
                createActionPendingIntent(context, ACTION_NEW_BILL, 103)
            )
            views.setOnClickPendingIntent(
                R.id.widget_action_add_stock,
                createActionPendingIntent(context, ACTION_ADD_STOCK, 104)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createActionPendingIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                this.action = ACTION_WIDGET_CLICK
                putExtra(EXTRA_WIDGET_ACTION, action)
                // Set unique data URI so OS doesn't collapse distinct shortcut pending intents
                data = Uri.parse("aliedu://widget/${action.lowercase()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
