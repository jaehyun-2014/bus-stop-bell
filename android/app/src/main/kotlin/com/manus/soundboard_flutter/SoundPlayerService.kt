package com.manus.soundboard_flutter



import android.app.Service

import android.content.Intent

import android.media.MediaPlayer

import android.os.IBinder

import android.util.Log



class SoundPlayerService : Service() {
  
    private var mediaPlayer: MediaPlayer? = null
  

  
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      
        if (intent?.action == "PLAY_SOUND") {
          
            playSound()
            
        }
        
        return START_NOT_STICKY
      
    }
    

    
    private fun playSound() {
      
        try {
          
            mediaPlayer?.release()
            

            
            // Flutter assets에서 소리 파일을 가져와 재생 (기본값으로 bell.mp3 사용 시도)
            
            // 실제 구현에서는 SharedPreferences 등에 저장된 파일 경로를 사용
            
            val assetManager = assets
          
            val descriptor = assetManager.openFd("flutter_assets/assets/bell.mp3")
            

            
            mediaPlayer = MediaPlayer().apply {
              
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                
                prepare()
                
                start()
                
                setOnCompletionListener { 
                  
                    it.release()
                    
                    stopSelf()
                    
                }
                
            }
            
            descriptor.close()
            
        } catch (e: Exception) {
          
            Log.e("SoundPlayerService", "Error playing sound", e)
            
            stopSelf()
            
        }
        
    }
    

    
    override fun onBind(intent: Intent?): IBinder? = null
  

  
    override fun onDestroy() {
      
        mediaPlayer?.release()
        
        super.onDestroy()
        
    }
    
}










































