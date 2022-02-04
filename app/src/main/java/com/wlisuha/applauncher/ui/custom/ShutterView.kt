package com.wlisuha.applauncher.ui.custom

import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import com.wlisuha.applauncher.R
import com.wlisuha.applauncher.databinding.ShutterViewBinding
import com.wlisuha.applauncher.services.NLService


class ShutterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), View.OnClickListener {
    private var mediaController: MediaController? = null
    private val bluetoothAdapter
        get() = context.getSystemService(BluetoothManager::class.java).adapter


    private val mediaCallBack = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            setSoundInfo(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            setPlayStateIcon()
        }
    }
    val binding: ShutterViewBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.shutter_view,
        this,
        true
    )

    init {

    }

    private fun createComponentName() =
        ComponentName(context, NLService::class.java)


    fun onHide() {
        mediaController?.unregisterCallback(mediaCallBack)
    }

    fun onShow() {
        setupPlayerLayout()
        setupMainSettingsLayout()
    }

    private fun setupMainSettingsLayout() {

    }

    private fun setupPlayerLayout() {
        if (!hasNotificationPermission()) return
        val msm = context.getSystemService(MediaSessionManager::class.java)

        msm.getActiveSessions(createComponentName()).forEach { mediaController ->
            setSoundInfo(mediaController.metadata)
            mediaController.registerCallback(mediaCallBack)
            this.mediaController = mediaController
        }
        setPlayStateIcon()
    }

    private fun setSoundInfo(metadata: MediaMetadata?) {
        binding.artistName.text = metadata
            ?.getText(MediaMetadata.METADATA_KEY_ARTIST)
        binding.soundName.text = metadata
            ?.getText(MediaMetadata.METADATA_KEY_TITLE)
    }

    fun hasNotificationPermission() = NotificationManagerCompat
        .getEnabledListenerPackages(context).contains(context.packageName)


    private fun sendCommand(keyCode: Int) {
        mediaController?.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        mediaController?.dispatchMediaButtonEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    init {
        setupListener()
    }

    private fun setupListener() {
        binding.playerRewind.setOnClickListener(this)
        binding.playerForward.setOnClickListener(this)
        binding.play.setOnClickListener(this)
        binding.bluetoothSettings.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.playerRewind -> sendCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            R.id.playerForward -> sendCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
            R.id.play -> sendCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            R.id.bluetoothSettings -> onBluetoothClick()
        }
    }

    private fun onBluetoothClick() {
        if (bluetoothAdapter.isEnabled) bluetoothAdapter.disable()
        else bluetoothAdapter.enable()
    }

    private fun setPlayStateIcon() {
        when (mediaController?.playbackState?.state) {
            PlaybackState.STATE_PLAYING -> R.drawable.ic_player_pause
            else -> R.drawable.ic_player_play
        }.let(binding.play::setImageResource)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mediaController?.unregisterCallback(mediaCallBack)
    }


//
//    interface OnSettingsClick {
//        fun onBluetoothClick()
//    }
}