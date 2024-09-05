/*
package com.example.samplescreenrecorder

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import com.hbisoft.hbrecorder.Constants
import com.hbisoft.hbrecorder.NotificationReceiver
import com.hbisoft.hbrecorder.ScreenRecordService
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Objects

class RecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mIntent: Intent? = null

    private var screenDensity: Int = 0
    private var hasMaxFileBeenReached = false
    private var maxFileSize = Constants.NO_SPECIFIED_MAX_SIZE.toLong()
    private var returnedUri: Uri? = null

    private val isVideoHD = false
    private var name: String? = null

    private var path: String? = null
    private var filePath: String? = null
    private var fileName: String? = null



    val channelId = "ScreenRecordServiceChannel"
    val channelName = "Screen Record Service"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var isAction = false

        intent?.let {

            if (intent.action != null) {
                isAction = true
            }

            if (isAction) {
                if (intent.action == "pause") {
                    //Pause Recording
                    pauseRecording()
                } else if (intent.action == "resume") {
                    //Pause Recording
                    resumeRecording()
                }
            } else {
                //Start Recording

                hasMaxFileBeenReached = false
                mIntent = intent
                maxFileSize = intent.getLongExtra(
                    Constants.MAX_FILE_SIZE_KEY,
                    Constants.NO_SPECIFIED_MAX_SIZE.toLong()
                )

                screenDensity = resources.displayMetrics.densityDpi
                path = intent.getStringExtra("path")
                name = intent.getStringExtra("fileName")
                setUpNotification()

                if (intent.getStringExtra("mUri") != null) {
                    returnedUri = Uri.parse(intent.getStringExtra("mUri"))
                }

                try {
                    initRecorder()
                } catch (e: java.lang.Exception) {
                    val receiver =
                        intent.getParcelableExtra<ResultReceiver>(ScreenRecordService.BUNDLED_LISTENER)
                    val bundle = Bundle()
                    bundle.putString(Constants.ERROR_REASON_KEY, Log.getStackTraceString(e))
                    receiver?.send(Activity.RESULT_OK, bundle)
                }


                try {
                   initMediaProjection()
                }catch (e: Exception) {

                    val receiver = intent.getParcelableExtra(
                        ScreenRecordService.BUNDLED_LISTENER,
                        ResultReceiver::class.java
                    )

                    val bundle = Bundle()
                    bundle.putString(Constants.ERROR_REASON_KEY, Log.getStackTraceString(e))
                    receiver?.send(Activity.RESULT_OK, bundle)

                }

                startRecording()
                return START_NOT_STICKY
            }
        } ?: stopSelf(startId)
        return START_STICKY
    }

    @Throws(java.lang.Exception::class)
    private fun initRecorder() {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        val curTime = formatter.format(curDate).replace(" ", "")
        val videoQuality = "HD"

        filePath = path + "/" + name + ".mp4"

        fileName = "$name.mp4"

        mediaRecorder = MediaRecorder()

        //        if (isAudioEnabled) {
//            mMediaRecorder.setAudioSource(audioSourceAsInt);
//        }
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder?.setOutputFormat()
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)


    }



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun initMediaProjection(): Int {

        val resultCode = mIntent?.getIntExtra("result_code", Activity.RESULT_OK) ?: Activity.RESULT_CANCELED
        val mediaProjectionData = mIntent?.getParcelableExtra<Intent>("media_projection", Intent::class.java)

        mediaProjectionData?.let {
            mediaProjection = (Objects.requireNonNull<Any>(
                getSystemService(
                    MEDIA_PROJECTION_SERVICE
                )
            ) as MediaProjectionManager).getMediaProjection(resultCode, mediaProjectionData)

            val handler = Handler(Looper.getMainLooper())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                    }
                }, handler)
            } else {
                mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                    // Nothing
                    // We don't use it but register it to avoid runtime error from SDK 34+.
                }, handler)
            }
            return START_STICKY
        }

        if (mediaProjectionData == null || resultCode != Activity.RESULT_OK) {
            Log.e(
                "ScreenRecordService",
                "MediaProjection data is null or resultCode is not OK"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        mediaProjection =
            (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                .getMediaProjection(resultCode, mediaProjectionData)

        if (mediaProjection == null) {
            Log.e("ScreenRecordService", "MediaProjection is null")
            stopSelf()
            return START_NOT_STICKY
        }

       return START_NOT_STICKY
    }

    private fun setUpNotification() {

        createNotificationChannel()
        val notification = createNotification()
        startFgs(1, notification)
    }

    private fun createNotificationChannel() {


        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW).apply {
            description = "Channel for screen recording service"
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {

        val notificationTitle: String = getString(R.string.stop_recording_notification_message)
        val notificationDescription: String = getString(R.string.stop_recording_notification_title)
        val notificationButtonText: String = getString(R.string.stop_recording)

        val myIntent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE)

        val actionStart = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.record),
            notificationButtonText,
            pendingIntent
        ).build()

        val actionStop = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.stop),
            notificationButtonText,
            pendingIntent
        ).build()

        val actionPause = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.pause),
            notificationButtonText,
            pendingIntent
        ).build()

        val actionResume = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.record),
            notificationButtonText,
            pendingIntent
        ).build()

        //Modify notification badge
        var notification: Notification = Notification.Builder(applicationContext, channelId)
            .setOngoing(true)
            .setSmallIcon(R.drawable.record_icon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationDescription)
            .addAction(actionStart)
            .addAction(actionStop)
            .addAction(actionResume)
            .addAction(actionPause)

            .build()

        return notification
    }

    private fun startFgs(notificationId: Int, notificaton: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notificationId,
                notificaton,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId,
                notificaton,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(notificationId, notificaton)
        }
    }
    private fun startRecording() {
        Log.d("ScreenRecordService", "Configuring MediaRecorder")
        configureMediaRecorder()

        val metrics = resources.displayMetrics
        val videoWidth = metrics.widthPixels
        val videoHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecordService",
            videoWidth, videoHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )

        if (virtualDisplay == null) {
            Log.e("ScreenRecordService", "VirtualDisplay is null")
            stopSelf()
            return
        }

        try {
            mediaRecorder?.start()
            Log.d("ScreenRecordService", "Screen recording started successfully")
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "Failed to start MediaRecorder: ${e.message}")
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("ScreenRecordService", "Error stopping MediaRecorder: ${e.message}")
        }

        mediaProjection?.stop()
        virtualDisplay?.release()

        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    private fun configureMediaRecorder() {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "ridescan")
        if (!publicDir.exists()) {
            publicDir.mkdirs()
        }

        val videoUri = "${publicDir.absolutePath}/screen_record_${System.currentTimeMillis()}.mp4"
        Log.d("ScreenRecordService", "Setting up MediaRecorder with output file: $videoUri")

        mediaRecorder = MediaRecorder().apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(videoUri)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                val metrics = resources.displayMetrics
                val videoWidth = metrics.widthPixels
                val videoHeight = metrics.heightPixels

                setVideoSize(videoWidth, videoHeight)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(5 * 1024 * 1024)

                prepare()
                Log.d("ScreenRecordService", "MediaRecorder prepared successfully")
            } catch (e: Exception) {
                Log.e("ScreenRecordService", "Failed to configure MediaRecorder: ${e.message}")
                stopSelf()
            }
        }
    }

    //Pause Recording
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun pauseRecording() {
        mediaRecorder?.pause()
        val bundle = Bundle()
        bundle.putString(Constants.ON_PAUSE_KEY, Constants.ON_PAUSE)
        mIntent?.getParcelableExtra(
            ScreenRecordService.BUNDLED_LISTENER,
            ResultReceiver::class.java
        )?.send(Activity.RESULT_OK, bundle)
    }

    //Resume Recording
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun resumeRecording() {
        mediaRecorder?.resume()
        val bundle = Bundle()
        bundle.putString(Constants.ON_RESUME_KEY, Constants.ON_RESUME)
        mIntent?.getParcelableExtra(
            ScreenRecordService.BUNDLED_LISTENER,
            ResultReceiver::class.java
        )?.send(Activity.RESULT_OK, bundle)
    }




    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
*/
