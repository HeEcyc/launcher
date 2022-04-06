package com.iosapp.ioslauncher.ui.app

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.iosapp.ioslauncher.ui.custom.LauncherView
import com.iosapp.ioslauncher.ui.custom.NonSwipeableViewPager
import com.iosapp.ioslauncher.ui.custom.NotificationScreenView
import com.iosapp.ioslauncher.ui.custom.ShutterView

class MainVPAdapter(
    private val viewModel: AppViewModel,
    private val permissionHelper: ShutterView.PermissionHelper,
    private val mainViewPager: NonSwipeableViewPager
) : PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return when (position) {
            0 -> notificationList(container)
            else -> appLayout(container)
        }
    }

    private fun appLayout(container: ViewGroup) =
        LauncherView(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            viewPager = mainViewPager
            container.addView(this)
            binding.viewList.permissionHelper = permissionHelper
            setViewModel(this@MainVPAdapter.viewModel)
        }

    private fun notificationList(container: ViewGroup) =
        NotificationScreenView(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            container.addView(this)
        }


    override fun getCount() = 2

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }

}