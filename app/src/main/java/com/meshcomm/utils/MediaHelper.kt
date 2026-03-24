package com.meshcomm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object MediaHelper {
    private const val TAG = "MediaHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    // --- Audio ---

    fun startRecording(context: Context, fileName: String): String? {
        val folder = File(context.filesDir, "MeshComm/audio")
        if (!folder.exists()) folder.mkdirs()
        val filePath = File(folder, "$fileName.m4a").absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(filePath)
            try {
                prepare()
                start()
                Log.d(TAG, "Recording started: $filePath")
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
                return null
            }
        }
        return filePath
    }

    fun stopRecording(): Long {
        var duration: Long = 0
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "stop() failed", e)
            }
        }
        mediaRecorder = null
        return duration // Ideally calculate duration
    }

    fun playAudio(filePath: String, onComplete: () -> Unit = {}) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener { 
                    onComplete()
                    stopPlayback()
                }
            } catch (e: IOException) {
                Log.e(TAG, "playAudio() failed", e)
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // --- Image ---

    fun compressImage(context: Context, uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val folder = File(context.filesDir, "MeshComm/images")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "img_${System.currentTimeMillis()}.jpg")

            var quality = 90
            var stream = ByteArrayOutputStream()
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            
            // Further compress if > 500KB
            while (stream.toByteArray().size > 500 * 1024 && quality > 10) {
                quality -= 10
                stream = ByteArrayOutputStream()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }

            FileOutputStream(file).use { it.write(stream.toByteArray()) }
            Log.d(TAG, "Image compressed and saved: ${file.absolutePath}, size: ${file.length() / 1024}KB")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "compressImage() failed", e)
            return null
        }
    }
}
