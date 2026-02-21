package com.ghost.browser

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class GhostVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
        vpnInterface = builder
            .setSession("GhostTunnel")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("1.1.1.1") // Privacy DNS
            .addRoute("0.0.0.0", 0)  // Force all traffic through here
            .establish()
        
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        super.onDestroy()
    }
}
