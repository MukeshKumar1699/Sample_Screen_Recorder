package com.example.samplescreenrecorder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.samplescreenrecorder.HBRecorderListenerImpl
import com.example.samplescreenrecorder.R
import com.example.samplescreenrecorder.helper.AppHelper
import com.example.samplescreenrecorder.helper.HBRecorderHelper
import com.example.samplescreenrecorder.helper.PermissionHelper.checkAudioPermissionGranted
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var hbRecorderListenerImpl: HBRecorderListenerImpl

    @Inject
    lateinit var hbRecorderHelper: HBRecorderHelper

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var recordIV: ImageView
    private lateinit var play_pause_IV: ImageView
    private lateinit var stopIV: ImageView
    private lateinit var micIV: ImageView
    private lateinit var timerTV: TextView

    private lateinit var params: LayoutParams

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var isAudioEnalbed: Boolean = false
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var elapsedTime = 0 // Elapsed time in seconds

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mResultCode = intent!!.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra("data")
        isAudioEnalbed = checkAudioPermissionGranted(this)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Inflate the custom layout for the overlay
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_view, null)

        recordIV = overlayView.findViewById(R.id.record_IV)
        play_pause_IV = overlayView.findViewById(R.id.play_pause_IV)
        stopIV = overlayView.findViewById(R.id.stop_IV)
        micIV = overlayView.findViewById(R.id.mic_IV)
        timerTV = overlayView.findViewById(R.id.timer_tv)

        params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.NO_GRAVITY
        params.x = 0
        params.y = 100

        overlayView.apply {
            setOnTouchListener { view, motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Capture initial touch coordinates
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = motionEvent.rawX
                        initialTouchY = motionEvent.rawY
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Calculate new position based on touch movement
                        params.x = initialX + (motionEvent.rawX - initialTouchX).toInt()
                        params.y = initialY + (motionEvent.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        view.performClick()
                        true
                    }

                    else -> false
                }
            }
        }
//
//        recordIV.apply {
//
//            setOnClickListener {
//                hbRecorderHelper.startRecordingScreen(mResultData, mResultCode)
//
//                // Start the timer
//                setRecordIVVisible(false)
//                setPlayPauseIVVisible(true)
//                setStopIVVisiblity(true)
//                startTimer()
//                Toast.makeText(this@OverlayService, "record Clicked", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        play_pause_IV.apply {
//
//            setOnClickListener {
//
//                if (hbRecorderHelper.screenRecorderIsBusy()) {
//                    hbRecorderHelper.pauseScreenRecording()
//
//                    if(hbRecorderHelper.checkIsRecordingPaused()) {
//                        setPlayPauseIcon(isPlay = true)
//                        Toast.makeText(this@OverlayService, "play Clicked", Toast.LENGTH_SHORT).show()
//                    }
//
//                } else {
//                    hbRecorderHelper.resumeScreenRecording()
//                    setPlayPauseIcon(isPlay = false)
//
//                    Toast.makeText(this@OverlayService, "resume Clicked", Toast.LENGTH_SHORT).show()
//
//                }
//            }
//
//        }

        micIV.apply {
            if (isAudioEnalbed) {
                micIV.setImageResource(R.drawable.on_mic_green)
            } else {
                micIV.setImageResource(R.drawable.off_mic)
            }
        }

        stopIV.apply {
            setOnClickListener {
                hbRecorderHelper.stopScreenRecording()

                if (!hbRecorderHelper.screenRecorderIsBusy()) {
                    // Update gallery depending on SDK Level
                    if (hbRecorderHelper.wasHbRecorderUriSet()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Call updateGalleryUri for Android 10 and above
                            hbRecorderHelper.updateGalleryUri()
                        } else {
                            // Call refreshGalleryFile for Android versions below Q
                            hbRecorderHelper.refreshGalleryFile()
                        }
                    } else {
                        // Handle case where URI was not set
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            hbRecorderHelper.refreshGalleryFile()
                        }
                    }
                }

                stopTimer()
                Toast.makeText(this@OverlayService, "Recording Stopped", Toast.LENGTH_SHORT).show()

                openFileLocator()


                stopSelf()
            }
        }

        windowManager.addView(overlayView, params)

        hbRecorderHelper.setAudioEnable(isAudioEnalbed)
        hbRecorderHelper.startRecordingScreen(mResultData, mResultCode)
        startTimer()

        return super.onStartCommand(intent, flags, startId)
    }

    // Open the recorded file after recording is finished
    private fun openFileLocator() {

        // Get the path to the recorded video file
        val filePathOrUri = hbRecorderHelper.getFilePathOrUri()

        filePathOrUri?.let {
            when (it) {
                is Uri -> {
                    // For Android 10 and above (URI case)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(it, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission for URI
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for launching activity from Service
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No app found to open the file", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                is String -> {
                    // For Android versions below Q (File path case)
                    val file = File(it)
                    val uri = Uri.fromFile(file) // Convert file path to URI
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant read permission for URI
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for launching activity from Service
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No app found to open the file", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                else -> {
                    Toast.makeText(this, "Invalid file or URI", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: Toast.makeText(
            this@OverlayService,
            "Please open any file explorer and open the Ride folder",
            Toast.LENGTH_SHORT
        ).show()


    }

    private fun startTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        elapsedTime = 0

        timerRunnable = object : Runnable {
            override fun run() {
                elapsedTime++
                timerTV.text = AppHelper.formatElapsedTime(elapsedTime)
                timerHandler?.postDelayed(this, 1000) // Update every second
            }
        }

        timerHandler?.post(timerRunnable!!)
    }


    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
    }

    private fun setPlayPauseIVVisible(visible: Boolean) {
//        play_pause_IV.visibility = if(visible) View.VISIBLE else View.GONE
    }

    private fun setPlayPauseIcon(isPlay: Boolean) {
        if (isPlay) {
            play_pause_IV.setImageResource(R.drawable.play)
        } else {
            play_pause_IV.setImageResource(R.drawable.pause)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
