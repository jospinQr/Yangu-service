package com.megamind.yanguservice

import android.app.Application
import com.megamind.yanguservice.di.appModule
import com.megamind.yanguservice.di.networkModule
import com.megamind.yanguservice.di.roomModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()


        startKoin {
            androidContext(this@MyApp)
            modules(networkModule, appModule, roomModule)
            androidLogger()

        }
    }
}
