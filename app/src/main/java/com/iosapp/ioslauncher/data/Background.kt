package com.iosapp.ioslauncher.data

import androidx.databinding.ObservableField

data class Background(val imageRes: Int) {
    val isSelected = ObservableField(Prefs.bgRes == imageRes)
}