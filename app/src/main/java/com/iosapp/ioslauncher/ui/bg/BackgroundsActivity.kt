package com.iosapp.ioslauncher.ui.bg

//import android.graphics.Rect
//import android.view.View
//import androidx.activity.viewModels
//import androidx.recyclerview.widget.RecyclerView
//import com.iosapp.ioslauncher.R
//import com.iosapp.ioslauncher.base.BaseActivity
//import com.iosapp.ioslauncher.databinding.DialogBackgroundsBinding
//
//class BackgroundsActivity :
//    BaseActivity<BackgroundsViewModel, DialogBackgroundsBinding>(R.layout.dialog_backgrounds) {
//    override val viewModel: BackgroundsViewModel by viewModels()
//
//    override fun setupUI() {
//        binding.backgrounds.addItemDecoration(createItemDecoration())
//        binding.closeButton.setOnClickListener {
//            finish()
//        }
//    }
//
//    private fun createItemDecoration() = object : RecyclerView.ItemDecoration() {
//        override fun getItemOffsets(
//            outRect: Rect,
//            view: View,
//            parent: RecyclerView,
//            state: RecyclerView.State
//        ) {
//            val position = parent.getChildLayoutPosition(view)
//            if (position % 2 == 0) {
//                outRect.left = 40
//                outRect.right = 20
//            } else {
//                outRect.left = 20
//                outRect.right = 40
//            }
//
//            outRect.bottom = if (position + 1 == parent.adapter?.itemCount) 400
//            else 40
//        }
//    }
//
//
//}