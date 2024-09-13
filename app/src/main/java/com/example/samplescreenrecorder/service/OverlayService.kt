package com.example.samplescreenrecorder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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
import com.example.samplescreenrecorder.helper.HBRecorderHelper
import com.example.samplescreenrecorder.helper.Helper
import com.example.samplescreenrecorder.helper.Helper.checkAudioPermissionGranted
import dagger.hilt.android.AndroidEntryPoint
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

    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var elapsedTime = 0 // Elapsed time in seconds

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        mResultCode = intent!!.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra("data")

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

        stopIV.apply {

            setOnClickListener {
                hbRecorderHelper.stopScreenRecording()

                if (!hbRecorderHelper.screenRecorderIsBusy()) {
                    //Update gallery depending on SDK Level
                    if (hbRecorderHelper.wasHbRecorderUriSet()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            hbRecorderHelper.updateGalleryUri()
                        } else {
                            hbRecorderHelper.refreshGalleryFile()
                        }
                    } else {
                        hbRecorderHelper.refreshGalleryFile()
                    }
                }

                stopTimer()
                setRecordIVVisible(true)
                setPlayPauseIVVisible(false)
                setStopIVVisiblity(false)

                Toast.makeText(this@OverlayService, "stop Button Clicked", Toast.LENGTH_SHORT)
                    .show()

                stopSelf()
            }
        }

        windowManager.addView(overlayView, params)

        hbRecorderHelper.setAudioEnable(checkAudioPermissionGranted(this))
        hbRecorderHelper.startRecordingScreen(mResultData, mResultCode)
        startTimer()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        elapsedTime = 0

        timerRunnable = object : Runnable {
            override fun run() {
                elapsedTime++
                timerTV.text = Helper.formatElapsedTime(elapsedTime)
                timerHandler?.postDelayed(this, 1000) // Update every second
            }
        }

        timerHandler?.post(timerRunnable!!)
    }


    private fun stopTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
    }

    private fun setStopIVVisiblity(visible: Boolean) {
        stopIV.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setRecordIVVisible(visible: Boolean) {
        recordIV.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setMicEnabledIcon() {
        micIV.setImageResource(R.drawable.on_mic)
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


    override fun onCreate() {
        super.onCreate()

        Toast.makeText(this, "Overlay Service created", Toast.LENGTH_SHORT).show()

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
