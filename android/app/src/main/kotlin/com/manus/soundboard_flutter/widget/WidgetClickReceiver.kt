package com.manus.soundboard_flutter.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import java.io.File

class WidgetClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.manus.soundboard_flutter.WIDGET_CLICK") {
            val soundPath = intent.getStringExtra("sound_path")
            if (soundPath != null && soundPath.isNotEmpty()) {
                playSound(soundPath)
            }
        }
    }

    private fun playSound(soundPath: String) {
        try {
            val file = File(soundPath)
            if (file.exists()) {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(file.absolutePath)
                mediaPlayer.prepare()
                mediaPlayer.start()
                
                // 재생 완료 후 리소스 해제
                mediaPlayer.setOnCompletionListener {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
