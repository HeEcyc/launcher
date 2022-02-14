package com.applauncher.applauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.applauncher.applauncher.LauncherApplication

object Prefs {
    val sharedPreference: SharedPreferences by lazy {
        LauncherApplication.instance.getSharedPreferences("app", Context.MODE_PRIVATE)
    }
}