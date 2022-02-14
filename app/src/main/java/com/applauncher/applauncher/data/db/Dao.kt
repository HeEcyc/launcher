package com.applauncher.applauncher.data.db

import androidx.room.*
import androidx.room.Dao
import com.applauncher.applauncher.data.AppScreenLocation

@Dao
interface Dao {

    @Query("SELECT * from AppScreenLocation")
    fun getAppsPositions(): List<AppScreenLocation>

    @Query("SELECT count(*) from AppScreenLocation")
    fun getRowCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addItem(appScreenLocation: AppScreenLocation)

    @Delete
    fun delete(appScreenLocation: AppScreenLocation)

    @Query("DELETE FROM AppScreenLocation WHERE packageName = :packageName")
    fun deletePackage(packageName: String)

    @Update
    fun updateItem(appScreenLocation: AppScreenLocation)
}