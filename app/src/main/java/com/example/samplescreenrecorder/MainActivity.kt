package com.example.samplescreenrecorder

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.samplescreenrecorder.Const.PERMISSION_REQ_ID_RECORD_AUDIO
import com.example.samplescreenrecorder.Const.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
import com.example.samplescreenrecorder.Const.SCREEN_RECORD_REQUEST_CODE
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hbisoft.hbrecorder.Constants
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), HBRecorderListener{

    @Inject
    lateinit var hbRecorder: HBRecorder

    @Inject
    lateinit var mediaProjectionManager: MediaProjectionManager

    private lateinit var fab: FloatingActionButton

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //When the user returns to the application, some UI changes might be necessary,
        //check if recording is in progress and make changes accordingly
        if (viewModel.screenRecorderIsBusy()) {
            setFabIcon(false)
        }

        initViews()
        setOnClickListeners()

    }


    private fun initViews() {
        fab = findViewById(R.id.fab)
    }

    private fun setOnClickListeners() {
        fab.setOnClickListener {

            if (checkPermissionsForAllAndroidVersions()) {

                //check if recording is in progress
                //and stop it
                if (viewModel.screenRecorderIsBusy()) {
                    viewModel.stopScreenRecording()
                    setFabIcon(true)
                } else {

                    val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
                    startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
                    setFabIcon(false)
                }
            }

        }
    }

    private fun checkPermissionsForAllAndroidVersions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Const.PERMISSION_REQ_POST_NOTIFICATIONS
                ) && checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                )
            ) {
                return true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                )
            ) {
                return true
            }
        } else {
            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                ) && checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun setFabIcon(showPlayIcon: Boolean) {
        if (showPlayIcon) fab.setImageResource(R.drawable.play) else fab.setImageResource(R.drawable.stop)
    }

    override fun HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called")
    }

    override fun HBRecorderOnComplete() {

        setFabIcon(true)
        showLongToast("Saved Successfully")

        //Update gallery depending on SDK Level
        if (viewModel.wasHbRecorderUriSet()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                viewModel.updateGalleryUri()
            } else {
                viewModel.refreshGalleryFile()
            }
        } else {
            viewModel.refreshGalleryFile()
        }
    }



    override fun HBRecorderOnError(errorCode: Int, reason: String?) {

        // Error 38 happens when
        // - the selected video encoder is not supported
        // - the output format is not supported
        // - if another app is using the microphone

        //It is best to use device default
        if (errorCode == Constants.SETTINGS_ERROR) {
            showLongToast(getString(R.string.settings_not_supported_message))
        } else if (errorCode == Constants.MAX_FILE_SIZE_REACHED_ERROR) {
            showLongToast(getString(R.string.max_file_size_reached_message))
        } else {
            showLongToast(getString(R.string.general_recording_error_message))
            Log.e("HBRecorderOnError", reason!!)
        }

        setFabIcon(true)
    }

    override fun HBRecorderOnPause() {
        Log.d("HBRecorder", "HBRecorderOnPause: ")
    }

    override fun HBRecorderOnResume() {
        Log.d("HBRecorder", "HBRecorderOnResume: ")
    }

    //Check if permissions was granted
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    private fun showLongToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                viewModel.startRecordingScreen(data, resultCode)

            }
        }
    }
}

























