package com.glazev.celebrationai

import android.app.Application
import com.glazev.celebrationai.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CelebrationApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CelebrationApp)
            modules(appModule)
        }
    }
}
