package com.R.movsenseapplication

import android.bluetooth.le.ScanResult

//import com.polidea.rxandroidble2.scan.ScanResult


data class MyScanResult(var macAddress: ScanResult, var connectedSerial: String?, var isConnected: Boolean = false) {
    fun markDisconnected() {
        isConnected = false
    }
    fun markConnected(serial: String) {
        connectedSerial = serial
        isConnected = true
    }
}

