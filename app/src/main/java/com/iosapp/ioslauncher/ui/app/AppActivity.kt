package com.iosapp.ioslauncher.ui.app

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseActivity
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.databinding.AppActivityBinding
import com.iosapp.ioslauncher.ui.dialogs.DialogTutorial
import com.iosapp.ioslauncher.utils.IRON_SOURCE_APP_KEY
import com.ironsource.mediationsdk.IronSource

class AppActivity : BaseActivity<AppViewModel, AppActivityBinding>(R.layout.app_activity) {

    private val roleIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

        }

    private val fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            askRole()
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
        }
    }
    override val viewModel: AppViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        IronSource.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        IronSource.onPause(this)
    }

    override fun setupUI() {
        IronSource.setMetaData("is_child_directed","false")
        IronSource.init(this, IRON_SOURCE_APP_KEY)

        if (!Prefs.isShowingTutorial) {
            DialogTutorial().show(supportFragmentManager, "tag")
        } else {
            askRole()
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallback)
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                super.onFragmentViewDestroyed(fm, f)
                askRole()
                supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
            }
        }, true)
    }

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

    override fun onBackPressed() {
        when {
            findViewById<MotionLayout>(R.id.motionView).progress > 0.2f ->
                findViewById<MotionLayout>(R.id.motionView).transitionToStart()
            else -> {
                viewModel.isSelectionEnabled.set(false)
//                super.onBackPressed()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.isSelectionEnabled.set(false)
    }
}