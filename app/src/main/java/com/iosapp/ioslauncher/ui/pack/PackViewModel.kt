package com.iosapp.ioslauncher.ui.pack

import androidx.databinding.ObservableField
import com.iosapp.ioslauncher.LauncherApplication
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.base.BaseViewModel
import com.iosapp.ioslauncher.base.createAdapter
import com.iosapp.ioslauncher.data.IconPack
import com.iosapp.ioslauncher.data.InstalledApp
import com.iosapp.ioslauncher.data.Prefs
import com.iosapp.ioslauncher.databinding.ItemPackBinding
import com.iosapp.ioslauncher.databinding.LauncherItemApplicationMenuBinding
import com.iosapp.ioslauncher.ui.app.AppViewModel
import com.iosapp.ioslauncher.utils.presetIconApps

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