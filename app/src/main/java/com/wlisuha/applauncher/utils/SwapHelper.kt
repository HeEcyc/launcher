package com.wlisuha.applauncher.utils

import android.os.Handler


class SwapHelper(private val swapHandler: Handler) {
    private var swapFromPosition = -1
    private var swapToPosition = -1

    fun requestToSwap(swapFromPosition: Int, swapToPosition: Int, action: () -> Unit) {
        if (swapFromPosition == swapToPosition) {
            swapHandler.removeCallbacksAndMessages(null)
            return
        }
        if (this.swapFromPosition == swapFromPosition && this.swapToPosition == swapToPosition) {
            return
        }
        this.swapFromPosition = swapFromPosition
        this.swapToPosition = swapToPosition
        removeRequestToSwap()
        swapHandler.postDelayed({ action.invoke() }, 500)
    }

    fun removeRequestToSwap() {
        swapHandler.removeCallbacksAndMessages(null)
    }


}