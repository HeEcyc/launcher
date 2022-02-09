package com.wlisuha.applauncher.ui.custom

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.databinding.NotificationViewBinding

class NotificationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    val binding: NotificationViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.notification_view,
        this,
        true
    )

    init {
        setBackgroundColor(Color.RED)
    }
}