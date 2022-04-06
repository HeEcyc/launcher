package com.iosapp.ioslauncher.ui

import android.content.Intent
import androidx.activity.viewModels
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseActivity
import com.iosapp.ioslauncher.base.BaseViewModel
import com.iosapp.ioslauncher.databinding.EmptyLaunchActivityBinding
import com.iosapp.ioslauncher.ui.app.AppActivity

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