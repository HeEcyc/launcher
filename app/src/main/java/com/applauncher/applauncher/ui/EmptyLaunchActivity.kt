package com.applauncher.applauncher.ui

import android.content.Intent
import androidx.activity.viewModels
import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.BaseActivity
import com.applauncher.applauncher.base.BaseViewModel
import com.applauncher.applauncher.databinding.EmptyLaunchActivityBinding
import com.applauncher.applauncher.ui.app.AppActivity

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