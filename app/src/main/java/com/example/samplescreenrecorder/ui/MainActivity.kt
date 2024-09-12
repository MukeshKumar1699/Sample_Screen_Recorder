package com.example.samplescreenrecorder.ui

import android.app.Activity
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.samplescreenrecorder.service.OverlayService
import com.example.samplescreenrecorder.R
import com.example.samplescreenrecorder.helper.Helper.checkOverlayPermissionGranted
import com.example.samplescreenrecorder.helper.Helper.checkPermissionsForNotificationAndAudio
import com.example.samplescreenrecorder.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var mediaProjectionManager: MediaProjectionManager

    private lateinit var fab: FloatingActionButton

    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab = findViewById(R.id.fab)
        setOnClickListeners()

    }

    private fun setOnClickListeners() {
        fab.setOnClickListener {

            if (isServiceRunning(OverlayService::class.java)) {
                Toast.makeText(this, "Overlay service is already running", Toast.LENGTH_SHORT).show()
            } else {
                val overlayIntent = checkOverlayPermissionGranted(this)
                if (overlayIntent != null) {
                    overlayPermissionLauncher.launch(overlayIntent)
                } else {
                    // Permission already granted

                    if (checkPermissionsForNotificationAndAudio(this)) {

                        if (viewModel.screenRecorderIsBusy()) {
                            viewModel.stopScreenRecording()
                            setFabIcon(true)
                        } else {
                            val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
                            screenRecordingLauncher.launch(permissionIntent)
                            setFabIcon(false)
                        }
                    }
                }
            }
        }
    }

    private fun setFabIcon(showPlayIcon: Boolean) {
        if (showPlayIcon) fab.setImageResource(R.drawable.play) else fab.setImageResource(R.drawable.stop)
    }


    private fun startOverlayService(data: Intent?, resultCode: Int) { val overlayService = Intent(this, OverlayService::class.java)

        if (!isServiceRunning(OverlayService::class.java)) {
            val overlayService = Intent(this, OverlayService::class.java)
            overlayService.putExtra("code", resultCode)
            overlayService.putExtra("data", data)
            startService(overlayService)
        } else {
            // Service is already running, so no need to start a new one
            Toast.makeText(this, "Overlay service is already running", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (Settings.canDrawOverlays(this)) {
            // Permission granted

            if (checkPermissionsForNotificationAndAudio(this)) {

                val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
                screenRecordingLauncher.launch(permissionIntent)
                setFabIcon(false)
            }

        } else {
            // Permission denied
            Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val screenRecordingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            startOverlayService(data, result.resultCode)
        }
    }

}

























