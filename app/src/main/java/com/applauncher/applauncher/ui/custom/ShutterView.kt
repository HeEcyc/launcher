package com.applauncher.applauncher.ui.custom

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import com.applauncher.applauncher.BR
import com.applauncher.applauncher.R
import com.applauncher.applauncher.databinding.ShutterViewBinding
import com.applauncher.applauncher.services.NLService
import com.shahryar.airbar.AirBar
import java.lang.reflect.Field


class ShutterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), View.OnClickListener,
    AirBar.OnProgressChangedListener {
    private val cameraManager
        get() =
            context.getSystemService(CameraManager::class.java)

    private var mediaController: MediaController? = null

    private val bluetoothAdapter
        get() = context.getSystemService(BluetoothManager::class.java).adapter
    private val wifiManager
        get() = context.getSystemService(WifiManager::class.java)

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
        binding.allSettingsLayout.visibility = View.GONE
        settingsData.onInit(context)
    }

    private fun createComponentName() =
        ComponentName(context, NLService::class.java)


    fun onHide() {
        binding.allSettingsLayout.visibility = View.GONE
        mediaController?.unregisterCallback(mediaCallBack)
    }

    fun onShow() {
        binding.allSettingsLayout.visibility = View.VISIBLE
        setupPlayerLayout()
    }

    private fun setupPlayerLayout() {
        if (!hasNotificationPermission()) return

        val msm = context.getSystemService(MediaSessionManager::class.java)

        msm.getActiveSessions(createComponentName())
            .firstOrNull { it.metadata?.containsKey(MediaMetadata.METADATA_KEY_TITLE) == true }
            ?.let { mediaController ->
                mediaController.registerCallback(mediaCallBack)
                this.mediaController = mediaController
            }
        setSoundInfo(mediaController?.metadata)
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
        cameraManager.registerTorchCallback(settingsData, Handler(Looper.getMainLooper()))

    }

    private fun setupListener() {
        binding.playerRewind.setOnClickListener(this)
        binding.playerForward.setOnClickListener(this)
        binding.play.setOnClickListener(this)
        binding.bluetoothSettings.setOnClickListener(this)
        binding.wifiSettings.setOnClickListener(this)
        binding.airPlaneSettings.setOnClickListener(this)
        binding.mobileNetworkSettings.setOnClickListener(this)
        binding.brightnessProgressBar.setOnProgressChangedListener(this)
        binding.volumeProgressBar.setOnProgressChangedListener(this)
        binding.flashLightButton.setOnClickListener(this)
        binding.browserButton.setOnClickListener(this)
        binding.alarmButton.setOnClickListener(this)
        binding.cameraButton.setOnClickListener(this)
        binding.autoRotateButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.playerRewind -> sendCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            R.id.playerForward -> sendCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
            R.id.play -> sendCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            R.id.bluetoothSettings -> onBluetoothClick()
            R.id.mobileNetworkSettings, R.id.wifiSettings -> onNetworkClick()
            R.id.airPlaneSettings -> onAirplaneSettingsClick()
            R.id.flashLightButton -> onFlashLightClick()
            R.id.alarmButton -> openDefaultAlarm()
            R.id.browserButton -> openBrowser()
            R.id.cameraButton -> openCamera()
            R.id.autoRotateButton -> onAutoRotateClick()
        }
    }

    private fun onAutoRotateClick() {
        if (Settings.System.canWrite(context)) {
            val isEnabledAutoRotate = settingsData.isEnableAutoRotate.get() ?: false
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (isEnabledAutoRotate) 1 else 0
            )
        } else askWriteSettingsPermission()
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .let(::openDefaultApp)
    }

    private fun openDefaultAlarm() {
        Intent(AlarmClock.ACTION_SET_ALARM)
            .let(::openDefaultApp)
    }

    private fun openBrowser() {
        Intent("android.intent.action.VIEW", Uri.parse("http://"))
            .let(::openDefaultApp)
    }

    private fun openDefaultApp(intent: Intent) {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.let(context.packageManager::getLaunchIntentForPackage)
            ?.let(context::startActivity)
    }

    private fun onFlashLightClick() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            return
        val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return
        kotlin.runCatching {
            cameraManager.setTorchMode(cameraId, !(settingsData.isTorch.get() ?: false))
        }
    }

    private fun onAirplaneSettingsClick() {
        Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            .let(context::startActivity)
    }

    private fun onNetworkClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .let(context::startActivity)
        else wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
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
        cameraManager.unregisterTorchCallback(settingsData)
    }

    inner class SettingsData : CameraManager.TorchCallback() {
        val isEnableBluetooth = ObservableField<Boolean>()
        val isEnableAirplaneMode = ObservableField<Boolean>()
        val isEnableMobileNetwork = ObservableField<Boolean>()
        val isEnableWifi = ObservableField<Boolean>()
        val isTorch = ObservableField(false)
        val isEnableAutoRotate = ObservableField(false)
        val audioVolumePercent = ObservableField<Double>()
        val brightnessPercent = ObservableField<Double>()

        private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                readCurrentBrightnessValue(context)
            }
        }
        private val autoRotateObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                readIsAutoRotateValue(context)
            }
        }

        private fun readIsAutoRotateValue(context: Context) {
            val isEnableRotate = Settings.System
                .getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION) == 0
            isEnableAutoRotate.set(isEnableRotate)
        }

        private val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        var maxVolumeValue = -1
        var maxBrightnessValue = -1


        private val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }

        private val settingsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_AIRPLANE_MODE_CHANGED -> isEnableAirplaneMode
                        .set(isAirplaneModeOn(context))
                    BluetoothAdapter.ACTION_STATE_CHANGED -> isEnableBluetooth
                        .set(isBluetoothEnable(intent))
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> isEnableWifi
                        .set(isWifiEnable(context))
                    ConnectivityManager.CONNECTIVITY_ACTION ->
                        isEnableMobileNetwork.set(isEnableMobileData())
                    "android.media.VOLUME_CHANGED_ACTION" -> handleVolumeState(intent)
                }
            }
        }

        private fun handleVolumeState(intent: Intent) {
            if (intent.extras?.getInt(EXTRA_VOLUME_STREAM_TYPE) != AudioManager.STREAM_MUSIC) return
            val currentVolume = intent.extras?.getInt(EXTRA_VOLUME_STREAM_VALUE) ?: 0
            audioVolumePercent.set(currentVolume / maxVolumeValue.toDouble())
        }

        private fun isEnableMobileData(): Boolean {
            return TrafficStats.getMobileRxBytes() > 0
        }

        fun onInit(context: Context) {
            registerListeners(context)
            readCurrentState(context)
        }

        private fun readCurrentState(context: Context) {
            context.getSystemService(BluetoothManager::class.java)
                .adapter?.isEnabled?.let(isEnableBluetooth::set)
            isEnableAirplaneMode.set(isAirplaneModeOn(context))
            isEnableWifi.set(isWifiEnable(context))
            isEnableMobileNetwork.set(isEnableMobileData())
            readMaxVolumeValue(context)
            readCurrentVolumeValue(context)
            readMaxBrightnessValue(context)
            readCurrentBrightnessValue(context)
            readIsAutoRotateValue(context)
        }

        private fun readCurrentBrightnessValue(context: Context) {
            val currentBrightness = Settings.System
                .getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightnessPercent.set(currentBrightness / maxBrightnessValue.toDouble())
        }

        private fun readMaxBrightnessValue(context: Context) {
            maxBrightnessValue = getMaxBrightness(context)
        }

        private fun readCurrentVolumeValue(context: Context) {
            val currentVolume = context.getSystemService(AudioManager::class.java)
                .getStreamVolume(AudioManager.STREAM_MUSIC)
            audioVolumePercent.set(currentVolume / maxVolumeValue.toDouble())
        }

        private fun readMaxVolumeValue(context: Context) {
            maxVolumeValue = context.getSystemService(AudioManager::class.java)
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

        private fun registerListeners(context: Context) {
            context.registerReceiver(settingsReceiver, intentFilter)
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                brightnessObserver
            )
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                autoRotateObserver
            )
        }

        fun onDestroy(context: Context) {
            context.unregisterReceiver(settingsReceiver)
            context.contentResolver.unregisterContentObserver(brightnessObserver)
            context.contentResolver.unregisterContentObserver(autoRotateObserver)
        }

        private fun isAirplaneModeOn(context: Context): Boolean {
            return Settings.System
                .getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        }

        private fun isBluetoothEnable(intent: Intent) = intent
            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) ==
                BluetoothAdapter.STATE_ON

        private fun isWifiEnable(context: Context) =
            context.getSystemService(WifiManager::class.java).isWifiEnabled

        private fun getMaxBrightness(context: Context): Int {
            val powerManager = context.getSystemService(PowerManager::class.java)
            val fields: Array<Field> = powerManager.javaClass.declaredFields
            for (field in fields) {
                if (field.name.equals("BRIGHTNESS_ON")) {
                    field.isAccessible = true
                    return try {
                        field.get(powerManager) as Int
                    } catch (e: IllegalAccessException) {
                        255
                    }
                }
            }
            return 255
        }

        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            isTorch.set(enabled)
        }
    }

    override fun onProgressChanged(airBar: AirBar, progress: Double, percentage: Double) {
        when (airBar.id) {
            R.id.brightnessProgressBar -> changeBrightness(percentage)
            R.id.volumeProgressBar -> changeVolume(percentage)
        }
    }

    private fun changeVolume(percentage: Double) {
        val currentVolume = (percentage * settingsData.maxVolumeValue).toInt()
        context.getSystemService(AudioManager::class.java)
            .setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND)
    }

    private fun changeBrightness(percentage: Double) {
        Settings.System.putInt(
            context.contentResolver, Settings.System
                .SCREEN_BRIGHTNESS, (percentage * settingsData.maxBrightnessValue).toInt()
        )
    }

    override fun afterProgressChanged(airBar: AirBar, progress: Double, percentage: Double) {

    }

    override fun canChange(airBar: AirBar): Boolean {
        return when (airBar.id) {
            R.id.brightnessProgressBar -> Settings.System.canWrite(context)
            else -> true
        }
    }

    override fun actionWhenCantChange(airBar: AirBar) {
        askWriteSettingsPermission()
    }

    override fun canMoveByFinger(airBar: AirBar): Boolean {
        return airBar.id == R.id.brightnessProgressBar
    }

    private fun askWriteSettingsPermission() {
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .let(context::startActivity)
    }
}