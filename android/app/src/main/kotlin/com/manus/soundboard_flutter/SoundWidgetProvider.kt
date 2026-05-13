package com.manus.soundboard_flutter



import android.app.PendingIntent

import android.appwidget.AppWidgetManager

import android.appwidget.AppWidgetProvider

import android.content.Context

import android.content.Intent

import android.widget.RemoteViews



class SoundWidgetProvider : AppWidgetProvider() {
  
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
      
        for (appWidgetId in appWidgetIds) {
          
            updateAppWidget(context, appWidgetManager, appWidgetId)
            
        }
        
    }
    

    
    companion object {
      
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: IntArray) {
          
            for (id in appWidgetId) {
              
                updateAppWidget(context, appWidgetManager, id)
                
            }
            
        }
        

        
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
          
            val views = RemoteViews(context.packageName, R.layout.widget_sound_layout)
            

            
            // 탭했을 때 소리 재생 서비스를 실행하도록 설정
            
            val intent = Intent(context, SoundPlayerService::class.java).apply {
              
                action = "PLAY_SOUND"
              
                putExtra("widgetId", appWidgetId)
                
            }
            

            
            val pendingIntent = PendingIntent.getService(
              
                context, 
              
                appWidgetId, 
              
                intent, 
              
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
              
            )
            

            
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            

            
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
        }
        
    }
    
}




































