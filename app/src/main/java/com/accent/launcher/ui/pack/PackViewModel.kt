package com.accent.launcher.ui.pack

import androidx.core.content.res.ResourcesCompat
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
            val resources = LauncherApplication.instance.resources
            binding.selected = ObservableWrapper(selected)
            binding.recycler.adapter = createAdapter<InstalledApp, LauncherItemApplicationMenuBinding>(R.layout.launcher_item_application_menu) {
                initItems = MutableList(presetIconApps.take(12).size) { index ->
                    InstalledApp(
                        "",
                        ResourcesCompat.getDrawable(
                            resources,
                            resources.getIdentifier(
                                "ic_${item.packId()}_${index.toString().padStart(2, '0')}",
                                "mipmap",
                                LauncherApplication.instance.packageName
                            ),
                            null
                        ),
                        "",
                        true,
                        AppViewModel()
                    )
                }
            }
            binding.imgSelected.alpha = if (selected == item) 1f else 0f
            binding.button.setOnClickListener { selected.set(item) }
        }
    }

    class ObservableWrapper(val observable: ObservableField<IconPack>)

}