package com.glazev.celebrationai.di

import com.glazev.celebrationai.BuildConfig
import com.glazev.celebrationai.Config
import com.glazev.celebrationai.data.AppDatabase
import com.glazev.celebrationai.data.AppSettings
import com.glazev.celebrationai.service.*
import com.glazev.celebrationai.ui.viewmodel.CelebrationViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().celebrationDao() }
    single { AppSettings(androidContext()) }
    single { AuthManager(androidContext()) }
    single { SyncManager(get(), get()) }
    // Теперь используем ключ из BuildConfig, который берется из local.properties
    single { GeminiService(androidContext(), BuildConfig.BACKEND_URL, get(), get()) }
    single { NotificationHelper(androidContext()) }
    single { BiometricHelper(androidContext()) }
    single { YandexAdManager(androidContext()) }
    single<BillingManager> { RuStoreBillingManagerImpl(androidContext()) }
    viewModel { CelebrationViewModel(androidApplication(), get(), get(), get(), get()) }
}
