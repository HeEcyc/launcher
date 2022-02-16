package com.applauncher.applauncher.ui.bg

import android.graphics.Rect
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.BaseActivity
import com.applauncher.applauncher.databinding.DialogBackgroundsBinding

class BackgroundsActivity :
    BaseActivity<BackgroundsViewModel, DialogBackgroundsBinding>(R.layout.dialog_backgrounds) {
    override val viewModel: BackgroundsViewModel by viewModels()

    override fun setupUI() {
        binding.backgrounds.addItemDecoration(createItemDecoration())
    }

    private fun createItemDecoration() = object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildLayoutPosition(view)
            if (position % 2 == 0) {
                outRect.left = 40
                outRect.right = 20
            } else {
                outRect.left = 20
                outRect.right = 40
            }

            outRect.bottom = 40
        }
    }


}