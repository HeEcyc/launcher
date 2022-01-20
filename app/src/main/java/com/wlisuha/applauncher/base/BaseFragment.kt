package com.wlisuha.applauncher.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wlisuha.applauncher.BR
import com.wlisuha.applauncher.ui.AppViewModel

abstract class BaseFragment<B : ViewDataBinding>(@LayoutRes open val layoutId: Int) :
    Fragment() {

    protected val viewModel: AppViewModel by activityViewModels()
    protected lateinit var binding: B

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this
        binding.root.isClickable = true
        binding.root.isFocusableInTouchMode = true
        binding.root.setBackgroundColor(Color.WHITE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }


    abstract fun setupUI()

}
