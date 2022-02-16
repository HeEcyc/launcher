package com.applauncher.applauncher.ui.bg

import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.BaseViewModel
import com.applauncher.applauncher.base.createAdapter
import com.applauncher.applauncher.data.Background
import com.applauncher.applauncher.data.Prefs
import com.applauncher.applauncher.databinding.ItemBackgroundBinding

class BackgroundsViewModel : BaseViewModel() {
    val adapter = createAdapter<Background, ItemBackgroundBinding>(R.layout.item_background) {
        initItems = getBackgrounds()
        onItemClick = ::handleBackgroundClick
    }

    private fun onBackgroundSelected(background: Background) {
        Prefs.bgRes = background.imageRes
        background.isSelected.set(true)
    }

    private fun handleBackgroundClick(background: Background) {
        if (background.isSelected.get() == true) return
        clearSelectedItem()
        onBackgroundSelected(background)
    }

    private fun clearSelectedItem() {
        adapter.getData()
            .firstOrNull { it.isSelected.get() == true }
            ?.isSelected?.set(false)
    }

    private fun getBackgrounds() = listOf(
        Background(R.mipmap.img_0),
        Background(R.mipmap.img_1),
        Background(R.mipmap.img_2),
        Background(R.mipmap.img_3),
        Background(R.mipmap.img_4),
        Background(R.mipmap.img_5),
        Background(R.mipmap.img_6),
        Background(R.mipmap.img_7),
        Background(R.mipmap.img_8),
        Background(R.mipmap.img_9),
        Background(R.mipmap.img_10),
    )

}