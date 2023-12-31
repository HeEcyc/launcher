package com.iosapp.ioslauncher.ui.bg

import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseViewModel
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.data.Background
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.databinding.ItemBackgroundBinding

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