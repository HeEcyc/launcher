package com.wlisuha.applauncher.data.db

import androidx.room.*
import androidx.room.Dao
import com.wlisuha.applauncher.data.AppScreenLocation

@Dao
interface Dao {

    @Query("SELECT * from AppScreenLocation")
    fun getAppsPositions(): List<AppScreenLocation>

    @Query("SELECT count(*) from AppScreenLocation")
    fun getRowCount(): Int

    @Insert
    fun addItem(appScreenLocation: AppScreenLocation)

    @Delete
    fun delete(appScreenLocation: AppScreenLocation)

    @Update
    fun updateItem(appScreenLocation: AppScreenLocation)
}