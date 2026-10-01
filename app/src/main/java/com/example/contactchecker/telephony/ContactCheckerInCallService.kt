package com.example.contactchecker.telephony

import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class ContactCheckerInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: $call, state=${call.state}")
        ContactCheckerCallManager.registerCall(call)
        call.registerCallback(callCallback)
        updateCallState(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved: $call")
        call.unregisterCallback(callCallback)
        ContactCheckerCallManager.unregisterCall(call)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "onStateChanged: state=$state")
            updateCallState(call)
        }
    }

    private fun updateCallState(call: Call) {
        val mappedState = when (call.state) {
            Call.STATE_DIALING -> CallState.DIALING
            Call.STATE_CONNECTING -> CallState.CONNECTING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallState.DISCONNECTED
            else -> CallState.IDLE
        }
        ContactCheckerCallManager.onCallStateChanged(mappedState)
    }

    companion object {
        private const val TAG = "InCallService"
    }
}
