package com.accent.launcher.ui.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.accent.launcher.R
import com.accent.launcher.data.Prefs
import com.accent.launcher.databinding.DialogTutorialBinding

class DialogTutorial : DialogFragment() {

    protected lateinit var binding: DialogTutorialBinding
    private var isShowNext = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_tutorial, container, false)!!
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireActivity(), R.style.WideDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun setupUI() {
        binding.nextButton.setOnClickListener {
            if (isShowNext) {
                dismiss()
            } else {
                isShowNext = true
                showNextView()
            }
        }
    }

    private fun showNextView() {
        binding.swipeTopDown.visibility = View.GONE
        binding.swipeTopText.visibility = View.GONE
        binding.swipeLeft.visibility = View.VISIBLE
        binding.swipeLeftText.visibility = View.VISIBLE
    }

    override fun onDetach() {
        Prefs.isShowingTutorial = true
        super.onDetach()
    }
}