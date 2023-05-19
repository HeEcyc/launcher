package com.accent.launcher.ui.app

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.accent.launcher.R
import com.accent.launcher.base.BaseActivity
import com.accent.launcher.data.Prefs
import com.accent.launcher.databinding.AppActivityBinding
import com.accent.launcher.ui.dialogs.DialogTutorial
import com.accent.launcher.utils.IRON_SOURCE_APP_KEY
import com.ironsource.mediationsdk.IronSource
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent.setEventListener
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener


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
        setEventListener(this,
            KeyboardVisibilityEventListener { isOpen ->
                viewModel.isKeyboardOpen.set(isOpen)
            }
        )
        IronSource.setMetaData("is_child_directed","false")
        IronSource.init(this, IRON_SOURCE_APP_KEY)

        binding.root.fitSystemWindowsAndAdjustResize()

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
        viewModel.onBackPressed.observe(this) { onBackPressed() }
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
            viewModel.hasAppInfoBubble.get() ->
                viewModel.hasAppInfoBubble.set(false)
            (viewModel.searchQuery.get()?.length ?: 0) > 0 ->
                viewModel.searchQuery.set("")
            viewModel.isKeyboardOpen.get() ->
                getSystemService(InputMethodManager::class.java)
                    .hideSoftInputFromWindow(binding.root.windowToken, 0)
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

    private fun View?.fitSystemWindowsAndAdjustResize() = this?.let { view ->
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            view.fitsSystemWindows = true
            val bottom = insets.getInsets(WindowInsetsCompat.Type.ime() or WindowInsetsCompat.Type.statusBars()).bottom

            WindowInsetsCompat
                .Builder()
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(0, 0, 0, bottom)
                )
                .build()
                .apply {
                    ViewCompat.onApplyWindowInsets(v, this)
                }
        }
    }

}