package com.safetap.app

import android.app.Application
import com.safetap.app.di.AppContainer

class SafeTapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
