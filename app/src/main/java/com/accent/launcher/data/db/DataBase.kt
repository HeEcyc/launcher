package com.accent.launcher.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.accent.launcher.LauncherApplication
import com.accent.launcher.data.AppScreenLocation
import com.accent.launcher.data.RecentApp

@Database(entities = [AppScreenLocation::class, RecentApp::class], version = 3, exportSchema = false)
abstract class DataBase : RoomDatabase() {

    abstract fun dao(): Dao

    companion object {

        val dao by lazy { generateDao() }

        private fun generateDao() = Room
            .databaseBuilder(LauncherApplication.instance, DataBase::class.java, "db")
            .build()
            .dao()
    }

}