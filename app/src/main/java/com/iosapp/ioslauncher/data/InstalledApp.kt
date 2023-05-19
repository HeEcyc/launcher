package com.iosapp.ioslauncher.data

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.ui.app.AppViewModel

data class InstalledApp(
    val name: String,
    var iconInit: Drawable?,
    val packageName: String,
    var isNonSystemApp: Boolean,
    val viewModel: AppViewModel
) {

    val icon = ObservableField<Drawable>(iconInit).also { iconInit = null }

    val applicationInfo
        get() = LauncherApplication.instance.packageManager.getApplicationInfo(packageName, 0)

}
