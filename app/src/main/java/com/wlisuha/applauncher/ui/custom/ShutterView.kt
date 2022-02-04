package com.wlisuha.applauncher.ui.custom

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.wlisuha.applauncher.BR
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
    private val settingsData = SettingsData()

    private val mediaCallBack = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            setSoundInfo(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            setPlayStateIcon()
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaController?.unregisterCallback(this)
            mediaController = null
            setSoundInfo(null)
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
        binding.setVariable(BR.settingsData, settingsData)
        settingsData.onInit(context)
    }

    private fun createComponentName() =
        ComponentName(context, NLService::class.java)


    fun onHide() {
        mediaController?.unregisterCallback(mediaCallBack)
    }

    fun onShow() {
        setupPlayerLayout()
    }

    private fun setupPlayerLayout() {
        if (!hasNotificationPermission()) return

        val msm = context.getSystemService(MediaSessionManager::class.java)

        msm.getActiveSessions(createComponentName())
            .firstOrNull { it.metadata?.containsKey(MediaMetadata.METADATA_KEY_TITLE) == true }
            ?.let { mediaController ->
                setSoundInfo(mediaController.metadata)
                mediaController.registerCallback(mediaCallBack)
                this.mediaController = mediaController
            }
        setPlayStateIcon()
    }

    private fun hasNotification(packageName: String) = context
        .getSystemService(NotificationManager::class.java)
        .activeNotifications.any {
            Log.d("12345", it.packageName)
            it.packageName == packageName
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
        settingsData.onDestroy(context)
    }


    class SettingsData {
        val isEnableBluetooth = ObservableField<Boolean>()
        val isEnableAirplaneMode = ObservableField<Boolean>()
        val isEnableMobileNetwork = ObservableField<Boolean>()
        val isEnableWifi = ObservableField<Boolean>()
        private val airPlaneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isEnableAirplaneMode.set(isAirplaneModeOn(context))
            }
        }

        private val wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isEnableWifi.set(isWifiEnable(context))
            }
        }
        private val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                isEnableBluetooth.set(isBluetoothEnable(intent))
            }
        }

        private fun isBluetoothEnable(intent: Intent) = intent
            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                BluetoothAdapter.STATE_ON

        private fun isWifiEnable(context: Context) =
            context.getSystemService(WifiManager::class.java).isWifiEnabled

        fun onInit(context: Context) {
            registerListeners(context)
            readCurrentState(context)
        }

        private fun readCurrentState(context: Context) {
            context.getSystemService(BluetoothManager::class.java)
                .adapter.isEnabled.let(isEnableBluetooth::set)
            isEnableAirplaneMode.set(isAirplaneModeOn(context))
            isEnableWifi.set(isWifiEnable(context))
        }

        private fun registerListeners(context: Context) {
            context.registerReceiver(
                bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
            context.registerReceiver(
                airPlaneReceiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            )

            context.registerReceiver(
                wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
            )
        }

        fun onDestroy(context: Context) {
            context.unregisterReceiver(bluetoothReceiver)
            context.unregisterReceiver(airPlaneReceiver)
            context.unregisterReceiver(wifiReceiver)
        }

        private fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.System
                .getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        }

    }
}