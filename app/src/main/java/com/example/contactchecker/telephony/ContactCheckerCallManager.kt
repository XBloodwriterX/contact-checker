package com.example.contactchecker.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContactCheckerCallManager {

    private val _currentCallState = MutableStateFlow(CallState.IDLE)
    val currentCallState: StateFlow<CallState> = _currentCallState.asStateFlow()

    @Volatile
    private var activeCall: Call? = null

    private var telephonyCallback: Any? = null
    var isSimulationMode: Boolean = false

    fun registerCall(call: Call) {
        activeCall = call
    }

    fun unregisterCall(call: Call) {
        if (activeCall == call) {
            activeCall = null
        }
        _currentCallState.value = CallState.DISCONNECTED
    }

    fun onCallStateChanged(state: CallState) {
        _currentCallState.value = state
    }

    @Suppress("MissingPermission")
    fun placeCall(context: Context, phoneNumber: String): Boolean {
        if (isSimulationMode) {
            _currentCallState.value = CallState.DIALING
            return true
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        return try {
            val formattedUri = "tel:${Uri.encode(phoneNumber)}".toUri()
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            if (telecomManager != null) {
                telecomManager.placeCall(formattedUri, null)
            } else {
                val intent = Intent(Intent.ACTION_CALL, formattedUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            _currentCallState.value = CallState.DIALING
            true
        } catch (_: Exception) {
            _currentCallState.value = CallState.IDLE
            false
        }
    }

    @Suppress("MissingPermission", "DEPRECATION")
    fun disconnectCurrentCall(context: Context? = null) {
        if (isSimulationMode) {
            _currentCallState.value = CallState.DISCONNECTED
            return
        }

        activeCall?.let { call ->
            try {
                call.disconnect()
            } catch (_: Exception) {
                // Ignore exception if call already ended
            }
            activeCall = null
        }

        if (context != null && (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED)) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            try {
                telecomManager?.endCall()
            } catch (_: Exception) {
                // Ignored
            }
        }

        _currentCallState.value = CallState.DISCONNECTED
    }

    fun registerTelephonyListener(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleTelephonyState(state)
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            } catch (_: SecurityException) {
                // Missing permission
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleTelephonyState(state)
                }
            }
            telephonyCallback = listener
            try {
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            } catch (_: SecurityException) {
                // Missing permission
            }
        }
    }

    fun unregisterTelephonyListener(context: Context) {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyCallback?.let { cb ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && cb is TelephonyCallback) {
                telephonyManager.unregisterTelephonyCallback(cb)
            } else if (cb is PhoneStateListener) {
                @Suppress("DEPRECATION")
                telephonyManager.listen(cb, PhoneStateListener.LISTEN_NONE)
            }
        }
        telephonyCallback = null
    }

    private fun handleTelephonyState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                _currentCallState.value = CallState.RINGING
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (_currentCallState.value == CallState.DIALING || _currentCallState.value == CallState.CONNECTING) {
                    _currentCallState.value = CallState.RINGING
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (_currentCallState.value != CallState.IDLE) {
                    _currentCallState.value = CallState.DISCONNECTED
                }
            }
        }
    }
}
