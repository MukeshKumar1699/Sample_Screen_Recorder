package com.example.samplescreenrecorder

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.samplescreenrecorder.service.OverlayService
import com.hbisoft.hbrecorder.HBRecorderListener
import javax.inject.Inject

class HBRecorderListenerImpl @Inject constructor(val context: Context) : HBRecorderListener {

    override fun HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called")
    }

    override fun HBRecorderOnComplete() {
        Log.d("HBRecorder", "HBRecorderOnComplete: ")
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        Log.e("HBRecorderOnError", reason!!)
        Toast.makeText(context, "Error recording your screen errorCode: $errorCode", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, OverlayService::class.java)
        context.stopService(intent)
    }

    override fun HBRecorderOnPause() {
        Log.d("HBRecorder", "HBRecorderOnPause: ")
    }

    override fun HBRecorderOnResume() {
        Log.d("HBRecorder", "HBRecorderOnResume: ")
    }

}
