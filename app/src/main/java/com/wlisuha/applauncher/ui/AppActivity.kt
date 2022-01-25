package com.wlisuha.applauncher.ui

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.base.BaseActivity
import com.wlisuha.applauncher.data.DragInfo
import com.wlisuha.applauncher.databinding.AppActivityBinding
import com.wlisuha.applauncher.utils.APP_COLUMN_COUNT
import com.wlisuha.applauncher.utils.MOVING_PAGE_DELAY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AppActivity : BaseActivity<AppViewModel, AppActivityBinding>(R.layout.app_activity),
    View.OnDragListener {

    private lateinit var viewPagerAdapter: VPAdapter
    override val viewModel: AppViewModel by viewModels()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> handleAddedApplication(intent.data ?: return)
                Intent.ACTION_PACKAGE_REMOVED -> handleRemovedApplication(intent.data ?: return)
            }
        }
    }

    private fun handleRemovedApplication(data: Uri) {
        Log.d("12345", data.toString())
        Log.d("12345", "removed")
    }

    private fun handleAddedApplication(data: Uri) {
        Log.d("12345", data.toString())
        Log.d("12345", "added")
    }

    override fun setupUI() {
        binding.fakeNavBar.layoutParams.height = with(getNavBarSize()) {
            if (!hasNavigationBar()) this
            else this / 2
        }
        calculateAppItemViewHeight()
        Log.d("12345", "${!hasNavigationBar()}}")
//        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName))
//            openAirplaneSettings()
        binding.bottomAppsList.itemAnimator = null

        binding.bottomAppsOverlay.setOnDragListener(this)
        binding.appPages.setOnDragListener(this)

        binding.leftTriggeredView.setOnDragListener(this)
        binding.rightTriggeredView.setOnDragListener(this)

        binding.motionView.setOnLongClickListener {
            viewModel.isSelectionEnabled.set(!(viewModel.isSelectionEnabled.get() ?: false))
            true
        }
        registerReceiver(broadcastReceiver, viewModel.intentFilter)
    }

    private fun askRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            with(getSystemService(RoleManager::class.java)) {
                if (!isRoleAvailable(RoleManager.ROLE_HOME) || isRoleHeld(RoleManager.ROLE_HOME)) return
                startActivityForResult(createRequestRoleIntent(RoleManager.ROLE_HOME), 200)
            }
        }
    }

    private fun calculateAppItemViewHeight() {
        binding.appPages.post {
            lifecycleScope.launch(Dispatchers.Main) {
                viewPagerAdapter = withContext(Dispatchers.IO) { createVPAdapter() }
                binding.appPages.adapter = viewPagerAdapter
                binding.appPages.offscreenPageLimit = viewPagerAdapter.count
                binding.pageIndicator.setViewPager(binding.appPages)
            }
        }
    }

    override fun onBackPressed() {
        //  askRole()
    }

    override fun onDrag(v: View, event: DragEvent): Boolean {
        return when (v.id) {
            R.id.appPages -> handleEventMainAppList(v, event)
            R.id.bottomAppsOverlay -> handleBottomAppList(v, event)
            R.id.leftTriggeredView -> handleLeftTrigger(event)
            R.id.rightTriggeredView -> handleRightTrigger(event)
            else -> false
        }
    }

    private fun handleLeftTrigger(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> if (viewModel.movePageJob?.isActive == true) return true
            else viewModel.movePageJob = lifecycleScope.launch(Dispatchers.IO) {
                while (binding.appPages.currentItem > 0) {
                    delay(MOVING_PAGE_DELAY)
                    withContext(Dispatchers.Main) { binding.appPages.currentItem-- }
                }
            }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> viewModel.movePageJob?.cancel()
        }
        return true
    }

    private fun handleRightTrigger(event: DragEvent): Boolean {
        val dragInfo = event.localState as DragInfo
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> if (viewModel.movePageJob?.isActive == true) return true
            else viewModel.movePageJob = lifecycleScope.launch(Dispatchers.IO) {
                while (binding.appPages.currentItem < viewPagerAdapter.count - 1) {
                    delay(MOVING_PAGE_DELAY)
                    withContext(Dispatchers.Main) { binding.appPages.currentItem++ }
                }
                delay(MOVING_PAGE_DELAY)
                withContext(Dispatchers.Main) {
                    if (viewPagerAdapter.createPage(binding.appPages.currentItem)) binding.appPages.currentItem++
                    viewPagerAdapter.insertToLastPosition(dragInfo, binding.appPages.currentItem)
                }
            }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                viewModel.movePageJob?.cancel()
            }
        }
        return true
    }

    private fun handleBottomAppList(v: View, event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                if (viewModel.isFirstItem(dragInfo)) viewModel
                    .insertFirstItemToBottomBar(dragInfo)
                else viewModel.insertItemToBottomBar(dragInfo, getItemPosition(event))
            }
            DragEvent.ACTION_DRAG_ENTERED ->
                if (dragInfo.currentPage != -1) {
                    Log.d("12345", "remove")
                    //TODO
                    //dragInfo.removeItem()
                }
            DragEvent.ACTION_DRAG_EXITED -> viewModel
                .deleteItemFromAppsBarList(dragInfo)
        }
        return true
    }

    private fun handleEventMainAppList(v: View, event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> handleItemMovement(event.x, event.y, dragInfo)
            DragEvent.ACTION_DRAG_ENTERED -> (event.localState as? DragInfo)?.restoreItem()
            DragEvent.ACTION_DRAG_EXITED -> viewPagerAdapter.removeRequestToSwap()
        }
        return true
    }

    private fun handleItemMovement(x: Float, y: Float, dragInfo: DragInfo) {

        val currentPage = binding.appPages.currentItem

        val currentRecycler = viewPagerAdapter.getCurrentAppListView(currentPage) ?: return

        if (currentRecycler.itemAnimator?.isRunning == true) return
        val currentView = currentRecycler.findChildViewUnder(x, y)
        if (currentView == null) {
            viewPagerAdapter.removeRequestToSwap()
            viewPagerAdapter.insertToLastPosition(dragInfo, currentPage)
            return
        }
        val holder = currentRecycler.getChildViewHolder(currentView)
        viewPagerAdapter.swapItem(dragInfo, holder.adapterPosition, currentPage)
    }


    private fun getItemPosition(dragEvent: DragEvent): Int? {
        val dragInfo = dragEvent.localState as DragInfo

        val currentView = binding.bottomAppsList
            .findChildViewUnder(
                dragEvent.x - binding.bottomAppsList.x,
                dragEvent.y + binding.bottomAppsOverlay.y
            ) ?: return if (dragEvent.x <= binding.bottomAppsOverlay.width / 2) 0
        else viewModel.getBottomAppsItemCount()

        val currentHolder = binding.bottomAppsList
            .findContainingViewHolder(currentView)
            ?.takeIf { viewModel.bottomAppListAdapter.getData()[it.layoutPosition].packageName != dragInfo.draggedItem.packageName }
            ?: return null

        return Rect()
            .apply { currentView.getGlobalVisibleRect(this) }
            .let { dragEvent.x <= it.centerX() }
            .let { isLeftSide ->
                if (isLeftSide) return currentHolder.layoutPosition
                else currentHolder.layoutPosition + 1
            }
    }

    private suspend fun createVPAdapter(): VPAdapter {
        val rowCount =
            binding.appPages.height / (binding.appPages.width / APP_COLUMN_COUNT * 1.1f)
        val visibleItemCountOnPageScreen = rowCount.toInt() * APP_COLUMN_COUNT
        return VPAdapter(
            viewModel.readAllPackage(visibleItemCountOnPageScreen),
            visibleItemCountOnPageScreen,
            viewModel,
        )
    }

    private fun getNavBarSize() =
        with(resources.getIdentifier("navigation_bar_height", "dimen", "android")) {
            if (this != 0) resources.getDimensionPixelSize(this)
            else 0
        }

    private fun hasNavigationBar(): Boolean {
        val id: Int = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }
}