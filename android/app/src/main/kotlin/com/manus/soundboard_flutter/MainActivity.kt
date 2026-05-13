package com.manus.soundboard_flutter

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.io.FileOutputStream

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

    private fun getFilePathFromUri(uri: Uri): String? {
        return try {
            // 먼저 직접 경로 확인
            if (uri.scheme == "file") {
                return uri.path
            }

            // MediaStore를 통한 경로 확인
            val projection = arrayOf(MediaStore.Audio.Media.DATA)
            val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    return it.getString(columnIndex)
                }
            }

            // 위 방법들이 실패하면 파일을 캐시 디렉토리로 복사
            val fileName = getFileNameFromUri(uri) ?: "audio_${System.currentTimeMillis()}.mp3"
            val cacheFile = File(cacheDir, fileName)
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    return it.getString(nameIndex)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                val path = getFilePathFromUri(uri) ?: ""
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
