package com.iosapp.ioslauncher.data

import android.graphics.drawable.Drawable

data class InstalledApp(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    var isNonSystemApp: Boolean
)
