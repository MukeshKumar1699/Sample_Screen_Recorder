package com.hbisoft.hbrecorder;

import android.os.Build;

import androidx.annotation.RequiresApi;

public interface HBRecorderListener {
    void HBRecorderOnStart();
    void HBRecorderOnComplete();
    void HBRecorderOnError(int errorCode, String reason);
    void HBRecorderOnPause();
    void HBRecorderOnResume();
}
