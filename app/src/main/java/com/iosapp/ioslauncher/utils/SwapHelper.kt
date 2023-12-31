package com.iosapp.ioslauncher.utils

import android.os.Handler


class SwapHelper(private val swapHandler: Handler) {
    private var swapFromPosition = -1
    private var swapToPosition = -1
    private var swapFromPage = -1
    private var swapToPage = -1
    private var requestType: RequestType? = null


    fun requestToSwapInSomePage(swapFromPosition: Int, swapToPosition: Int, action: () -> Unit) {
        if (requestType != RequestType.SWAP_IN_SOME_PAGE) {
            clearRequest()
            requestType = RequestType.SWAP_IN_SOME_PAGE
        }
        if (swapFromPosition == swapToPosition) {
            removeRequest()
            return
        }
        if (this.swapFromPosition == swapFromPosition && this.swapToPosition == swapToPosition) {
            return
        }
        this.swapFromPosition = swapFromPosition
        this.swapToPosition = swapToPosition
        removeRequest()
        swapHandler.postDelayed({
            this.swapFromPosition = -1
            this.swapToPosition = -1
            action.invoke()
            clearRequest()
        }, REQUEST_DELAY)
    }

    private fun removeRequest() {
        swapHandler.removeCallbacksAndMessages(null)
    }

    fun clearRequest() {
        swapFromPosition = -1
        swapToPosition = -1
        swapFromPage = -1
        swapToPage = -1
        removeRequest()
        requestType = null
    }

    fun requestToSwapBetweenPages(
        swapToPosition: Int,
        swapFromPosition: Int,
        swapFromPage: Int,
        swapToPage: Int,
        action: () -> Unit
    ) {

        if (requestType != RequestType.SWAP_BETWEEN_PAGE) {
            clearRequest()
            requestType = RequestType.SWAP_BETWEEN_PAGE
        }

        if (!canCreateRequest(swapToPage, swapFromPosition, swapToPosition, swapFromPage)) {
            return
        }

        this.swapToPage = swapToPage
        this.swapFromPage = swapFromPage
        this.swapToPosition = swapToPosition
        this.swapFromPosition = swapFromPosition

        removeRequest()

        swapHandler.postDelayed({
            this.swapFromPosition = -1
            this.swapFromPage = -1
            this.swapToPosition = -1
            this.swapToPage = -1
            action.invoke()
            clearRequest()
        }, REQUEST_DELAY)
    }

    fun requestInsertToLastPosition(swapToPage: Int, action: () -> Unit) {
        if (requestType != RequestType.INSERT_TO_LAST_POSITION) {
            clearRequest()
            requestType = RequestType.INSERT_TO_LAST_POSITION
        }

        if (this.swapToPage == swapToPage) return

        this.swapToPage = swapToPage

        removeRequest()

        swapHandler.postDelayed({
            this.swapToPage = -1
            action.invoke()
        }, REQUEST_DELAY)
    }

    private fun canCreateRequest(
        swapToPage: Int,
        swapFromPosition: Int,
        swapToPosition: Int,
        swapFromPage: Int
    ) = this.swapToPage != swapToPage ||
            this.swapFromPage != swapFromPage ||
            this.swapFromPosition != swapFromPosition ||
            this.swapToPosition != swapToPosition

    fun requestInsertToPosition(swapToPage: Int, swapToPosition: Int, action: () -> Unit) {
        if (requestType != RequestType.INSERT_TO_POSITION) {
            clearRequest()
            requestType = RequestType.INSERT_TO_POSITION
        }

        if (this.swapToPage == swapToPage && this.swapToPosition == swapToPosition) return

        removeRequest()

        this.swapToPage = swapToPage
        this.swapToPosition = swapToPosition
        swapHandler.postDelayed({
            this.swapToPage = -1
            this.swapToPosition = -1
            action.invoke()
        }, REQUEST_DELAY)
    }


    enum class RequestType {
        INSERT_TO_POSITION,
        INSERT_TO_LAST_POSITION,

        SWAP_BETWEEN_PAGE,
        SWAP_IN_SOME_PAGE
    }
}