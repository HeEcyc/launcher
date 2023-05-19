package com.accent.launcher.base

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.accent.launcher.BR


abstract class BaseActivity<TViewModel : BaseViewModel, TBinding : ViewDataBinding>(private val layoutId: Int) :
    AppCompatActivity() {

    lateinit var binding: TBinding
    abstract val viewModel: TViewModel

    private var onPermission: ((Boolean) -> Unit)? = null
    private var onImagesSelected: ((Array<Uri>) -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this
        setupUI()
    }

    private fun setStatusBarColor() {
        with(window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) setDecorFitsSystemWindows(false)
            else setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }

    abstract fun setupUI()

    fun askPermission(
        permissions: Array<String>,
        onPermission: ((isGrand: Boolean) -> Unit)? = null
    ) {
        if (hasPermissions(permissions)) {
            onPermission?.invoke(true)
            return
        }
        this.onPermission = onPermission
        ActivityCompat.requestPermissions(this, permissions, 200)
    }

    private fun hasPermissions(permissions: Array<String>) = !permissions
        .map { ActivityCompat.checkSelfPermission(this, it) }
        .any { it == PackageManager.PERMISSION_DENIED }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onPermission?.let { it(!grantResults.contains(PackageManager.PERMISSION_DENIED)) }
        onPermission = null
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }
}

