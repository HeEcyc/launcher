package com.iosapp.ioslauncher.ui.pack

import androidx.activity.viewModels
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseActivity
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.databinding.PackActivityBinding
import com.iosapp.ioslauncher.ui.custom.ItemDecorationWithEnds

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