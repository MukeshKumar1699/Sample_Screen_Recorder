package com.example.samplescreenrecorder.viewmodel

import androidx.lifecycle.ViewModel
import com.example.samplescreenrecorder.helper.HBRecorderHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val hbRecorderHelper: HBRecorderHelper
) : ViewModel() {

    init {
        hbRecorderHelper.setUpHbRecorderCodecInfo()
    }

    fun screenRecorderIsBusy(): Boolean {
        return hbRecorderHelper.screenRecorderIsBusy()
    }

    fun stopScreenRecording() {
        hbRecorderHelper.stopScreenRecording()
    }

}
