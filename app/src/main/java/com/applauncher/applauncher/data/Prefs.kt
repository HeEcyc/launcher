package com.applauncher.applauncher.data

import android.content.Context
import android.content.SharedPreferences
import com.applauncher.applauncher.LauncherApplication
import com.applauncher.applauncher.R

object Prefs {

    val sharedPreference: SharedPreferences by lazy {
        LauncherApplication.instance.getSharedPreferences("app", Context.MODE_PRIVATE)
    }

    const val bgResKey = "bg_res_key"
    const val isShowingTutorialKey = "isShowingTutorial"

    var bgRes: Int = R.mipmap.img_10
        get() = sharedPreference.getInt(bgResKey, R.mipmap.img_10)
        set(value) {
            saveBG(value)
            field = value
        }


    var isShowingTutorial: Boolean = false
        get() = sharedPreference.getBoolean(isShowingTutorialKey, false)
        set(value) {
            sharedPreference.edit().putBoolean(isShowingTutorialKey, value).apply()
            field = value
        }

    private fun saveBG(value: Int) {
        sharedPreference.edit().putInt(bgResKey, value).apply()
    }
}