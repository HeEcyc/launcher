package com.accent.launcher.ui.pack

import androidx.databinding.ObservableField
import com.accent.launcher.LauncherApplication
import com.accent.launcher.R
import com.accent.launcher.base.BaseViewModel
import com.accent.launcher.base.createAdapter
import com.accent.launcher.data.IconPack
import com.accent.launcher.data.InstalledApp
import com.accent.launcher.data.Prefs
import com.accent.launcher.databinding.ItemPackBinding
import com.accent.launcher.databinding.LauncherItemApplicationMenuBinding
import com.accent.launcher.ui.app.AppViewModel
import com.accent.launcher.utils.presetIconApps

class PackViewModel : BaseViewModel() {

    var selected = ObservableField(Prefs.iconPack)

    val adapter = createAdapter<IconPack, ItemPackBinding>(R.layout.item_pack) {
        initItems = IconPack.values().toMutableList()
        onBind = { item, binding, adapter ->
            val pm = LauncherApplication.instance.packageManager
            binding.selected = ObservableWrapper(selected)
            binding.recycler.adapter = createAdapter<InstalledApp, LauncherItemApplicationMenuBinding>(R.layout.launcher_item_application_menu) {
                initItems = presetIconApps.take(12).map {
                    InstalledApp(
                        "",
                        item.getAppIcon(pm.getApplicationInfo(it, 0), pm),
                        it,
                        true,
                        AppViewModel()
                    )
                }.toMutableList()
            }
            binding.imgSelected.alpha = if (selected == item) 1f else 0f
            binding.button.setOnClickListener { selected.set(item) }
        }
    }

    class ObservableWrapper(val observable: ObservableField<IconPack>)

}