package com.overklassniy.q25.dialer.service

import android.telecom.Call
import android.telecom.CallScreeningService

class DialerCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}