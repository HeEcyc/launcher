package com.iosapp.ioslauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.iosapp.ioslauncher.LauncherApplication

object Prefs {

    val sharedPreference: SharedPreferences by lazy {
        LauncherApplication.instance.getSharedPreferences("app", Context.MODE_PRIVATE)
    }

//    const val bgResKey = "bg_res_key"
    const val isShowingTutorialKey = "isShowingTutorial"
    const val iconPackKey = "iconPack"

//    var bgRes: Int = R.mipmap.img_10
//        get() = sharedPreference.getInt(bgResKey, R.mipmap.img_10)
//        set(value) {
//            saveBG(value)
//            field = value
//        }


    var isShowingTutorial: Boolean = false
        get() = sharedPreference.getBoolean(isShowingTutorialKey, false)
        set(value) {
            sharedPreference.edit().putBoolean(isShowingTutorialKey, value).apply()
            field = value
        }

//    private fun saveBG(value: Int) {
//        sharedPreference.edit().putInt(bgResKey, value).apply()
//    }

    val onIconPackChanged = MutableLiveData<Unit>()
    var iconPack: IconPack
        get() = IconPack.values()[sharedPreference.getInt(iconPackKey, IconPack.Default.packId())]
        set(value) {
            sharedPreference.edit().putInt(iconPackKey, value.packId()).apply()
            onIconPackChanged.postValue(Unit)
        }

}