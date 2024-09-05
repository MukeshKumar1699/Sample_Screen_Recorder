package com.example.samplescreenrecorder.di

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.projection.MediaProjectionManager
import com.example.samplescreenrecorder.HBRecorderListenerImpl
import com.example.samplescreenrecorder.helper.HBRecorderHelper
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HBRecorderModule {

    @Provides
    fun provideHBRecorderListener(hbRecorderListenerImpl: HBRecorderListenerImpl): HBRecorderListener {
        return hbRecorderListenerImpl
    }

    @Provides
    @Singleton
    fun provideHBRecorder(
        context: Context,
        listener: HBRecorderListener
    ): HBRecorder {
        return HBRecorder(context, listener)
    }

    @Provides
    @Singleton
    fun provideHBRecorderHelper(
        context: Context,
        hbRecorder: HBRecorder,
        contentValues: ContentValues,
        contentResolver: ContentResolver
    ): HBRecorderHelper {
        return HBRecorderHelper(context, hbRecorder, contentResolver, contentValues)
    }

}


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideContentResolver(context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideContentValues(): ContentValues {
        return ContentValues()
    }

    @Provides
    @Singleton
    fun provideMediaProjectionManager(context: Context): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

}
