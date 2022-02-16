package com.applauncher.applauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.applauncher.applauncher.LauncherApplication
import com.applauncher.applauncher.R

object Prefs {

    private val sharedPreference: SharedPreferences by lazy {
        LauncherApplication.instance.getSharedPreferences("app", Context.MODE_PRIVATE)
    }

    private const val bgResKey = "bg_res_key"

    var bgRes: Int = R.mipmap.img_10
        get() = sharedPreference.getInt(bgResKey, R.mipmap.img_10)
        set(value) {
            sharedPreference
                .edit()
                .putInt(bgResKey, value)
                .apply()
            field = value
        }
}