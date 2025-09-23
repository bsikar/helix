package com.bsikar.helix

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.bsikar.helix.data.model.PresetTags
import com.bsikar.helix.di.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HelixApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PresetTags from assets/tags.json
        PresetTags.initialize(this)
        
        // WorkManager will automatically use our Configuration.Provider implementation
        // No manual initialization needed when implementing Configuration.Provider
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}