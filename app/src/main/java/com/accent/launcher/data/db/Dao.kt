package com.accent.launcher.data.db

import androidx.room.*
import androidx.room.Dao
import com.accent.launcher.data.AppScreenLocation
import com.accent.launcher.data.RecentApp

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

    @Query("DELETE FROM AppScreenLocation WHERE page = :page AND position = :position")
    fun deleteShortcutByPosition(page: Int, position: Int)

    @Update
    fun updateItem(appScreenLocation: AppScreenLocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentApp(recentApp: RecentApp)

    @Query("SELECT * FROM RecentApp")
    fun getRecentApps(): List<RecentApp>

    @Query("DELETE FROM RecentApp WHERE packageName = :packageName")
    fun deletePackageFromRecent(packageName: String)
}