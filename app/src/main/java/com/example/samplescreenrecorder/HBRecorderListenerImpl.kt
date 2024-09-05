package com.example.samplescreenrecorder

import android.util.Log
import com.hbisoft.hbrecorder.HBRecorderListener
import javax.inject.Inject

class HBRecorderListenerImpl @Inject constructor() : HBRecorderListener {

    override fun HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called")
    }

    override fun HBRecorderOnComplete() {
        Log.d("HBRecorder", "HBRecorderOnComplete: ")
    }

    override fun HBRecorderOnError(errorCode: Int, reason: String?) {
        Log.e("HBRecorderOnError", reason!!)
    }

    override fun HBRecorderOnPause() {
        Log.d("HBRecorder", "HBRecorderOnPause: ")
    }

    override fun HBRecorderOnResume() {
        Log.d("HBRecorder", "HBRecorderOnResume: ")
    }

}
