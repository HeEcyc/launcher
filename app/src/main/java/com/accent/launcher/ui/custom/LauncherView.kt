package com.accent.launcher.ui.custom

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.accent.launcher.BR
import com.accent.launcher.LauncherApplication
import com.accent.launcher.R
import com.accent.launcher.base.BaseAdapter
import com.accent.launcher.data.DesktopCell
import com.accent.launcher.data.DragInfo
import com.accent.launcher.data.InstalledApp
import com.accent.launcher.data.Prefs
import com.accent.launcher.databinding.BubbleBinding
import com.accent.launcher.databinding.LauncherItemApplicationBinding
import com.accent.launcher.databinding.LauncherViewBinding
import com.accent.launcher.ui.app.AppListAdapter
import com.accent.launcher.ui.app.AppViewModel
import com.accent.launcher.ui.pack.PackActivity
import com.accent.launcher.utils.APP_COLUMN_COUNT
import com.accent.launcher.utils.MOVING_PAGE_DELAY
import com.accent.launcher.utils.PAGE_INDEX_JUST_MENU
import kotlinx.coroutines.*
import me.everything.android.ui.overscroll.IOverScrollDecor
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.lang.reflect.Method
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class LauncherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), View.OnDragListener,
    NonSwipeableViewPager.StateProvider, MotionLayout.TransitionListener,
    View.OnLongClickListener, CoroutineScope by MainScope() {

    val binding: LauncherViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.launcher_view,
        this,
        true
    )

    val viewModel: AppViewModel get() = binding.viewModel!!

    private var viewPagerAdapter: AppListAdapter? = null
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

    private var lastTimeIdleMillis = System.currentTimeMillis()
    @SuppressLint("WrongConstant")
    private val onRefreshListener = OnRefreshListener {
        val sbs = context.getSystemService("statusbar")
        val statusbarManager = Class.forName("android.app.StatusBarManager")
        val showsb: Method = statusbarManager.getMethod("expandNotificationsPanel")
        showsb.invoke(sbs)
        binding.srl.isRefreshing = false
    }

    private var overScrollDecor: IOverScrollDecor? = null

    fun setViewModel(viewModel: AppViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.notifyChange()
        initView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding.menuPages.offscreenPageLimit = 9
        binding.arrow.state = -1f
        binding.menu.post { binding.menu.alpha = 0f }
        binding.appsContainer.layoutParams.let { it as LayoutParams }.setMargins(0, getStatusBarHeight(), 0, 0)
        binding.bubbleContainer.layoutParams.let { it as LinearLayout.LayoutParams }.setMargins(0, getStatusBarHeight(), 0, 0)

        binding.fakeNavBar.layoutParams.height = if (hasNavigationBar()) getNavBarSize() * 2 else binding.fakeNavBar.layoutParams.height
        binding.fakeSearchBarBottom.layoutParams.height = if (hasNavigationBar()) getNavBarSize() else 1
        binding.searchBarBottom.layoutParams.height = binding.fakeSearchBarBottom.layoutParams.height

        calculateAppItemViewHeight()
        setTouchListenerOnIndicator()
        viewModel.stateProvider = this
        binding.bottomAppsList.itemAnimator = null
        binding.motionView.addTransitionListener(this)
        binding.bottomAppsOverlay.setOnDragListener(this)
        binding.appPages.setOnDragListener(this)
        binding.topFields.setOnDragListener(this)
        binding.fieldDelete.setOnDragListener(this)
        binding.fieldRemove.setOnDragListener(this)
        binding.motionView.setOnDragListener(this)
        binding.motionView.setOnLongClickListener(this)
        context.registerReceiver(broadcastReceiver, viewModel.intentFilter)
        val lifecycleOwner = findViewTreeLifecycleOwner()!!
        viewModel.onMenuItemLongClick.observe(lifecycleOwner) {
//            binding.motionView.progress = 0f
            onAppSelected()
        }
        viewModel.disableMotionLayoutLongClick.observe(lifecycleOwner) {
            binding.motionView.canCallLongCLick = false
        }
        binding.srl.setProgressViewOffset(false, -200, -300)
        binding.srl.setOnRefreshListener(onRefreshListener)
        viewModel.viewModelScope.launch {
            while (true) {
                delay(200)
                val pos = binding.srl.children.first { it is ImageView }.y
                if (pos == -200f)
                    lastTimeIdleMillis = System.currentTimeMillis()
                else {
                    if (System.currentTimeMillis() - lastTimeIdleMillis >= 500) {
                        onRefreshListener.onRefresh()
                    }
                }
            }
        }
        binding.menuTouchInterceptor.setOnTouchListener { _, _ -> true }
        viewModel.showTopFields.observe(lifecycleOwner) {
            if (!viewModel.isAppSystem(it.packageName)) binding.fieldDelete.visibility = View.VISIBLE
            showTopFields()
        }
        overScrollDecor = OverScrollDecoratorHelper.setUpOverScroll(binding.appPages)
        overScrollDecor?.setOverScrollStateListener { _, _, _ -> hideArrowShowDots() }
        overScrollDecor?.setOverScrollUpdateListener { _, _, _ -> hideArrowShowDots() }
        binding.appPages.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    viewModel.viewModelScope.launch(Dispatchers.Main) {
                        delay(250)
                        showArrowHideDots()
                    }
                } else hideArrowShowDots()
            }
        })
        binding.menuTabBar.post { binding.menuTabBar.bindMenuAdapter(viewModel.menuAdapter) }
        binding.menuTabBar.onItemClick.observe(lifecycleOwner) {
            binding.menuPages.currentItem = binding.menuTabBar.currentTab
        }
        binding.menuPages.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                binding.menuTabBar.currentTab = position
            }
            override fun onPageScrollStateChanged(state: Int) {}
        })
        viewModel.scrollResultsToStart.observe(lifecycleOwner) {
            binding.searchResults.smoothScrollToPosition(0)
        }
        Prefs.onIconPackChanged.observe(lifecycleOwner) {
            val pm = LauncherApplication.instance.packageManager
            viewPagerAdapter?.listAppPages?.flatten()?.forEach {
                it.app.get()?.let { app -> app.icon.set(Prefs.iconPack.getAppIcon(app.applicationInfo, pm)) }
            }
            viewModel.menuAdapter.getData().flatMap { it.apps }.forEach {
                it?.icon?.set(Prefs.iconPack.getAppIcon(it.applicationInfo, pm))
            }
            viewModel.bottomAppListAdapter.getData().forEach {
                it.icon.set(Prefs.iconPack.getAppIcon(it.applicationInfo, pm))
            }
            viewModel.recentAdapter.getData().forEach {
                it?.icon?.set(Prefs.iconPack.getAppIcon(it.applicationInfo, pm))
            }
        }
        binding.buttonSettings.setOnClickListener {
            context.startActivity(Intent(context, PackActivity::class.java))
        }
    }

    private var arrowVisibilityAnimator: ValueAnimator? = null

    private fun showArrowHideDots() {
        arrowVisibilityAnimator?.cancel()
        val onCompleteEnd = { _: Animator -> arrowVisibilityAnimator = null }
        arrowVisibilityAnimator = ValueAnimator.ofFloat(binding.pageIndicator.alpha, 0f).apply {
            interpolator = LinearInterpolator()
            duration = 125
            addUpdateListener { binding.pageIndicator.alpha = it.animatedValue as Float }
            addListener(
                onCancel = onCompleteEnd,
                onEnd = {
                    arrowVisibilityAnimator = ValueAnimator.ofFloat(binding.arrow.alpha, 1f).apply {
                        interpolator = LinearInterpolator()
                        duration = 125
                        addUpdateListener { binding.arrow.alpha = it.animatedValue as Float }
                        addListener(onCancel = onCompleteEnd, onEnd = onCompleteEnd)
                        start()
                    }
                }
            )
            start()
        }
    }

    private fun hideArrowShowDots() {
        arrowVisibilityAnimator?.cancel()
        arrowVisibilityAnimator = null
        binding.arrow.alpha = 0f
        binding.pageIndicator.alpha = 1f
    }

    private var topFieldsAnimator: ValueAnimator? = null
    private var topFieldsEndValue = 0f

    private fun showTopFields() {
        if (topFieldsEndValue == 1f) return
        topFieldsEndValue = 1f
        topFieldsAnimator?.cancel()
        val currentPosition = binding.topFields.translationY / (binding.topFields.height * 0.75f)
        val targetMenuScaleFactor = (binding.appPages.height - binding.topFields.height * 0.75f) / binding.appPages.height
        val targetMenuScaleFactorDelta = binding.appPages.scaleX - targetMenuScaleFactor
        val currentMenuScaleFactor = binding.appPages.scaleX
        topFieldsAnimator = ValueAnimator.ofFloat(currentPosition, 1f).apply {
            interpolator = LinearInterpolator()
            duration = 100
            addUpdateListener {
                val newValue = it.animatedValue as Float
                binding.topFields.translationY = (binding.topFields.height * 0.75f) * newValue
                val newScaleFactor = currentMenuScaleFactor - targetMenuScaleFactorDelta * newValue
                binding.appPages.scaleX = newScaleFactor
                binding.appPages.scaleY = newScaleFactor
                binding.appPages.translationY = (binding.topFields.height * 0.75f) / 2f * newValue
            }
            addListener(
                onEnd = { topFieldsAnimator = null },
                onCancel = { topFieldsAnimator = null }
            )
            start()
        }
    }

    private fun hideTopFields() {
        if (topFieldsEndValue == 0f) return
        topFieldsEndValue = 0f
        topFieldsAnimator?.cancel()
        val currentPosition = binding.topFields.translationY / (binding.topFields.height * 0.75f)
        val targetMenuScaleFactor = 1f
        val targetMenuScaleFactorDelta = targetMenuScaleFactor - binding.appPages.scaleX
        topFieldsAnimator = ValueAnimator.ofFloat(currentPosition, 0f).apply {
            interpolator = LinearInterpolator()
            duration = 200
            addUpdateListener {
                val newValue = it.animatedValue as Float
                binding.topFields.translationY = (binding.topFields.height * 0.75f) * newValue
                val newScaleFactor = targetMenuScaleFactor - targetMenuScaleFactorDelta * newValue
                binding.appPages.scaleX = newScaleFactor
                binding.appPages.scaleY = newScaleFactor
                binding.appPages.translationY = (binding.topFields.height * 0.75f) / 2f * newValue
            }
            addListener(
                onEnd = {
                    topFieldsAnimator = null
                    binding.fieldDelete.visibility = View.GONE
                },
                onCancel = {
                    topFieldsAnimator = null
                    binding.fieldDelete.visibility = View.GONE
                }
            )
            start()
        }
    }

    private fun getStatusBarHeight() =
        resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android")).takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.statusBarHeight)

    private fun getRectWidth() = (binding.appPages.width * 0.08).toInt()

    private fun handleRemovedApplication(data: Uri) {
        viewPagerAdapter?.onRemovedApp(data.encodedSchemeSpecificPart) { shiftPage ->
            binding.appPages.currentItem += shiftPage
        }
    }

    private fun handleAddedApplication(data: Uri) {
        viewModel.createModel(data.encodedSchemeSpecificPart)
            .takeIf { viewModel.availableApp(it.packageName) }
//            ?.let(viewPagerAdapter::onNewApp)
            ?.let(viewModel::onAppInstalled)
    }

    private fun setPageIndicatorViewPager(viewPager: ViewPager) {
        launch(Dispatchers.Main) {
            while (viewPager.adapter === null) delay(100)
            binding.pageIndicator.setViewPager(viewPager)
        }
    }

    private fun calculateAppItemViewHeight() {
        binding.appPages.post {
            launch(Dispatchers.Main) {
                viewPagerAdapter = withContext(Dispatchers.IO) { createVPAdapter() }
                binding.appPages.adapter = viewPagerAdapter
//                binding.pageIndicator.setViewPager(binding.appPages)
                setPageIndicatorViewPager(binding.appPages)
            }
        }
    }

    override fun onDrag(v: View, event: DragEvent): Boolean {
        return (handleEventOriginPlace(v, event) || when (v.id) {
            R.id.appPages -> handleEventMainAppList(event)
            R.id.bottomAppsOverlay -> handleBottomAppList(event)
            R.id.topFields -> handleTopFields(event)
            R.id.fieldDelete -> handleFieldDelete(event)
            R.id.fieldRemove -> handleFieldRemove(event)
            R.id.motionView -> true
            else -> false
        }).also {
            if (it && event.action == DragEvent.ACTION_DRAG_ENDED) {
                translateViewUp { hideTopFields() }
            }
        }
    }

    private fun handleEventOriginPlace(v: View, event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo
        val originView = dragInfo?.originView
        if (originView !== null) {
            if (v === dragInfo.originView) {
                dragInfo.hasCheckedIfOverOriginal = true
                if (dragInfo.timeMillisSinceDragBegin >= 200) {
                    if (!viewModel.hasAppInfoBubble.get())
                        showBubble(dragInfo.draggedItem, originView)
                }
                return true
            } else {
                if (dragInfo.hasCheckedIfOverOriginal) {
                    if (event.action !in listOf(DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DROP))
                        return true
                    val left = 0
                    val top = 0
                    val right = originView.width.times(0.5).toInt()
                    val bottom = originView.height.times(0.5).toInt()
                    val originViewRect = Rect(left, top, right, bottom)
                    val originViewPoint = Point(originView.width.times(0.25).toInt(),originView.height.times(0.25).toInt())
                    binding.root.let { it as ViewGroup }.getChildVisibleRect(dragInfo.originView, originViewRect, originViewPoint)
                    if (originViewRect.contains(event.x.toInt(), event.y.toInt())) return true
                } else
                    return true
            }
        }
        if (binding.motionView.progress > 0f)
            binding.motionView.progress = 0f
        viewModel.hasAppInfoBubble.set(false)
        return false
    }

    private fun showBubble(app: InstalledApp, originView: View) {
        binding.bubbleContainer.removeAllViews()
        viewModel.hasAppInfoBubble.set(true)
        binding.bubbleContainer.post {
            val upperSpace = originView.y
            val lowerSpace = binding.bubbleContainer.height - originView.y - originView.height
            val aboveOriginal = upperSpace >= lowerSpace
            val bubble = BubbleBinding.inflate(LayoutInflater.from(context))
            bubble.root.layoutParams =
                LayoutParams(originView.width * 2, originView.height.times(0.7).toInt()).let { bodyParams ->
                    bodyParams.topToTop = LayoutParams.PARENT_ID
                    bodyParams.bottomToBottom = LayoutParams.PARENT_ID
                    bodyParams.startToStart = LayoutParams.PARENT_ID
                    bodyParams.endToEnd = LayoutParams.PARENT_ID
                    bodyParams.verticalBias = if (aboveOriginal)
                        (originView.y - bodyParams.height) / (binding.bubbleContainer.height - bodyParams.height)
                    else
                        (originView.y + originView.height) / (binding.bubbleContainer.height - bodyParams.height)
                    bodyParams.horizontalBias =
                        (originView.x + originView.width / 2) / (binding.bubbleContainer.measuredWidth)
                    bubble.tail.layoutParams.let { it as LayoutParams }.let { tailParams ->
                        if (!aboveOriginal) {
                            tailParams.topToBottom = LayoutParams.UNSET
                            tailParams.bottomToBottom = LayoutParams.UNSET
                            tailParams.topToTop = R.id.body
                            tailParams.bottomToTop = R.id.body
                        }
                        tailParams.horizontalBias = bodyParams.horizontalBias
                    }
                    bodyParams
                }
            binding.bubbleContainer.addView(bubble.root, bubble.root.layoutParams)
            bubble.body.setOnClickListener {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.addCategory(Intent.CATEGORY_DEFAULT)
                i.data = Uri.parse("package:" + app.packageName)
                context.startActivity(i)
                viewModel.hasAppInfoBubble.set(false)
            }
        }
    }

    private fun handleTopFields(event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> translateViewDown()
            DragEvent.ACTION_DRAG_EXITED -> translateViewUp()
        }
        return true
    }

    private fun handleFieldDelete(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_EXITED -> handleTopFields(event)
            DragEvent.ACTION_DROP -> viewModel.removeItem(dragInfo.draggedItem.packageName)
        }
        return true
    }

    private fun handleFieldRemove(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_EXITED -> handleTopFields(event)
            DragEvent.ACTION_DROP -> viewPagerAdapter?.removeShortcut(dragInfo)
        }
        return true
    }

    private var rootTranslationAnimator: ValueAnimator? = null
    private var rootTranslationEndValue = 0f

    private fun translateViewDown() {
        if (rootTranslationEndValue == 1f) return
        rootTranslationEndValue = 1f
        rootTranslationAnimator?.cancel()
        val statusBarHeight = getStatusBarHeight()
        val currentPosition = binding.root.translationY / statusBarHeight
        rootTranslationAnimator = ValueAnimator.ofFloat(currentPosition, 1f).apply {
            interpolator = LinearInterpolator()
            duration = 100
            addUpdateListener {
                val newValue = it.animatedValue as Float
                binding.root.translationY = statusBarHeight * newValue
            }
            addListener(
                onEnd = { rootTranslationAnimator = null },
                onCancel = { rootTranslationAnimator = null }
            )
            start()
        }
    }

    private fun translateViewUp(onEnd: () -> Unit = {}) {
        if (rootTranslationEndValue == 0f) {
            onEnd()
            return
        }
        rootTranslationEndValue = 0f
        rootTranslationAnimator?.cancel()
        val statusBarHeight = getStatusBarHeight()
        val currentPosition = binding.root.translationY / statusBarHeight
        rootTranslationAnimator = ValueAnimator.ofFloat(currentPosition, 0f).apply {
            interpolator = LinearInterpolator()
            duration = 100
            addUpdateListener {
                val newValue = it.animatedValue as Float
                binding.root.translationY = statusBarHeight * newValue
            }
            addListener(
                onEnd = { rootTranslationAnimator = null; onEnd() },
                onCancel = { rootTranslationAnimator = null; onEnd() }
            )
            start()
        }
    }

    private fun handleBottomAppList(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                val oldPage = dragInfo.currentPage
                viewModel.insertItemToBottomBar(dragInfo, getItemPosition(event))
                viewPagerAdapter?.checkForRemovePage(oldPage) { binding.appPages.currentItem += it }
//                binding.pageIndicator.setViewPager(binding.appPages)
                setPageIndicatorViewPager(binding.appPages)
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                viewPagerAdapter?.clearRequests()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
//                viewPagerAdapter.canCreatePage = true
            }
        }
        return true
    }

    private fun handleEventMainAppList(event: DragEvent): Boolean {
        val dragInfo = event.localState as? DragInfo ?: return false
        when (event.action) {
            DragEvent.ACTION_DRAG_LOCATION -> {
                binding.srl.isEnabled = false
                handleItemMovement(event.x, event.y, dragInfo)
            }
            DragEvent.ACTION_DROP -> {
                binding.srl.isEnabled = true
                stopMovePagesJob()
            }
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleItemMovement(x: Float, y: Float, dragInfo: DragInfo) {
        when {
            moveLeftRect.contains(x.toInt(), y.toInt()) -> {
                viewPagerAdapter?.clearRequests()
                if (viewModel.movePageJob == null) movePagesLeft()
                return
            }
            moveRightRect.contains(x.toInt(), y.toInt()) -> {
                viewPagerAdapter?.clearRequests()
                if (viewModel.movePageJob == null) movePagesRight(dragInfo)
                return
            }
            viewModel.movePageJob != null -> stopMovePagesJob()
        }

        val currentPage = binding.appPages.currentItem

        val currentRecycler = viewPagerAdapter?.getCurrentAppListView(currentPage)

        if (currentRecycler?.itemAnimator?.isRunning == true) return
        val currentView = currentRecycler?.findChildViewUnder(x, y)
        if (currentView === null) return

        val targetPosition = currentRecycler.getChildAdapterPosition(currentView)
        val targetAdapter = currentRecycler.adapter as BaseAdapter<DesktopCell, LauncherItemApplicationBinding>

        if (dragInfo.draggedItem === targetAdapter.getData()[targetPosition].app.get()) return
        if (targetAdapter.getData()[targetPosition].app.get() === null) {
            viewPagerAdapter?.insertItemToPosition(currentPage, targetPosition, dragInfo)
            return
        } else if (/*targetAdapter.getData().last().app.get() === null*/ dragInfo.cell?.page == PAGE_INDEX_JUST_MENU) {
//                viewPagerAdapter?.insertToLastPosition(dragInfo, currentPage, true)
            viewPagerAdapter?.insertAndMoveOtherForward(dragInfo, viewPagerAdapter?.getAdapterIndex(targetAdapter)!!, targetPosition)
            return
        }

        val holder = currentRecycler.getChildViewHolder(currentView)

//        if (dragInfo.currentPage == -1)
//            viewPagerAdapter?.insertItemToPosition(currentPage, holder.layoutPosition, dragInfo)
//        else
        viewPagerAdapter?.swapItem(dragInfo, holder.layoutPosition, currentPage)
    }

    private fun stopMovePagesJob() {
        viewModel.movePageJob?.cancel()
        viewModel.movePageJob = null
    }

    private fun movePagesRight(dragInfo: DragInfo) {
        viewModel.movePageJob = launch(Dispatchers.IO) {
            val viewPagerAdapter = viewPagerAdapter ?: return@launch
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
             viewModel.getFormattedAppPositions(visibleItemCountOnPageScreen),
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
            binding.motionView.canCallLongCLick = false
            if (touchRect.contains(event.x.toInt(), event.y.toInt())) handleMovingPages(event.x)
            true
        }
    }

    private fun handleMovingPages(touchXPosition: Float) {
        val viewPagerAdapter = viewPagerAdapter ?: return
        val percent = touchXPosition / binding.indicatorOverlayMax.width
        binding.appPages.currentItem = (viewPagerAdapter.count * percent).roundToInt()
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

    }

    private var menuProgress = 0f
    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {
        viewModel.hasAppInfoBubble.set(false)
        binding.srl.isEnabled = false

        val roundProgress = (progress * 100).toInt() / 100f

        val canSwipeViewPager = roundProgress < 0.1f

        binding.motionView.longClickTask?.cancel()

        binding.appPages.canSwipe = canSwipeViewPager


        val alphaProgress = min(progress, 0.66f)
        val newAlpha = alphaProgress / 0.66f
        binding.menu.alpha = newAlpha
        binding.bottomRoot.alpha = 1f - newAlpha
        binding.bottomRoot.translationY = if (alphaProgress < 0.66f) 0f else -100000f
        binding.appPages.alpha = 1f - newAlpha
        binding.appPages.translationY = -200 * newAlpha

        if (abs(progress - menuProgress) < 0.001f) {
            animateArrow(binding.arrow.state, 0f)
        } else if (progress < menuProgress) {
            animateArrow(binding.arrow.state, 1f)
        } else if (progress > menuProgress) {
            animateArrow(binding.arrow.state, -1f)
        } else {
            animateArrow(binding.arrow.state, 0f)
        }
        menuProgress = progress
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
        if (currentId == R.id.end) {
            viewModel.isSelectionEnabled.set(false)
            binding.srl.isEnabled = false
            setLightStatusBar(true)
            binding.menuTouchInterceptor.setOnTouchListener(null)
        }
        if (currentId == R.id.start) {
            viewModel.disableSelection = false
            binding.srl.isEnabled = true
            setLightStatusBar(false)
            binding.menuTouchInterceptor.setOnTouchListener { _, _ -> true }
            animateArrow(binding.arrow.state, -1f)
            binding.menu.alpha = 0f
        }
    }

    private var arrowAnimator: ValueAnimator? = null
    private var arrowEndValue: Float? = null
    private fun animateArrow(from: Float, to: Float) {
        if (to == arrowEndValue) return
        arrowAnimator?.cancel()
        if (to == binding.arrow.state) return
        arrowAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = 200
            addUpdateListener { binding.arrow.state = it.animatedValue as Float }
            addListener(
                onStart = { arrowEndValue = to },
                onEnd = { arrowEndValue = null },
                onCancel = { arrowEndValue = null }
            )
            start()
        }
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {

    }

    override fun isPresentOnHomeScreen() = true//binding.motionView.progress < 0.2f

    override fun onAppSelected() {
        binding.motionView.canCallLongCLick = false
    }

    override fun onLongClick(p0: View?): Boolean {
//        viewModel.isSelectionEnabled.set(!(viewModel.isSelectionEnabled.get() ?: false))
        return true
    }

    private fun setLightStatusBar(isLight: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.let { it as Activity }.window.insetsController?.setSystemBarsAppearance(
                if (isLight) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else setLightStatusBarLegacy(isLight)
    }

    @Suppress("DEPRECATION")
    private fun setLightStatusBarLegacy(isLight: Boolean) {
        context.let { it as Activity }.window.decorView.systemUiVisibility =
            if (isLight) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }

}