package com.manus.soundboard_flutter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import java.io.File

/**
 * 홈 화면 사운드 위젯의 핵심 Provider 클래스입니다.
 *
 * 위젯이 추가/업데이트/삭제될 때의 생명주기를 관리하고,
 * 위젯 터치 시 소리 재생 브로드캐스트를 전송합니다.
 */
class SoundWidgetProvider : AppWidgetProvider() {

    companion object {
        // 위젯 터치 시 전송되는 브로드캐스트 액션
        const val ACTION_PLAY_SOUND = "com.manus.soundboard_flutter.ACTION_PLAY_SOUND"
        // 인텐트에 포함될 위젯 ID 키
        const val EXTRA_WIDGET_ID = "widget_id"

        /**
         * 특정 위젯의 UI를 업데이트합니다.
         * 저장된 설정(이미지 경로, 소리 경로, 이름)을 읽어 RemoteViews에 적용합니다.
         */
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = WidgetPrefsHelper.getPrefs(context)

            // 저장된 설정 읽기
            val imagePath = prefs.getString(WidgetPrefsHelper.keyImagePath(appWidgetId), null)
            val soundName = prefs.getString(WidgetPrefsHelper.keySoundName(appWidgetId), "터치하여 재생")

            // RemoteViews 생성
            val views = RemoteViews(context.packageName, R.layout.widget_sound_layout)

            // 이미지 설정
            if (!imagePath.isNullOrEmpty()) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    try {
                        // 메모리 효율을 위해 이미지 크기 조절 후 로드
                        val bitmap = decodeSampledBitmap(imagePath, 300, 300)
                        views.setImageViewBitmap(R.id.widget_image, bitmap)
                    } catch (e: Exception) {
                        // 이미지 로드 실패 시 기본 이미지 사용
                        views.setImageViewResource(R.id.widget_image, R.drawable.widget_default_image)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_image, R.drawable.widget_default_image)
                }
            } else {
                views.setImageViewResource(R.id.widget_image, R.drawable.widget_default_image)
            }

            // 라벨 설정
            views.setTextViewText(R.id.widget_label, soundName ?: "터치하여 재생")

            // 터치 이벤트 설정: 위젯 전체를 터치하면 소리 재생 브로드캐스트 전송
            val playIntent = Intent(context, SoundWidgetProvider::class.java).apply {
                action = ACTION_PLAY_SOUND
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
                // PendingIntent 충돌 방지를 위해 위젯 ID를 requestCode로 사용
                data = Uri.parse("widget://$appWidgetId")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 위젯 전체(루트 레이아웃)에 클릭 이벤트 연결
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 위젯 업데이트 적용
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * 메모리 효율적인 비트맵 디코딩 (큰 이미지를 위젯 크기에 맞게 축소)
         */
        private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            return BitmapFactory.decodeFile(path, options)
        }

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val (height, width) = options.outHeight to options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }

    /**
     * 위젯이 처음 생성되거나 업데이트가 필요할 때 호출됩니다.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /**
     * 위젯이 홈 화면에서 삭제될 때 호출됩니다.
     * 저장된 설정 데이터를 정리합니다.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = WidgetPrefsHelper.getPrefs(context)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(WidgetPrefsHelper.keyImagePath(appWidgetId))
            editor.remove(WidgetPrefsHelper.keySoundPath(appWidgetId))
            editor.remove(WidgetPrefsHelper.keySoundName(appWidgetId))
        }
        editor.apply()
    }

    /**
     * 브로드캐스트 수신: 위젯 터치 시 소리 재생 처리
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_PLAY_SOUND) {
            val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // 소리 재생 서비스 시작
                val serviceIntent = Intent(context, SoundPlayerService::class.java).apply {
                    putExtra(EXTRA_WIDGET_ID, appWidgetId)
                }
                context.startService(serviceIntent)
            }
        }
    }

    /**
     * 마지막 위젯 인스턴스가 삭제될 때 호출됩니다.
     */
    override fun onDisabled(context: Context) {
        // 필요한 경우 전체 정리 작업 수행
    }
}
