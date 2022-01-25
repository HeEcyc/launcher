package com.wlisuha.applauncher.utils

import android.os.Handler


class SwapHelper(private val swapHandler: Handler) {
    private var swapFromPosition = -1
    private var swapToPosition = -1

    private var swapFromPage = -1
    private var swapToPage = -1


    fun requestToSwapInSomePage(swapFromPosition: Int, swapToPosition: Int, action: () -> Unit) {
        if (swapFromPosition == swapToPosition) {
            removeRequestToSwap()
            return
        }
        if (this.swapFromPosition == swapFromPosition && this.swapToPosition == swapToPosition) {
            return
        }
        this.swapFromPosition = swapFromPosition
        this.swapToPosition = swapToPosition
        removeRequestToSwap()
        swapHandler.postDelayed({
            this.swapFromPosition = -1
            this.swapToPosition = -1
            action.invoke()
            clearRequest()
        }, 500)
    }

    private fun removeRequestToSwap() {
        swapHandler.removeCallbacksAndMessages(null)
    }

    fun clearRequest() {
        swapFromPosition = -1
        swapToPosition = -1
        swapFromPage = -1
        swapToPage = -1
        removeRequestToSwap()
    }

    fun requestToSwapBetweenPages(
        swapToPosition: Int,
        swapFromPosition: Int,
        swapFromPage: Int,
        swapToPage: Int,
        action: () -> Unit
    ) {
        if (!canCreateRequest(swapToPage, swapFromPosition, swapToPosition, swapFromPage)) {
            return
        }

        this.swapToPage = swapToPage
        this.swapFromPage = swapFromPage
        this.swapToPosition = swapToPosition
        this.swapFromPosition = swapFromPosition

        removeRequestToSwap()

        swapHandler.postDelayed({
            this.swapFromPosition = -1
            this.swapToPosition = -1
            this.swapFromPage = -1
            this.swapToPage = -1
            action.invoke()
            clearRequest()
        }, 500)
    }

    private fun canCreateRequest(
        swapToPage: Int,
        swapFromPosition: Int,
        swapToPosition: Int,
        swapFromPage: Int
    ) = this.swapToPage != swapToPage &&
            this.swapFromPage != swapFromPage &&
            this.swapFromPosition != swapFromPosition &&
            this.swapToPosition != swapToPosition


}