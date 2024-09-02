package com.example.samplescreenrecorder

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
import androidx.lifecycle.ViewModel
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderCodecInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hbRecorder: HBRecorder,
    private val resolver: ContentResolver,
    private val contentValues: ContentValues
) : ViewModel() {

    var wasHDSelected: Boolean = true
    var isAudioEnabled: Boolean = true

    var mUri: Uri? = null

    init {
        setUpHbRecorderCodecInfo()
    }

    fun wasHbRecorderUriSet(): Boolean {
        return hbRecorder.wasUriSet()
    }

    private fun setUpHbRecorderCodecInfo() {
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

           /* Log.e("HBRecorderCodecInfo", "defaultVideoEncoder for ($mMimeType) -> $defaultVideoEncoder")
            Log.e("HBRecorderCodecInfo", "MaxSupportedFrameRate -> " + hbRecorderCodecInfo.getMaxSupportedFrameRate(mWidth, mHeight, mMimeType))
            Log.e("HBRecorderCodecInfo", "MaxSupportedBitrate -> " + hbRecorderCodecInfo.getMaxSupportedBitrate(mMimeType))
            Log.e("HBRecorderCodecInfo", "isSizeAndFramerateSupported @ Width = $mWidth Height = $mHeight FPS = $mFPS -> $isSizeAndFramerateSupported")
            Log.e("HBRecorderCodecInfo", "isSizeSupported @ Width = " + mWidth + " Height = " + mHeight + " -> " + hbRecorderCodecInfo.isSizeSupported(mWidth, mHeight, mMimeType))
            Log.e("HBRecorderCodecInfo", "Default Video Format = " + hbRecorderCodecInfo.defaultVideoFormat)

            val supportedVideoMimeTypes = hbRecorderCodecInfo.supportedVideoMimeTypes
            for ((key, value) in supportedVideoMimeTypes) {
                Log.e("HBRecorderCodecInfo", "Supported VIDEO encoders and mime types : $key -> $value")
            }

            val supportedAudioMimeTypes = hbRecorderCodecInfo.supportedAudioMimeTypes
            for ((key, value) in supportedAudioMimeTypes) {
                Log.e("HBRecorderCodecInfo", "Supported AUDIO encoders and mime types : $key -> $value")
            }

            val supportedVideoFormats = hbRecorderCodecInfo.supportedVideoFormats
            for (j in supportedVideoFormats.indices) {
                Log.e("HBRecorderCodecInfo", "Available Video Formats : " + supportedVideoFormats[j])
            }*/

        } else {
            Log.e("HBRecorderCodecInfo", "MimeType not supported")
        }

    }

    fun startRecordingScreen(data: Intent?, resultCode: Int) {
        quickSettings()
        //Set file path or Uri depending on SDK version
        setOutputPath()
        //Start screen recording
        hbRecorder.startScreenRecording(data, resultCode)

    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
    private fun setOutputPath() {
        val filename: String = generateFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {


            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder")
            contentValues.put(MediaStore.Video.Media.TITLE, filename)
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            contentValues.put(
                MediaStore.MediaColumns.MIME_TYPE,
                getMimeTypeForOutputFormat("DEFAULT")
            )

            mUri = resolver!!.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            mUri?.let {
                //FILE NAME SHOULD BE THE SAME
                hbRecorder.fileName = filename
                hbRecorder.setOutputUri(mUri)
            }
        } else {
            createFolder()
            hbRecorder.setOutputPath(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    .toString() + "/HBRecorder"
            )
        }
    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private fun createFolder() {
        val f1 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "HBRecorder"
        )
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created")
            }
        }
    }

    // Passing the MIME_TYPE to ContentValues() depending on what output format was selected
    // This is just to demonstrate for the demo app - more can be added
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


    //Generate a timestamp to be used as a file name
    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate).replace(" ", "")
    }

    fun screenRecorderIsBusy(): Boolean {
        return hbRecorder.isBusyRecording
    }

    fun stopScreenRecording() {
        hbRecorder.stopScreenRecording()
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
        hbRecorder.setNotificationTitle( getStringResource(R.string.stop_recording_notification_title))
        hbRecorder.setNotificationDescription(getStringResource(R.string.stop_recording_notification_message))
    }

    fun getStringResource(id: Int): String {
        return context.getString(id)
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun updateGalleryUri() {
        contentValues?.clear()
        contentValues?.put(MediaStore.Video.Media.IS_PENDING, 0)
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
