package com.wlisuha.applauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.wlisuha.applauncher.LauncherApplication

object Prefs {
    val sharedPreference: SharedPreferences by lazy {
        LauncherApplication.instance.getSharedPreferences("app", Context.MODE_PRIVATE)
    }
}