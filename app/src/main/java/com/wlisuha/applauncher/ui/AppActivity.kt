package com.wlisuha.applauncher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.audiofx.BassBoost
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.view.DragEvent
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.motion.widget.MotionLayout
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
import kotlin.math.roundToInt


class AppActivity : BaseActivity<AppViewModel, AppActivityBinding>(R.layout.app_activity),
    View.OnDragListener, MotionLayout.TransitionListener {

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
    private val touchRect by lazy {
        Rect(0, 0, binding.indicatorTouch.width, binding.indicatorTouch.height)
    }

    private val moveLeftRect by lazy {
        Rect(0, 0, getRectWidth(), binding.appPages.height)
    }

    private val moveRightRect by lazy {
        Rect(
            binding.appPages.width - getRectWidth(),
            0,
            binding.appPages.width,
            binding.appPages.height
        )
    }

    private fun getRectWidth() = (binding.appPages.width * 0.08).toInt()

    private fun handleRemovedApplication(data: Uri) {
        viewPagerAdapter.onRemovedApp(data.encodedSchemeSpecificPart) { shiftPage ->
            binding.appPages.currentItem += shiftPage
        }
    }

    private fun handleAddedApplication(data: Uri) {
        viewModel.createModel(data.encodedSchemeSpecificPart)
            .takeIf { it.packageName != packageName }
            ?.let(viewPagerAdapter::onNewApp)
    }

    override fun setupUI() {
        binding.fakeNavBar.layoutParams.height = with(getNavBarSize()) {
            if (!hasNavigationBar()) this
            else this / 2
        }
        calculateAppItemViewHeight()
        setTouchListenerOnIndicator()
        checkNotificationsPermissions()

        askSettingsPermissions()

        binding.bottomAppsList.itemAnimator = null
        binding.motionView.addTransitionListener(this)
        binding.bottomAppsOverlay.setOnDragListener(this)
        binding.appPages.setOnDragListener(this)
        binding.motionView.setOnLongClickListener {
            viewModel.isSelectionEnabled.set(!(viewModel.isSelectionEnabled.get() ?: false))
            true
        }
        registerReceiver(broadcastReceiver, viewModel.intentFilter)
    }

    private fun askSettingsPermissions() {
        val intent = Intent(ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun checkNotificationsPermissions() {
        if (!binding.viewList.hasNotificationPermission())
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .let(::startActivity)
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
        askRole()
    }

    override fun onDrag(v: View, event: DragEvent): Boolean {
        return when (v.id) {
            R.id.appPages -> handleEventMainAppList(event)
            R.id.bottomAppsOverlay -> handleBottomAppList(event)
            else -> false
        }
    }

    private fun handleBottomAppList(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                viewModel.insertItemToBottomBar(dragInfo, getItemPosition(event))
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                viewPagerAdapter.clearRequests()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                dragInfo.adapter = viewModel.bottomAppListAdapter
                dragInfo.currentPage = -1
            }
        }
        return true
    }

    private fun handleEventMainAppList(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> handleItemMovement(event.x, event.y, dragInfo)
            DragEvent.ACTION_DROP -> stopMovePagesJob()
        }
        return true
    }

    private fun handleItemMovement(x: Float, y: Float, dragInfo: DragInfo) {
        when {
            moveLeftRect.contains(x.toInt(), y.toInt()) -> {
                viewPagerAdapter.clearRequests()
                if (viewModel.movePageJob == null) movePagesLeft()
                return
            }
            moveRightRect.contains(x.toInt(), y.toInt()) -> {
                viewPagerAdapter.clearRequests()
                if (viewModel.movePageJob == null) movePagesRight(dragInfo)
                return
            }
            viewModel.movePageJob != null -> stopMovePagesJob()
        }

        val currentPage = binding.appPages.currentItem

        val currentRecycler = viewPagerAdapter.getCurrentAppListView(currentPage)

        if (currentRecycler.itemAnimator?.isRunning == true) return
        val currentView = currentRecycler.findChildViewUnder(x, y)
        if (currentView == null) {
            viewPagerAdapter.insertToLastPosition(dragInfo, currentPage, true)
            return
        }
        val holder = currentRecycler.getChildViewHolder(currentView)

        if (dragInfo.currentPage == -1) viewPagerAdapter
            .insertItemToPosition(currentPage, holder.adapterPosition, dragInfo)
        else viewPagerAdapter
            .swapItem(dragInfo, holder.adapterPosition, currentPage)
    }

    private fun stopMovePagesJob() {
        viewModel.movePageJob?.cancel()
        viewModel.movePageJob = null
    }

    private fun movePagesRight(dragInfo: DragInfo) {
        viewModel.movePageJob = lifecycleScope.launch(Dispatchers.IO) {
            while (binding.appPages.currentItem < viewPagerAdapter.count - 1) {
                delay(MOVING_PAGE_DELAY)
                withContext(Dispatchers.Main) { binding.appPages.currentItem++ }
            }
            delay(MOVING_PAGE_DELAY)
            withContext(Dispatchers.Main) {
                if (viewPagerAdapter.createPage()) {
                    binding.appPages.currentItem++
                    viewPagerAdapter.insertToLastPosition(
                        dragInfo,
                        binding.appPages.currentItem,
                        true
                    )
                }
            }
            stopMovePagesJob()
        }
    }

    private fun movePagesLeft() {
        viewModel.movePageJob = lifecycleScope.launch(Dispatchers.IO) {
            while (binding.appPages.currentItem > 0) {
                delay(MOVING_PAGE_DELAY)
                withContext(Dispatchers.Main) { binding.appPages.currentItem-- }
            }
        }
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


    @SuppressLint("ClickableViewAccessibility")
    fun setTouchListenerOnIndicator() {
        binding.indicatorTouch.setOnTouchListener { _, event ->
            if (touchRect.contains(event.x.toInt(), event.y.toInt()))
                handleMovingPages(event.x)
            return@setOnTouchListener true
        }
    }

    private fun handleMovingPages(touchXPosition: Float) {
        val percent = touchXPosition / binding.indicatorTouch.width
        binding.appPages.currentItem = (viewPagerAdapter.count * percent).roundToInt()
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

    }

    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {

    }

    override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
        with(binding.viewList) {
            if (currentId == R.id.start) onHide()
            else onShow()
        }
    }


    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {

    }
}