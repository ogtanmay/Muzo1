package com.shashwat.muzo

import android.app.Application

class MuzoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MuzoApplication
            private set
    }
}
