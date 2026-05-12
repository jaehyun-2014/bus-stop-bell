package com.manus.soundboard_flutter

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.soundboard/file"
    private val REQUEST_CODE_PICK_AUDIO = 1001

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "pickAudioFile" -> {
                    pickAudioFile(result)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun pickAudioFile(result: MethodChannel.Result) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_AUDIO)
            // 결과는 onActivityResult에서 처리
            this.result = result
        } catch (e: Exception) {
            result.error("UNAVAILABLE", "파일 앱을 열 수 없습니다", null)
        }
    }

    private var result: MethodChannel.Result? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val path = uri.path ?: ""
                result?.success(path)
            } else {
                result?.success("")
            }
        } else {
            result?.success("")
        }
        result = null
    }
}
