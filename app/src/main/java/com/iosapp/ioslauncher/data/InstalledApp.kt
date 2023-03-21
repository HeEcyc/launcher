package com.iosapp.ioslauncher.data

import android.graphics.drawable.Drawable
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.ui.app.AppViewModel

data class InstalledApp(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    var isNonSystemApp: Boolean,
    val viewModel: AppViewModel
) {

    val applicationInfo
        get() = LauncherApplication.instance.packageManager.getApplicationInfo(packageName, 0)

}
