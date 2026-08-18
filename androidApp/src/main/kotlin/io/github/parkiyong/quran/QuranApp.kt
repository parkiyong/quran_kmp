package io.github.parkiyong.quran

import android.app.Application
import io.github.parkiyong.quran.di.initKoin
import org.koin.android.ext.koin.androidContext

class QuranApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@QuranApp)
        }
    }
}
