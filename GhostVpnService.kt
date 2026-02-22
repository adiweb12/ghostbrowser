package com.example.ghostbrowser

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.content.Intent

class GhostVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
        builder.setSession("GhostVPN")
            .addAddress("10.0.0.1", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}
