package com.wlisuha.applauncher.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wlisuha.applauncher.LauncherApplication
import com.wlisuha.applauncher.data.AppScreenLocation

@Database(entities = [AppScreenLocation::class], version = 1, exportSchema = false)
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