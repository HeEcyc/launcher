package com.iosapp.ioslauncher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecentApp(
    @PrimaryKey
    val packageName: String,
    val time: Long,
)