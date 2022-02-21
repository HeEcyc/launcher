package com.applauncher.applauncher.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.service.notification.StatusBarNotification
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.applauncher.applauncher.BR
import com.applauncher.applauncher.LauncherApplication
import com.applauncher.applauncher.R
import com.applauncher.applauncher.base.createAdapter
import com.applauncher.applauncher.data.Prefs
import com.applauncher.applauncher.databinding.ItemNotificationBinding
import com.applauncher.applauncher.databinding.NotificationViewBinding
import com.applauncher.applauncher.utils.diff.utils.NotificationsDiffUtils

class NotificationScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), LauncherApplication.NotificationListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val notificationTextColor = NotificationTextColor()

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
                binding.setVariable(BR.textColor, notificationTextColor)
                binding.notifyPropertyChanged(BR.textColor)
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
        binding.removeAllNotificationsButton.setOnClickListener {
            Intent("cancel").let(context::sendBroadcast)
        }
        binding.notificationsList.addItemDecoration(NotificationItemDecoration())
        binding.notificationsList.adapter = adapter
        ItemTouchHelper(touchCallback)
            .attachToRecyclerView(binding.notificationsList)
        LauncherApplication.instance.notificationListener = this
        Typeface.createFromAsset(context.assets, "font.ttf").let {
            binding.mainClock.paint.typeface = it
            binding.textClock.typeface = it
            binding.notificationsCenterText.typeface = it
        }
        Prefs.sharedPreference.registerOnSharedPreferenceChangeListener(this)
        notificationTextColor.notificationTextColor.set(
            if (Prefs.bgRes == R.mipmap.img_10) Color.BLACK
            else Color.WHITE
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LauncherApplication.instance.notificationListener = null
        Prefs.sharedPreference.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onNotificationsChanges(statusBarNotification: List<StatusBarNotification>) {
        val oldList = adapter.getData().toList()

        adapter.getData().clear()
        adapter.getData().addAll(statusBarNotification)

        DiffUtil.calculateDiff(NotificationsDiffUtils(oldList, adapter.getData()))
            .dispatchUpdatesTo(adapter)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == Prefs.bgResKey) {
            val textColor =
                if (sharedPreferences.getInt(key, R.mipmap.img_10) == R.mipmap.img_10) Color.BLACK
                else Color.WHITE

            notificationTextColor.notificationTextColor
                .set(textColor)
        }
    }

    class NotificationTextColor {
        val notificationTextColor = ObservableField(Color.BLACK)
    }
}