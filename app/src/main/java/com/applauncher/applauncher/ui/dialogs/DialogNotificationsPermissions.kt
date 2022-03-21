package com.applauncher.applauncher.ui.dialogs

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.applauncher.applauncher.R
import com.applauncher.applauncher.databinding.DialogNotificationsPermissionBinding

class DialogNotificationsPermissions : DialogFragment() {
    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            dismiss()
        }
    private lateinit var binding: DialogNotificationsPermissionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_notifications_permission,
            container,
            false
        )!!
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
        binding.dismissButton.setOnClickListener {
            dismiss()
        }

        binding.allowButton.setOnClickListener {
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .let(notificationLauncher::launch)
        }
    }

}