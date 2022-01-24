package com.wlisuha.applauncher.data.db

import androidx.room.Dao
import androidx.room.Query
import com.wlisuha.applauncher.data.AppScreenLocation

@Dao
interface Dao {

    @Query("SELECT * from AppScreenLocation")
    fun getAppsPositions(): List<AppScreenLocation>

}