package com.manus.soundboard_flutter.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.graphics.BitmapFactory
import com.manus.soundboard_flutter.R
import java.io.File

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
            
            // SharedPreferences에서 카드 정보 읽기
            val sharedPref = context.getSharedPreferences("flutter.soundboard", Context.MODE_PRIVATE)
            val cardsJson = sharedPref.getString("flutter.soundCards", null)
            
            if (!cardsJson.isNullOrEmpty()) {
                try {
                    // JSON 파싱 (간단한 방식)
                    val firstCardMatch = Regex(""""imagePath":"([^"]+)""").find(cardsJson)
                    val soundPathMatch = Regex(""""soundPath":"([^"]+)""").find(cardsJson)
                    
                    val imagePath = firstCardMatch?.groupValues?.get(1)
                    val soundPath = soundPathMatch?.groupValues?.get(1)
                    
                    if (!imagePath.isNullOrEmpty()) {
                        val imageFile = File(imagePath)
                        if (imageFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            views.setImageViewBitmap(R.id.widget_image, bitmap)
                        }
                    }
                    
                    // 클릭 리스너 설정
                    val intent = Intent(context, WidgetClickReceiver::class.java)
                    intent.action = "com.manus.soundboard_flutter.WIDGET_CLICK"
                    intent.putExtra("sound_path", soundPath ?: "")
                    
                    val pendingIntent = android.app.PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    
                    views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
