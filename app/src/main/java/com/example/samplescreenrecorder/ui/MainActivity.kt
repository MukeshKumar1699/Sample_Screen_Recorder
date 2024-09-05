package com.example.samplescreenrecorder.ui

import android.app.Activity
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
import com.example.samplescreenrecorder.OverlayService
import com.example.samplescreenrecorder.R
import com.example.samplescreenrecorder.helper.Helper.checkOverlayPermissionGranted
import com.example.samplescreenrecorder.helper.Helper.checkPermissionsForAllAndroidVersions
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

        // Start file creation intent
        requestWriteAccess()

    }

    private fun setOnClickListeners() {
        fab.setOnClickListener {

            val overlayIntent = checkOverlayPermissionGranted(this)
            if(overlayIntent != null) {
                overlayPermissionLauncher.launch(overlayIntent)
            } else {
                // Permission already granted

                if (checkPermissionsForAllAndroidVersions(this)) {

                    if (viewModel.screenRecorderIsBusy()) {
                        viewModel.stopScreenRecording()
                        setFabIcon(true)
                    }else {
                        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
                        screenRecordingLauncher.launch(permissionIntent)
                        setFabIcon(false)
                    }
                }
            }
        }
    }

    private fun requestWriteAccess() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "new_image.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyApp/")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            writeFileToUri(it)
        } ?: run {
            // Handle error
        }
    }

    private fun writeFileToUri(uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val data = "Your file content".toByteArray()
            outputStream.write(data)
        } ?: run {
            // Handle error
        }
    }

    private fun setFabIcon(showPlayIcon: Boolean) {
        if (showPlayIcon) fab.setImageResource(R.drawable.play) else fab.setImageResource(R.drawable.stop)
    }


    private fun startOverlayService(data: Intent?, resultCode: Int) {
        val overlayService = Intent(this, OverlayService::class.java)
        overlayService.putExtra("code", resultCode)
        overlayService.putExtra("data", data)
        startService(overlayService)
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (Settings.canDrawOverlays(this)) {
            // Permission granted

            if (checkPermissionsForAllAndroidVersions(this)) {

//                if (viewModel.screenRecorderIsBusy()) {
//                    startOverlayService()
//                }else {
                    val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
                    screenRecordingLauncher.launch(permissionIntent)
                    setFabIcon(false)
//                }
            }

        } else {
            // Permission denied
            Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val screenRecordingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if ( result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            startOverlayService(data, result.resultCode)
        }
    }

}

























