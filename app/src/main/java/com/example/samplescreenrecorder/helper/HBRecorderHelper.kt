package com.example.samplescreenrecorder.helper

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderCodecInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class HBRecorderHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hbRecorder: HBRecorder,
    private val resolver: ContentResolver,
    private val contentValues: ContentValues
) {

    private var mUri: Uri? = null
    private var filePathOrUri: Any? = null // This will hold either a String (file path) or Uri


    fun wasHbRecorderUriSet(): Boolean {
        return hbRecorder.wasUriSet()
    }

    fun getFilePathOrUri(): Any? {
        return filePathOrUri
    }

    fun setUpHbRecorderCodecInfo() {
        // Examples of how to use the HBRecorderCodecInfo class to get codec info
        val hbRecorderCodecInfo = HBRecorderCodecInfo()
        val mWidth = hbRecorder.defaultWidth
        val mHeight = hbRecorder.defaultHeight
        val mMimeType = "video/avc"
        val mFPS = 30

        if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {

            val defaultVideoEncoder = hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType)
            val isSizeAndFramerateSupported = hbRecorderCodecInfo.isSizeAndFramerateSupported(
                mWidth,
                mHeight,
                mFPS,
                mMimeType,
                Configuration.ORIENTATION_PORTRAIT
            )
        } else {
            Log.e("HBRecorderCodecInfo", "MimeType not supported")
        }

    }

    fun startRecordingScreen(data: Intent?, resultCode: Int) {

        //Set file path or Uri depending on SDK version
        setOutputPath()
        //Start screen recording
        hbRecorder.startScreenRecording(data, resultCode)

    }

    fun screenRecorderIsBusy(): Boolean {
        return hbRecorder.isBusyRecording
    }

    fun stopScreenRecording() {
        hbRecorder.stopScreenRecording()
        finalizeRecording()
    }

    fun isAudioRecordingEnabled(): Boolean {
        return hbRecorder.isAudioEnabled
    }

    fun setAudioEnable(enable: Boolean) {
        hbRecorder.setAudioEnable(enable)
    }

    fun startAudioRecording() {
        hbRecorder.startAudioRecording()
    }

    fun pauseScreenRecording() {
        hbRecorder.pauseScreenRecording()
    }

    fun checkIsRecordingPaused(): Boolean {
        return hbRecorder.isRecordingPaused
    }

    fun resumeScreenRecording() {
        hbRecorder.resumeScreenRecording()
    }

    private fun setOutputPath() {
        val filename: String = generateFileName() // Generate a unique file name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10 (Q) and above
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Ride Scan") // Folder path
                put(MediaStore.Video.Media.TITLE, filename) // Video title
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename) // Display name
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeTypeForOutputFormat("DEFAULT")) // MIME type
                put(MediaStore.Video.Media.IS_PENDING, 1) // Mark file as pending
            }

            // Insert the new video into MediaStore
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            mUri?.let {
                // Assign filename and URI to HBRecorder
                hbRecorder.fileName = filename
                hbRecorder.setOutputUri(mUri)
                filePathOrUri = mUri // Store the URI for later use
            }
        } else {
            // For Android versions below Q, create the folder manually and set the output path
            createFolder() // Function that creates the folder if it doesn't exist
            val outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .toString() + "/Ride Scan/$filename"

            hbRecorder.setOutputPath(outputPath) // Set output path for HBRecorder
            filePathOrUri = outputPath // Store the file path for later use
        }
    }    // Ensure to call this after the recording is complete to make the file visible
    private fun finalizeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mUri != null) {
            // Update IS_PENDING to 0 to make the file visible in MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(mUri!!, contentValues, null, null)
        }
    }


    //Generate a timestamp to be used as a file name
    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate).replace(" ", "")
    }

    private fun getMimeTypeForOutputFormat(outputFormat: String?): String {
        var mimetype = "video/mp4"
        if (outputFormat != null) {
            mimetype = when (outputFormat) {
                "0" -> "video/mp4"
                "1" -> "video/mp4"
                "2" -> "video/3gpp"
                "3" -> "video/webm"
                else -> "video/mp4"
            }
        }
        return mimetype
    }

    private fun createFolder() {
        val f1 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Ride Scan"
        )
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created")
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun updateGalleryUri() {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        mUri?.let { resolver.update(it, contentValues, null, null) }
    }

    fun refreshGalleryFile() {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(hbRecorder.filePath), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }

}