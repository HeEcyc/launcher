package com.wlisuha.applauncher.ui.custom

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.wlisuha.applauncher.LauncherApplication
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.createAdapter
import com.wlisuha.applauncher.databinding.ItemNotificationBinding
import com.wlisuha.applauncher.databinding.NotificationViewBinding
import com.wlisuha.applauncher.utils.diff.utils.NotificationsDiffUtils

class NotificationScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), LauncherApplication.NotificationListener {
    var drawerLayout: DrawerLayout? = null

    private val touchCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                cancelNotification(adapter.getData()[viewHolder.adapterPosition])
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return if (adapter.getData()[viewHolder.adapterPosition].isClearable) super
                    .getMovementFlags(recyclerView, viewHolder)
                else 0
            }
        }

    private fun cancelNotification(statusBarNotification: StatusBarNotification) {
        Intent("cancel_current")
            .putExtra("key", statusBarNotification.key)
            .let(context::sendBroadcast)
    }

    @SuppressLint("ClickableViewAccessibility")
    private val adapter =
        createAdapter<StatusBarNotification, ItemNotificationBinding>(R.layout.item_notification) {
            onBind = { _, binding, _ ->
                binding.root.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> drawerLayout?.requestDisallowInterceptTouchEvent(
                            true
                        )
                        MotionEvent.ACTION_UP -> drawerLayout?.requestDisallowInterceptTouchEvent(
                            false
                        )

                    }
                    return@setOnTouchListener false
                }
            }
            onItemClick = {
                it.notification.contentIntent.send()
            }
        }
    val binding: NotificationViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.notification_view,
        this,
        true
    )

    init {
        binding.notificationsList.addItemDecoration(NotificationItemDecoration())
        binding.notificationsList.adapter = adapter
        ItemTouchHelper(touchCallback)
            .attachToRecyclerView(binding.notificationsList)
        LauncherApplication.instance.notificationListener = this
        ResourcesCompat.getFont(context, R.font.sf_pro_display)?.let {
            binding.mainClock.typeface = it
            binding.textClock.typeface = it
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LauncherApplication.instance.notificationListener = null
    }

    override fun onNotificationsChanges(statusBarNotification: List<StatusBarNotification>) {

        val oldList = adapter.getData().toList()

        adapter.getData().clear()
        adapter.getData().addAll(statusBarNotification)

        DiffUtil.calculateDiff(NotificationsDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)
    }
}