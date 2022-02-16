package com.applauncher.applauncher.ui.dialogs

import android.app.Dialog
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.createAdapter
import com.applauncher.applauncher.data.Background
import com.applauncher.applauncher.databinding.DialogBackgroundsBinding
import com.applauncher.applauncher.databinding.ItemBackgroundBinding
import com.applauncher.applauncher.ui.app.AppViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BgDialogs : BottomSheetDialogFragment() {
    private lateinit var binding: DialogBackgroundsBinding
    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            bottomSheetDialog
                .findViewById<View?>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { it ->
                    val behaviour = BottomSheetBehavior.from(it)
                    setupFullHeight(it)
                    behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                    behaviour.addBottomSheetCallback(object :
                        BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                                dismiss()
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {

                        }

                    })
                }
        }
        return dialog
    }

    val adapter = createAdapter<Background, ItemBackgroundBinding>(R.layout.item_background) {
        onItemClick = ::handleBackgroundClick
    }

    private fun handleBackgroundClick(background: Background) {
        if (background.isSelected.get() == true) return
        clearSelectedItem()
        viewModel.onBackgroundSelected(background)
    }

    private fun clearSelectedItem() {
        adapter.getData()
            .firstOrNull { it.isSelected.get() == true }
            ?.isSelected?.set(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_backgrounds, container, false)!!
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.backgrounds.adapter = adapter
        adapter.reloadData(viewModel.getBackgrounds())
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

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }
}