package com.iosapp.ioslauncher.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.data.AppScreenLocation
import com.iosapp.ioslauncher.data.RecentApp

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