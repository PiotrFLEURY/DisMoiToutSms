package fr.piotr.dismoitoutsms.headset

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Handler

import java.util.concurrent.atomic.AtomicBoolean

import fr.piotr.dismoitoutsms.service.DisMoiToutSmsService
import fr.piotr.dismoitoutsms.util.ConfigurationManager

/**
 * Created by piotr on 08/08/2017.
 *
 */

class BluetoothReceiver(disMoiToutSmsService: DisMoiToutSmsService) : AbstractHeadSetReceiver(disMoiToutSmsService) {

    private val autoStarted = AtomicBoolean()
    val handler = Handler()

    override fun onReceive(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        if (device != null && !ConfigurationManager.isBluetoothBanned(context, device.address)) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> postDelayCheck(context)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> checkBluetoothHeadSetState(context)
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    onStateChanged(state)
                }
            }
        }


    }

    private fun postDelayCheck(context: Context) {
        handler.postDelayed({ checkBluetoothHeadSetState(context) }, 3000)
    }

    private fun onStateChanged(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {}
            BluetoothAdapter.STATE_TURNING_OFF -> {}
            BluetoothAdapter.STATE_ON -> {}
            BluetoothAdapter.STATE_TURNING_ON -> {}
        }
    }

    private fun checkBluetoothHeadSetState(context: Context) {
        // Get the default adapter
        checkHeadsetProfile(context, BluetoothAdapter.getDefaultAdapter())
    }

    private fun checkHeadsetProfile(context: Context, mBluetoothAdapter: BluetoothAdapter) {
        val mHeadsetProfileListener = object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    val mBluetoothHeadset = proxy as BluetoothHeadset
                    val connected = !mBluetoothHeadset.connectedDevices.isEmpty()
                    onAudioEvent(context, connected)
                    if (!connected) {
                        checkAD2PProfile(context, mBluetoothAdapter)
                    }
                }
                // Close proxy connection after use.
                mBluetoothAdapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {
                //
            }
        }

        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(context, mHeadsetProfileListener, BluetoothProfile.HEADSET)
    }

    private fun checkAD2PProfile(context: Context, mBluetoothAdapter: BluetoothAdapter) {
        val mA2DPProfileListener = object : BluetoothProfile.ServiceListener {

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    val mBluetoothA2dp = proxy as BluetoothA2dp
                    onAudioEvent(context, !mBluetoothA2dp.connectedDevices.isEmpty())
                }

                // Close proxy connection after use.
                mBluetoothAdapter.closeProfileProxy(profile, proxy)
            }

            override fun onServiceDisconnected(profile: Int) {
                //
            }
        }

        mBluetoothAdapter.getProfileProxy(context, mA2DPProfileListener, BluetoothProfile.A2DP)
    }

    private fun onAudioEvent(context: Context, connected: Boolean) {
        if (connected) {
            onHeadSetPluggedIn(context)
        } else {
            onHeadSetPluggedOut(context)
        }
    }

    override fun notifyActivationPurpose(context: Context) {
        //DO NOT PURPOSE ACTIVATION EACH TIME A BLUETOOTH HEADSET IS PLUGGED IN TO NOT FLOOD THE USER
    }

    override fun onAutoStart() {
        autoStarted.set(true)
    }

    override fun onAutoStop(): Boolean {
        if (autoStarted.get()) {
            autoStarted.set(false)
            return true
        }
        return false
    }

    override fun isHeadsetModeActivated(context: Context): Boolean {
        return ConfigurationManager.getBoolean(context, ConfigurationManager.Configuration.BLUETOOTH_HEADSET_MODE)
    }
}
