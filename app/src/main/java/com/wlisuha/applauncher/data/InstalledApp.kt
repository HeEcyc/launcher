package com.wlisuha.applauncher.data

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField

data class InstalledApp(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    val labelColor: ObservableField<Int>
)
