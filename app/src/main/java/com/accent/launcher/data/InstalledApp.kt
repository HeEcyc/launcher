package com.accent.launcher.data

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import com.accent.launcher.LauncherApplication
import com.accent.launcher.ui.app.AppViewModel

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
