package com.accent.launcher.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import com.accent.launcher.LauncherApplication
import com.accent.launcher.utils.presetIconApps
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable

sealed interface IconPack : Serializable {

    fun getAppIcon(ai: ApplicationInfo, pm: PackageManager): Drawable

    companion object {
        fun values() = listOf(Pack0, Pack1, Pack2, Default)
    }

    fun packId() = values().indexOf(this)

    object Default : IconPack {
        override fun getAppIcon(ai: ApplicationInfo, pm: PackageManager): Drawable =
            ai.loadIcon(pm)
    }

    private interface Custom : IconPack {

        private fun getAppIconFile(packageName: String): File =
            File(
                LauncherApplication.instance.filesDir,
                "$packageName:" + this::class.java.name + ".png"
            )

        fun transformDrawable(drawable: Drawable): Drawable

        override fun getAppIcon(ai: ApplicationInfo, pm: PackageManager): Drawable {
            val resources = LauncherApplication.instance.resources

            if (ai.packageName in presetIconApps)
                return ResourcesCompat.getDrawable(
                    resources,
                    resources.getIdentifier(
                        "ic_${packId()}_" + presetIconApps.indexOf(ai.packageName).toString().padStart(2, '0'),
                        "mipmap",
                        LauncherApplication.instance.packageName
                    ),
                    null
                )!!

            val file = getAppIconFile(ai.packageName)
            val bitmap: Bitmap
            if (!file.exists()) {
                bitmap = transformDrawable(ai.loadIcon(pm)).toBitmap()
                file.createNewFile()
                BufferedOutputStream(FileOutputStream(file)).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } else {
                bitmap = BitmapFactory.decodeFile(file.path)// ?: ai.loadIcon(pm).toBitmap()
            }
            return bitmap.toDrawable(resources)
        }

    }

    object Pack0 : Custom {
        override fun transformDrawable(drawable: Drawable): Drawable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                drawable.background.setTint(Color.BLACK)
                drawable.foreground.setTint(Color.WHITE)
            }
            return drawable
        }
    }

    object Pack1 : Custom {
        override fun transformDrawable(drawable: Drawable): Drawable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                drawable.background.setTint(
                    Palette.from(
                        drawable.background.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
                    ).generate().getDominantColor(Color.BLACK)
                )
                drawable.foreground.setTint(Color.WHITE)
            }
            return drawable
        }
    }

    object Pack2 : Custom {
        override fun transformDrawable(drawable: Drawable): Drawable {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                drawable.foreground
            } else {
                drawable
            }
        }
    }

}