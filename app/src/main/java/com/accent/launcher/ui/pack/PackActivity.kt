package com.accent.launcher.ui.pack

import androidx.activity.viewModels
import com.accent.launcher.R
import com.accent.launcher.base.BaseActivity
import com.accent.launcher.data.Prefs
import com.accent.launcher.databinding.PackActivityBinding
import com.accent.launcher.ui.custom.ItemDecorationWithEnds

class PackActivity : BaseActivity<PackViewModel, PackActivityBinding>(R.layout.pack_activity) {

    val packViewModel: PackViewModel by viewModels()

    override val viewModel: PackViewModel
        get() = packViewModel

    override fun setupUI() {
        binding.recycler.post {
            binding.recycler.addItemDecoration(ItemDecorationWithEnds(
                bottomLast = binding.recycler.width / 360 * 96,
                lastPredicate = { p, c -> p == c - 1 }
            ))
        }
        binding.buttonOk.setOnClickListener {
            Prefs.iconPack = viewModel.selected.get()!!
            finish()
        }
        binding.buttonCancel.setOnClickListener { finish() }
    }

}