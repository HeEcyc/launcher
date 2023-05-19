package com.accent.launcher

import android.app.Application

class LauncherApplication : Application() {

    companion object {
        lateinit var instance: LauncherApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}