package com.iosapp.ioslauncher.ui.custom

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.iosapp.ioslauncher.BR
import com.iosapp.ioslauncher.R
import com.iosapp.ioslauncher.data.DragInfo
import com.iosapp.ioslauncher.databinding.LauncherViewBinding
import com.iosapp.ioslauncher.ui.app.AppListAdapter
import com.iosapp.ioslauncher.ui.app.AppViewModel
import com.iosapp.ioslauncher.ui.bg.BackgroundsActivity
import com.iosapp.ioslauncher.utils.APP_COLUMN_COUNT
import com.iosapp.ioslauncher.utils.MOVING_PAGE_DELAY
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class LauncherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), View.OnDragListener,
    NonSwipeableViewPager.StateProvider, MotionLayout.TransitionListener,
    View.OnLongClickListener, CoroutineScope by MainScope() {

    lateinit var viewPager: NonSwipeableViewPager
    val binding: LauncherViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.launcher_view,
        this,
        true
    )

    val viewModel: AppViewModel get() = binding.viewModel!!

    private lateinit var viewPagerAdapter: AppListAdapter
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> handleAddedApplication(intent.data ?: return)
                Intent.ACTION_PACKAGE_REMOVED -> handleRemovedApplication(intent.data ?: return)
            }
        }
    }
    private val touchRect by lazy {
        Rect(0, 0, binding.indicatorOverlayMax.width, binding.indicatorOverlayMax.height)
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

    fun setViewModel(viewModel: AppViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.notifyChange()
        initView()
    }

    private fun initView() {
        binding.fakeNavBar.layoutParams.height = with(getNavBarSize()) {
            if (!hasNavigationBar()) this
            else this / 2
        }
        calculateAppItemViewHeight()
        setTouchListenerOnIndicator()
        viewModel.stateProvider = this
        binding.bottomAppsList.itemAnimator = null
        binding.motionView.addTransitionListener(this)
        binding.bottomAppsOverlay.setOnDragListener(this)
        binding.appPages.setOnDragListener(this)
        binding.motionView.setOnLongClickListener(this)
        context.registerReceiver(broadcastReceiver, viewModel.intentFilter)
        binding.viewList.binding.settingsButton.setOnClickListener {
            Intent(context, BackgroundsActivity::class.java)
                .let(context::startActivity)
        }
    }

    private fun getRectWidth() = (binding.appPages.width * 0.08).toInt()

    private fun handleRemovedApplication(data: Uri) {
        viewPagerAdapter.onRemovedApp(data.encodedSchemeSpecificPart) { shiftPage ->
            binding.appPages.currentItem += shiftPage
        }
    }

    private fun handleAddedApplication(data: Uri) {
        viewModel.createModel(data.encodedSchemeSpecificPart)
            .takeIf { viewModel.availableApp(context.packageName) }
            ?.let(viewPagerAdapter::onNewApp)
    }

    private fun calculateAppItemViewHeight() {
        binding.appPages.post {
            launch(Dispatchers.Main) {
                viewPagerAdapter = withContext(Dispatchers.IO) { createVPAdapter() }
                binding.appPages.adapter = viewPagerAdapter
                binding.pageIndicator.setViewPager(binding.appPages)
            }
        }
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
                val oldPage = dragInfo.currentPage
                viewModel.insertItemToBottomBar(dragInfo, getItemPosition(event))
                viewPagerAdapter.checkForRemovePage(oldPage) { binding.appPages.currentItem += it }
                binding.pageIndicator.setViewPager(binding.appPages)
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                viewPagerAdapter.clearRequests()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                viewPagerAdapter.canCreatePage = true
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
            .insertItemToPosition(currentPage, holder.layoutPosition, dragInfo)
        else viewPagerAdapter
            .swapItem(dragInfo, holder.layoutPosition, currentPage)
    }

    private fun stopMovePagesJob() {
        viewModel.movePageJob?.cancel()
        viewModel.movePageJob = null
    }

    private fun movePagesRight(dragInfo: DragInfo) {
        viewModel.movePageJob = launch(Dispatchers.IO) {
            while (binding.appPages.currentItem < viewPagerAdapter.count - 1) {
                delay(MOVING_PAGE_DELAY)
                withContext(Dispatchers.Main) { binding.appPages.currentItem++ }
            }
            delay(MOVING_PAGE_DELAY)
            withContext(Dispatchers.Main) {
                if (viewPagerAdapter.createPage()) {
                    binding.appPages.currentItem++
                    viewPagerAdapter
                        .insertToLastPosition(dragInfo, binding.appPages.currentItem, false)
                }
            }
            stopMovePagesJob()
        }
    }

    private fun movePagesLeft() {
        viewModel.movePageJob = launch(Dispatchers.IO) {
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
                val itemPosition = if (isLeftSide) return currentHolder.layoutPosition
                else currentHolder.layoutPosition + 1
                itemPosition
            }
    }

    private suspend fun createVPAdapter(): AppListAdapter {
        val rowCount =
            binding.appPages.height / ((binding.appPages.width / APP_COLUMN_COUNT) * 1.2f)

        val visibleItemCountOnPageScreen = rowCount.toInt() * APP_COLUMN_COUNT
        return AppListAdapter(
            viewModel.readAllPackage(visibleItemCountOnPageScreen),
            visibleItemCountOnPageScreen,
            viewModel,
            this,
            binding.motionView
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
        binding.indicatorOverlayMax.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> viewPager.canSwipe = false
                MotionEvent.ACTION_UP -> viewPager.canSwipe = true
            }
            binding.motionView.canCallLongCLick = false
            if (touchRect.contains(event.x.toInt(), event.y.toInt())) handleMovingPages(event.x)
            true
        }
    }

    private fun handleMovingPages(touchXPosition: Float) {
        val percent = touchXPosition / binding.indicatorOverlayMax.width
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

        val roundProgress = (progress * 100).toInt() / 100f

        val canSwipeViewPager = roundProgress < 0.1f

        binding.motionView.longClickTask?.cancel()

        viewModel.disableSelection = !canSwipeViewPager
        viewPager.canSwipe = canSwipeViewPager
        binding.appPages.canSwipe = canSwipeViewPager

        with(binding.viewList) {
            if (canSwipeViewPager) onHide()
            else onShow()
        }
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
        if (currentId == R.id.end) viewModel.isSelectionEnabled.set(false)
        if (currentId == R.id.start) {
            viewModel.disableSelection = false
        }
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {

    }

    override fun isPresentOnHomeScreen() = binding.motionView.progress < 0.2f

    override fun onAppSelected() {
        binding.motionView.canCallLongCLick = false
    }

    override fun onLongClick(p0: View?): Boolean {
        viewModel.isSelectionEnabled.set(!(viewModel.isSelectionEnabled.get() ?: false))
        return true
    }

}