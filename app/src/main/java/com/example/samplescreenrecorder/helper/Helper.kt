package com.example.samplescreenrecorder.helper

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.samplescreenrecorder.helper.Const.REQUEST_CODE
import java.util.Locale

object PermissionHelper {

    fun checkAudioPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
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

    // Helper function to check if a permission is granted
    private fun isPermissionGranted(context: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Helper function to request permissions
    private fun requestPermissions(context: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(context, permissions, REQUEST_CODE)
    }

    fun checkPermissionsForAll(context: Activity): Boolean {
        val mandatoryPermissions = mutableListOf<String>()
        val optionalPermissions = mutableListOf<String>()

        // Define permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            if (!isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)) {
                mandatoryPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
                optionalPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 (Q) to Android 12 (S)
            if (!isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
                optionalPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            // For Android versions below Q
            if (!isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)) {
                optionalPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
            if (!isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mandatoryPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        // Handle mandatory permissions with rationale
        if (mandatoryPermissions.isNotEmpty()) {
            val showRationale = mandatoryPermissions.any {
                shouldShowRequestPermissionRationale(context, it)
            }

            if (showRationale) {
                // Show rationale dialog to the user
                showPermissionRationaleDialog(context, mandatoryPermissions)
            } else {
                // Request mandatory permissions directly
                requestPermissions(context, mandatoryPermissions.toTypedArray())
            }
            return mandatoryPermissions.isEmpty() && optionalPermissions.isEmpty()
        }

        // Request optional permissions directly
        if (optionalPermissions.isNotEmpty()) {
            requestPermissions(context, optionalPermissions.toTypedArray())
        }

        return true
    }

    // Helper function to check if rationale should be shown
    private fun shouldShowRequestPermissionRationale(
        context: Activity,
        permission: String
    ): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
    }

    // Helper function to show rationale dialog
    private fun showPermissionRationaleDialog(
        context: Activity,
        permissionsNeeded: List<String>
    ) {
        // Create a message string with the list of permissions needed
        val message = permissionsNeeded.joinToString(separator = ", ") { permission ->
            when (permission) {
                Manifest.permission.POST_NOTIFICATIONS -> "Notification permission"
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage permission"
                else -> "Unknown permissions"
            }
        }

        // Create the AlertDialog
        val builder = AlertDialog.Builder(context).apply {
            setTitle("Permissions Required")
            setMessage("The following permissions are required for the app to function properly: $message. Please grant them.")
            setPositiveButton("OK") { _, _ ->
                // Request permissions when the user clicks "OK"
                ActivityCompat.requestPermissions(
                    context,
                    permissionsNeeded.toTypedArray(),
                    REQUEST_CODE
                )
            }
            setNegativeButton("Cancel") { dialog, _ ->
                // Handle the case where user cancels the request
                dialog.dismiss()
            }
            setCancelable(false) // Prevent dismissing the dialog by tapping outside
        }

        // Show the dialog
        builder.create().show()
    }
}

object AppHelper {


    fun formatElapsedTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return if (hours > 0) {
            // Include hours if greater than 0
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            // Only include minutes and seconds if hours are not needed
            String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
        }
    }


}
