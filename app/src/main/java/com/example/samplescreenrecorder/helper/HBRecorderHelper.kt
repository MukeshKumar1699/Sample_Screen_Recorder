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

    fun wasHbRecorderUriSet(): Boolean {
        return hbRecorder.wasUriSet()
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


    //Get/Set the selected settings
    private fun quickSettings() {
        hbRecorder.setAudioBitrate(128000)
        hbRecorder.setAudioSamplingRate(44100)
        hbRecorder.recordHDVideo(true)
        hbRecorder.isAudioEnabled(true)
        //Customise Notification
//        hbRecorder.setNotificationSmallIcon(R.drawable.ic_launcher_foreground)
        //hbRecorder.setNotificationSmallIconVector(R.drawable.ic_baseline_videocam_24);
        hbRecorder.setNotificationTitle("Recording your Screen")
        hbRecorder.setNotificationDescription("Drag down to Stop Recording")
    }

    fun startRecordingScreen(data: Intent?, resultCode: Int) {

        quickSettings()
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
        val filename: String = generateFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Ride Scan")
            contentValues.put(MediaStore.Video.Media.TITLE, filename)
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            contentValues.put(
                MediaStore.MediaColumns.MIME_TYPE,
                getMimeTypeForOutputFormat("DEFAULT")
            )

            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            mUri?.let {
                //FILE NAME SHOULD BE THE SAME
                hbRecorder.fileName = filename
                hbRecorder.setOutputUri(mUri)
            }
        } else {
            createFolder()
            hbRecorder.setOutputPath(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    .toString() + "/Ride Scan"
            )
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