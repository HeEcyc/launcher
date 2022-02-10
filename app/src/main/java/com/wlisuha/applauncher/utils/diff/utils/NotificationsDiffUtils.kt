package com.wlisuha.applauncher.utils.diff.utils

import android.service.notification.StatusBarNotification
import androidx.recyclerview.widget.DiffUtil

class NotificationsDiffUtils(
    private var oldList: List<StatusBarNotification>,
    private var newList: List<StatusBarNotification>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].postTime == newList[newItemPosition].postTime &&
                oldList[oldItemPosition].packageName == newList[newItemPosition].packageName

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = true

}