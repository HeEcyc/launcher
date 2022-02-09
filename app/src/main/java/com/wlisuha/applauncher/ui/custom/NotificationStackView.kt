package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.graphics.Color
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.databinding.NotificationStackViewBinding
import kotlin.random.Random

class NotificationStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val stackAdapter = SwipeStackAdapter()
    private val notifications = mutableListOf<StatusBarNotification>()

    fun addNotification(statusBarNotification: StatusBarNotification) {
        if (notifications.any {
                it.packageName == statusBarNotification.packageName &&
                        it.notification.tickerText == statusBarNotification.notification.tickerText
            }) return
        notifications.add(0, statusBarNotification)
        stackAdapter.notifyDataSetChanged()
    }

    fun removeNotification(statusBarNotification: StatusBarNotification) {
        val toRemove = notifications.firstOrNull {
            it.packageName == statusBarNotification.packageName &&
                    it.notification.tickerText == statusBarNotification.notification.tickerText
        } ?: return
        notifications.remove(toRemove)
        stackAdapter.notifyDataSetChanged()
      //  binding.notificationStack.resetStack()
    }

    val binding: NotificationStackViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.notification_stack_view,
        this,
        true
    )

    init {
        stackAdapter.notifyDataSetChanged()
        binding.notificationStack.adapter = stackAdapter
    }

    fun getRandomColor(): Int {
        return Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    }

    inner class SwipeStackAdapter : BaseAdapter() {

        override fun getCount() = notifications.size

        override fun getItem(position: Int) = position

        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return View(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(getRandomColor())
            }
        }
    }
}