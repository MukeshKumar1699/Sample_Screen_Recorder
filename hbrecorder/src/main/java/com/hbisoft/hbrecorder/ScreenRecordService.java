package com.hbisoft.hbrecorder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.FileDescriptor;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

import static com.hbisoft.hbrecorder.Constants.ERROR_KEY;
import static com.hbisoft.hbrecorder.Constants.ERROR_REASON_KEY;
import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_REACHED_ERROR;
import static com.hbisoft.hbrecorder.Constants.MAX_FILE_SIZE_KEY;
import static com.hbisoft.hbrecorder.Constants.NO_SPECIFIED_MAX_SIZE;
import static com.hbisoft.hbrecorder.Constants.ON_COMPLETE;
import static com.hbisoft.hbrecorder.Constants.ON_COMPLETE_KEY;
import static com.hbisoft.hbrecorder.Constants.ON_PAUSE;
import static com.hbisoft.hbrecorder.Constants.ON_PAUSE_KEY;
import static com.hbisoft.hbrecorder.Constants.ON_RESUME;
import static com.hbisoft.hbrecorder.Constants.ON_RESUME_KEY;
import static com.hbisoft.hbrecorder.Constants.ON_START;
import static com.hbisoft.hbrecorder.Constants.ON_START_KEY;
import static com.hbisoft.hbrecorder.Constants.SETTINGS_ERROR;

/**
 * Created by HBiSoft on 13 Aug 2019
 * Copyright (c) 2019 . All rights reserved.
 */

public class ScreenRecordService extends Service {

    private static final String TAG = "ScreenRecordService";
    private long maxFileSize = NO_SPECIFIED_MAX_SIZE;
    private boolean hasMaxFileBeenReached = false;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;

    private boolean isAudioEnabled;
    private String path;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;

    private VirtualDisplay mVirtualDisplay;

    private String name;
    private static String filePath;
    private static String fileName;

    private int orientationHint;

    public final static String BUNDLED_LISTENER = "listener";
    private Uri returnedUri = null;
    private Intent mIntent;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        boolean isAction = false;

        //Check if there was an action called
        if (intent != null) {

            if (intent.getAction() != null) {
                isAction = true;
            }

            //If there was an action, check what action it was
            //Called when recording should be paused or resumed
            if (isAction) {
                //Pause Recording
                if (intent.getAction().equals("pause")) {
                    pauseRecording();
                }

                //Resume Recording
                else if (intent.getAction().equals("resume")) {
                    resumeRecording();
                }
            }
            //Start Recording
            else {
                //Get intent extras
                hasMaxFileBeenReached = false;
                mIntent = intent;
                maxFileSize = intent.getLongExtra(MAX_FILE_SIZE_KEY, NO_SPECIFIED_MAX_SIZE);

                mResultCode = intent.getIntExtra("code", -1);
                mResultData = intent.getParcelableExtra("data");

                orientationHint = intent.getIntExtra("orientation", 400);
                mScreenWidth = intent.getIntExtra("width", 0);
                mScreenHeight = intent.getIntExtra("height", 0);
                mScreenDensity = intent.getIntExtra("density", 1);

                if (intent.getStringExtra("mUri") != null) {
                    returnedUri = Uri.parse(intent.getStringExtra("mUri"));
                }

                if (mScreenHeight == 0 || mScreenWidth == 0) {
                    HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();
                    hbRecorderCodecInfo.setContext(this);
                    mScreenHeight = hbRecorderCodecInfo.getMaxSupportedHeight();
                    mScreenWidth = hbRecorderCodecInfo.getMaxSupportedWidth();
                }

                isAudioEnabled = intent.getBooleanExtra("audio", false);

                path = intent.getStringExtra("path");
                name = intent.getStringExtra("fileName");

                filePath = name;

                //Notification
               createNotification();

                //Init MediaRecorder
                try {
                    initRecorder();
                } catch (Exception e) {
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));

                    setupError(intent, bundle);

                }

                //Init MediaProjection
                try {
                    initMediaProjection();
                } catch (Exception e) {
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    setupError(intent, bundle);

                }

                //Init VirtualDisplay
                try {
                    initVirtualDisplay();
                } catch (Exception e) {
                    Bundle bundle = new Bundle();
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    setupError(intent, bundle);

                }

                mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                    @Override
                    public void onError(MediaRecorder mediaRecorder, int what, int extra) {
                        if (what == 268435556 && hasMaxFileBeenReached) {
                            // Benign error b/c recording is too short and has no frames. See SO: https://stackoverflow.com/questions/40616466/mediarecorder-stop-failed-1007
                            return;
                        }
                        Bundle bundle = new Bundle();
                        bundle.putInt(ERROR_KEY, SETTINGS_ERROR);
                        bundle.putString(ERROR_REASON_KEY, String.valueOf(what));
                        setupError(intent, bundle);

                    }
                });

                mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                    @Override
                    public void onInfo(MediaRecorder mr, int what, int extra) {
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                            hasMaxFileBeenReached = true;
                            Log.i(TAG, String.format(Locale.US, "onInfoListen what : %d | extra %d", what, extra));
                            ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
                            Bundle bundle = new Bundle();
                            bundle.putInt(ERROR_KEY, MAX_FILE_SIZE_REACHED_ERROR);
                            bundle.putString(ERROR_REASON_KEY, getString(R.string.max_file_reached));
                            if (receiver != null) {
                                receiver.send(Activity.RESULT_OK, bundle);
                            }
                        }
                    }
                });

                //Start Recording
                try {
                    mMediaRecorder.start();
                    ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
                    Bundle bundle = new Bundle();
                    bundle.putInt(ON_START_KEY, ON_START);
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle);
                    }
                } catch (Exception e) {
                    // From the tests I've done, this can happen if another application is using the mic or if an unsupported video encoder was selected
                    Bundle bundle = new Bundle();
                    bundle.putInt(ERROR_KEY, SETTINGS_ERROR);
                    bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                    setupError(intent, bundle);

                }
            }
        } else {
            stopSelf(startId);
        }

        return Service.START_STICKY;
    }

    private void setupError(Intent intent, Bundle bundle) {
        ResultReceiver receiver = intent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, bundle);
        }
    }

    private void createNotification() {
        String channelId = "001";
        String channelName = "RecordChannel";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Notification notification;

          /*  Intent myIntent = new Intent(this, NotificationReceiver.class);
            PendingIntent pendingIntent;

            pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE);
*/
            notification = new Notification.Builder(getApplicationContext(), channelId)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(getString(R.string.stop_recording_notification_title))
                    .build();

            startFgs(101, notification);
        }


        if (returnedUri == null) {
            if (path == null) {
                path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
            }
        }
    }

    //Pause Recording
    private void pauseRecording() {
        mMediaRecorder.pause();
        ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
        Bundle bundle = new Bundle();
        bundle.putString(ON_PAUSE_KEY, ON_PAUSE);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, bundle);
        }
    }

    //Resume Recording
    private void resumeRecording() {
        mMediaRecorder.resume();
        ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
        Bundle bundle = new Bundle();
        bundle.putString(ON_RESUME_KEY, ON_RESUME);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, bundle);
        }
    }


    private void initMediaProjection() {
        mMediaProjection = ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).getMediaProjection(mResultCode, mResultData);
        Handler handler = new Handler(Looper.getMainLooper());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mMediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                }
            }, handler);
        } else {
            mMediaProjection.registerCallback(new MediaProjection.Callback() {
                // Nothing
                // We don't use it but register it to avoid runtime error from SDK 34+.
            }, handler);
        }
    }

    //Return the output file path as string
    public static String getFilePath() {
        return filePath;
    }

    //Return the name of the output file
    public static String getFileName() {
        return fileName;
    }

    private void initRecorder() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate).replace(" ", "");
        String videoQuality = "HD";

        // Set default name if not provided
        if (name == null) {
            name = videoQuality + curTime;
        }

        // Set file path and name
        filePath = path + "/" + name + ".mp4";
        fileName = name + ".mp4";

        mMediaRecorder = new MediaRecorder();

        // Set orientation hint if specified
        if (orientationHint != 400) {
            mMediaRecorder.setOrientationHint(orientationHint);
        }


        // Configure MediaRecorder
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

        // Configure audio settings if audio is enabled
        if (isAudioEnabled) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setAudioEncodingBitRate(128000);
            mMediaRecorder.setAudioSamplingRate(44100);
        }

        // Set video properties
        mMediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);
        mMediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
        mMediaRecorder.setVideoFrameRate(60);

        // Set output file
        if (returnedUri != null) {
            try {
                ContentResolver contentResolver = getContentResolver();
                FileDescriptor inputPFD = Objects.requireNonNull(contentResolver.openFileDescriptor(returnedUri, "rw")).getFileDescriptor();
                mMediaRecorder.setOutputFile(inputPFD);
            } catch (Exception e) {
                ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
                Bundle bundle = new Bundle();
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e));
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle);
                }
            }
        } else {
            mMediaRecorder.setOutputFile(filePath);
        }

        // Set file size limit if specified
        if (maxFileSize > NO_SPECIFIED_MAX_SIZE) {
            mMediaRecorder.setMaxFileSize(maxFileSize); // in bytes
        }

        // Prepare MediaRecorder
        mMediaRecorder.prepare();
    }

    private void initVirtualDisplay() {
        if (mMediaProjection == null) {
            Log.d(TAG, "initVirtualDisplay: " + " Media projection is not initialized properly.");
            return;
        }
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private void startFgs(int notificationId, Notification notificaton) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(notificationId, notificaton, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notificaton, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(notificationId, notificaton);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetAll();
        callOnComplete();

    }

    private void callOnComplete() {
        if (mIntent != null) {
            ResultReceiver receiver = mIntent.getParcelableExtra(ScreenRecordService.BUNDLED_LISTENER);
            Bundle bundle = new Bundle();
            bundle.putString(ON_COMPLETE_KEY, ON_COMPLETE);
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle);
            }
        }
    }

    private void resetAll() {
        stopForeground(true);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
