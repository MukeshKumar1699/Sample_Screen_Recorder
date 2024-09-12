package com.example.samplescreenrecorder.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.samplescreenrecorder.helper.Const.PERMISSION_REQ_ID_RECORD_AUDIO
import com.example.samplescreenrecorder.helper.Const.PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
import java.util.Locale

object Helper {

    fun checkSelfPermission(context: Activity, permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    fun checkAudioPermissionGranted(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissionsForNotificationAndAudio(context: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Const.PERMISSION_REQ_POST_NOTIFICATIONS
                ) && checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                )
            ) {
                return true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                )
            ) {
                return true
            }
        } else {
            if (checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                    PERMISSION_REQ_ID_RECORD_AUDIO
                ) &&
                checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
                )
            ) {
                return true
            }
        }
        return false
    }

    fun checkOverlayPermissionGranted(context: Context): Intent? {
        if (!Settings.canDrawOverlays(context)) {
            return Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            // Permission already granted
            return null
        }
    }

    fun formatElapsedTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
    }


    private fun showLongToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }
}