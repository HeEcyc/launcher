package com.wlisuha.applauncher.ui

import android.graphics.Rect
import android.view.DragEvent
import android.view.View
import androidx.activity.viewModels
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.BaseActivity
import com.wlisuha.applauncher.base.BaseAdapter
import com.wlisuha.applauncher.data.DragInfo
import com.wlisuha.applauncher.data.InstalledApp
import com.wlisuha.applauncher.databinding.AppActivityBinding
import com.wlisuha.applauncher.utils.APP_COLUMN_COUNT


class AppActivity : BaseActivity<AppViewModel, AppActivityBinding>(R.layout.app_activity),
    View.OnDragListener {

    private val viewPagerAdapter: VPAdapter by lazy { createVPAdapter() }
    override val viewModel: AppViewModel by viewModels()

    override fun setupUI() {
        calculateAppItemViewHeight()

//        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName))
//            openAirplaneSettings()

        binding.bottomAppsList.itemAnimator = null

        binding.bottomAppsOverlay.setOnDragListener(this)
        binding.appPages.setOnDragListener(this)
    }


    private fun askRole() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            with(getSystemService(RoleManager::class.java)) {
//                if (!isRoleAvailable(RoleManager.ROLE_HOME) || isRoleHeld(RoleManager.ROLE_HOME)) return
//                startActivityForResult(createRequestRoleIntent(RoleManager.ROLE_HOME), 200)
//            }
//        }
    }

    private fun calculateAppItemViewHeight() {
        binding.appPages.post {
            binding.appPages.adapter = viewPagerAdapter
            binding.appPages.offscreenPageLimit = viewPagerAdapter.count
        }
    }


    private fun launchApp(installedApp: InstalledApp) {
        packageManager.getLaunchIntentForPackage(installedApp.packageName)
            ?.let(::startActivity)
    }


    override fun onBackPressed() {
        askRole()
    }


    override fun onDrag(v: View, event: DragEvent): Boolean {
        return when (v.id) {
            R.id.appPages -> handleEventMainAppList(v, event)
            R.id.bottomAppsOverlay -> handleBottomAppList(v, event)
            else -> false
        }
    }

    private fun handleBottomAppList(v: View, event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                if (viewModel.isFirstItem(dragInfo)) viewModel
                    .insertFirstItemToBottomBar(dragInfo)
                else viewModel.insertItemToBottomBar(dragInfo, getItemPosition(event.x))
            }
            DragEvent.ACTION_DRAG_EXITED -> viewModel
                .deleteItemFromAppsBarList(dragInfo)
        }
        return true
    }

    private fun handleEventMainAppList(v: View, event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false

        if (event.action == DragEvent.ACTION_DRAG_LOCATION)
            handleItemMovement(event.x, event.y, dragInfo)

        if (event.action == DragEvent.ACTION_DRAG_EXITED)
            (event.localState as? DragInfo)?.removeItem()

        if (event.action == DragEvent.ACTION_DRAG_ENTERED) {
            (event.localState as? DragInfo)?.restoreItem()
        }
        return true
    }

    private fun handleItemMovement(x: Float, y: Float, dragInfo: DragInfo) {
        val currentRecycler = viewPagerAdapter
            .getCurrentAppListView(binding.appPages.currentItem) ?: return

        if (currentRecycler.itemAnimator?.isRunning == true) return
        val currentView = currentRecycler.findChildViewUnder(x, y)

        if (currentView == null) {
            viewPagerAdapter.removeRequestToSwap()
            return
        }
        val holder = currentRecycler.getChildViewHolder(currentView)

        viewPagerAdapter.swapItem(dragInfo, holder.adapterPosition)
    }


    private fun getItemPosition(x: Float): Array<Int?> {

        binding.pointer.x = x - binding.pointer.width / 2
        binding.pointer.layoutParams.width = binding.pointer.height

        binding.pointer.requestLayout()

        val pointerRect = Rect()
        
        binding.pointer.getGlobalVisibleRect(pointerRect)

        val sideItemIndexes = arrayOf<Int?>(null, null)

        (0 until viewModel.getBottomAppsItemCount())
            .mapNotNull { binding.bottomAppsList.findViewHolderForLayoutPosition(it) }
            .map { it as BaseAdapter.BaseItem<*, *> }
            .forEach {
                val holderRect = Rect()
                it.itemView.getGlobalVisibleRect(holderRect)
                if (holderRect.intersect(pointerRect))
                    if (pointerRect.centerX() >= holderRect.centerX())
                        sideItemIndexes[1] = it.adapterPosition
                    else sideItemIndexes[0] = it.adapterPosition
            }

        if (sideItemIndexes.filterNotNull().isEmpty()) {
            val mainViewRect = Rect()
            binding.bottomAppsOverlay.getGlobalVisibleRect(mainViewRect)
            if (pointerRect.centerX() <= mainViewRect.centerX()) sideItemIndexes[0] = 0
            else sideItemIndexes[1] = 0
        }
        return sideItemIndexes
    }


    private fun createVPAdapter(): VPAdapter {
        val rowCount = binding.appPages.height / (binding.appPages.width / APP_COLUMN_COUNT * 1.1f)
        val visibleItemCountOnPageScreen = rowCount.toInt() * APP_COLUMN_COUNT
        return VPAdapter(viewModel.getApplicationList(), visibleItemCountOnPageScreen, ::launchApp)
    }
}