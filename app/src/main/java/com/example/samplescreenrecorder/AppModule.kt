package com.example.samplescreenrecorder

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.projection.MediaProjectionManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.hbisoft.hbrecorder.HBRecorder
import com.hbisoft.hbrecorder.HBRecorderListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/*
@Module
@InstallIn(ActivityComponent::class)
abstract class HBRRecorderModule {

    @ActivityScoped
    @Binds
    abstract fun bindHBRecorderListener(
        hbRecorderListenerImpl: HBRecorderListenerImpl
    ): HBRecorderListener

    companion object {
        @ActivityScoped
        @Provides
        fun provideHBRecorder(
            @ActivityContext context: Context,
            listener: HBRecorderListener
        ): HBRecorder {
            return HBRecorder(context, listener)
        }
    }

}*/


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val PREFERENCES_NAME = "my_preferences"

    @Provides
    @Singleton
    @Named("pref_name")// name in case of conflict
    fun providePreferenceName(): String = PREFERENCES_NAME

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
       PreferenceDataStoreFactory.create(

           produceFile = {
               context.preferencesDataStoreFile("test_preference")
           }

       )

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideHBRecorderListener(hbRecorderListenerImpl: HBRecorderListenerImpl) : HBRecorderListener {
        return hbRecorderListenerImpl
    }

    @Provides
    fun provideHBRecorder(
        context: Context,
        listener: HBRecorderListener
    ): HBRecorder {
        return HBRecorder(context, listener)
    }

    @Provides
    fun provideContentResolver(context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    fun provideContentValues(): ContentValues {
        return ContentValues()
    }

    @Provides
    fun provideMediaProjectionManager(context: Context): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

}
