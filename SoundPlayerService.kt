package com.manus.soundboard_flutter

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * 위젯 터치 시 소리를 재생하는 백그라운드 서비스입니다.
 *
 * 앱이 실행 중이 아닐 때도 홈 화면 위젯에서 소리를 재생할 수 있도록
 * 독립적인 서비스로 구현합니다. 재생이 완료되면 자동으로 서비스가 종료됩니다.
 */
class SoundPlayerService : Service() {

    companion object {
        private const val TAG = "SoundPlayerService"
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra(
            SoundWidgetProvider.EXTRA_WIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // 저장된 소리 파일 경로 읽기
        val prefs = WidgetPrefsHelper.getPrefs(this)
        val soundPath = prefs.getString(WidgetPrefsHelper.keySoundPath(appWidgetId), null)

        if (soundPath.isNullOrEmpty()) {
            Log.w(TAG, "위젯 $appWidgetId 의 소리 파일 경로가 없습니다.")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val soundFile = File(soundPath)
        if (!soundFile.exists()) {
            Log.e(TAG, "소리 파일을 찾을 수 없습니다: $soundPath")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        playSound(soundFile, startId)

        return START_NOT_STICKY
    }

    /**
     * MediaPlayer를 사용하여 소리 파일을 재생합니다.
     * 재생 완료 또는 오류 발생 시 서비스를 자동으로 종료합니다.
     */
    private fun playSound(soundFile: File, startId: Int) {
        // 이전에 재생 중인 소리가 있으면 중지
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                // 오디오 속성 설정 (미디어 재생용)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // 소리 파일 설정
                setDataSource(this@SoundPlayerService, Uri.fromFile(soundFile))
                prepare()

                // 재생 완료 시 서비스 종료
                setOnCompletionListener {
                    releaseMediaPlayer()
                    stopSelf(startId)
                }

                // 오류 발생 시 서비스 종료
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer 오류: what=$what, extra=$extra")
                    releaseMediaPlayer()
                    stopSelf(startId)
                    true
                }

                start()
                Log.d(TAG, "소리 재생 시작: ${soundFile.name}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "소리 파일 로드 실패: ${e.message}")
            stopSelf(startId)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaPlayer 상태 오류: ${e.message}")
            stopSelf(startId)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        super.onDestroy()
    }
}
