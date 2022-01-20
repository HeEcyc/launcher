package com.wlisuha.applauncher.utils

import androidx.recyclerview.widget.DiffUtil
import com.wlisuha.applauncher.data.InstalledApp

class AppListDiffUtils(
    private var oldList: List<InstalledApp>,
    private var newList: List<InstalledApp>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].packageName == newList[newItemPosition].packageName

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
}