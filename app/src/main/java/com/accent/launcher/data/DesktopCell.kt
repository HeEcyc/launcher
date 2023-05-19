package com.accent.launcher.data

import androidx.databinding.ObservableField

class DesktopCell(
    val position: Int,
    val page: Int,
    app: InstalledApp? = null
) {

    val app = ObservableField<InstalledApp>(app)

    override fun toString(): String {
        return "[cell: $page, $position, ${app.get()?.packageName}]"
    }

    fun updateApp(installedApp: InstalledApp?) = app.set(installedApp)

}