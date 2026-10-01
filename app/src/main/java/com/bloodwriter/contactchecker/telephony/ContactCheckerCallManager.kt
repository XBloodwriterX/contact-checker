package com.bloodwriter.contactchecker.telephony

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
import androidx.annotation.RequiresApi
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

    private fun getTelephonyManager(context: Context): TelephonyManager? {
        return try {
            context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        } catch (_: Throwable) {
            null
        }
    }

    private fun getTelecomManager(context: Context): TelecomManager? {
        return try {
            context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        } catch (_: Throwable) {
            null
        }
    }

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
            val telecomManager = getTelecomManager(context)

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
        } catch (_: Throwable) {
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

        try {
            activeCall?.let { call ->
                try {
                    call.disconnect()
                } catch (_: Throwable) {
                    // Ignore exception if call already ended
                }
                activeCall = null
            }

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) && context != null && (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED)) {
                val telecomManager = getTelecomManager(context)
                try {
                    telecomManager?.endCall()
                } catch (_: Throwable) {
                    // Ignored
                }
            }
        } catch (_: Throwable) {
            // Ignore system service errors
        } finally {
            _currentCallState.value = CallState.DISCONNECTED
        }
    }

    fun registerTelephonyListener(context: Context) {
        try {
            val telephonyManager = getTelephonyManager(context) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback = Api31TelephonyHelper.register(context, telephonyManager) { state ->
                    handleTelephonyState(state)
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
                } catch (_: Throwable) {
                    // Missing permission or service error
                }
            }
        } catch (_: Throwable) {
            telephonyCallback = null
        }
    }

    fun unregisterTelephonyListener(context: Context) {
        try {
            val telephonyManager = getTelephonyManager(context) ?: return
            telephonyCallback?.let { cb ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31TelephonyHelper.unregister(telephonyManager, cb)
                } else if (cb is PhoneStateListener) {
                    try {
                        @Suppress("DEPRECATION")
                        telephonyManager.listen(cb, PhoneStateListener.LISTEN_NONE)
                    } catch (_: Throwable) {
                        // Safe ignore
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore error
        } finally {
            telephonyCallback = null
        }
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

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31TelephonyHelper {
        fun register(
            context: Context,
            telephonyManager: TelephonyManager,
            onStateChanged: (Int) -> Unit
        ): Any? {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    onStateChanged(state)
                }
            }
            return try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                callback
            } catch (_: Throwable) {
                null
            }
        }

        fun unregister(telephonyManager: TelephonyManager, callback: Any) {
            if (callback is TelephonyCallback) {
                try {
                    telephonyManager.unregisterTelephonyCallback(callback)
                } catch (_: Throwable) {
                    // Safe ignore
                }
            }
        }
    }
}
