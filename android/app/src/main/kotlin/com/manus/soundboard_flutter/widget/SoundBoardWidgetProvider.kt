package com.manus.soundboard_flutter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.manus.soundboard_flutter.R

class SoundBoardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_soundboard)
            
            // 위젯 클릭 시 앱 실행
            val intent = Intent(context, WidgetClickReceiver::class.java)
            intent.action = "com.manus.soundboard_flutter.WIDGET_CLICK"
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
