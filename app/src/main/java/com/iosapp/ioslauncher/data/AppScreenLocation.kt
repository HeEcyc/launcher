package com.iosapp.ioslauncher.data

import androidx.room.Entity

@Entity(primaryKeys = ["page", "position"])
data class AppScreenLocation(
    val packageName: String,
    val page: Int,
    val position: Int
)