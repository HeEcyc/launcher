package com.accent.launcher.ui

import android.content.Intent
import androidx.activity.viewModels
import com.accent.launcher.R
import com.accent.launcher.base.BaseActivity
import com.accent.launcher.base.BaseViewModel
import com.accent.launcher.databinding.EmptyLaunchActivityBinding
import com.accent.launcher.ui.app.AppActivity

class EmptyLaunchActivity :
    BaseActivity<EmptyLaunchActivity.EmptyViewModel, EmptyLaunchActivityBinding>(R.layout.empty_launch_activity) {

    class EmptyViewModel : BaseViewModel()

    override val viewModel: EmptyViewModel by viewModels()

    override fun setupUI() {
        Intent(this, AppActivity::class.java)
            .let(::startActivity)
        finish()
    }
}