package com.wlisuha.applauncher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AppScreenLocation(
    @PrimaryKey
    val packageName: String,
    val page: Int,
    val position: Int
)