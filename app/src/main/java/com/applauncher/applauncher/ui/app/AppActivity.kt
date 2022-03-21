package com.applauncher.applauncher.ui.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.BaseActivity
import com.applauncher.applauncher.data.Prefs
import com.applauncher.applauncher.databinding.AppActivityBinding
import com.applauncher.applauncher.ui.custom.ShutterView
import com.applauncher.applauncher.ui.dialogs.DialogNotificationsPermissions
import com.applauncher.applauncher.ui.dialogs.DialogPermission
import com.applauncher.applauncher.ui.dialogs.DialogTutorial


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

    override fun setupUI() {
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