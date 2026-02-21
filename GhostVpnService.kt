package com.ghost.browser

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class GhostVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start the VPN logic
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        try {
            val builder = Builder()

            vpnInterface = builder
                .setSession("GhostTunnel")
                // Internal virtual IP address
                .addAddress("10.0.0.2", 24) 
                // FORCE PRIVACY DNS: Prevents ISP from tracking your searches
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                // ROUTE EVERYTHING: Sends all browser traffic through the tunnel
                .addRoute("0.0.0.0", 0) 
                // MTU: Set to 1500 for maximum speed/compatibility
                .setMtu(1500)
                // App-Specific: Only route this browser, not the whole phone
                .addAllowedApplication("com.ghost.browser") 
                .establish()

            Log.d("GhostVPN", "VPN Tunnel Established successfully")
        } catch (e: Exception) {
            Log.e("GhostVPN", "Failed to start VPN: ${e.localizedMessage}")
        }
    }

    override fun onRevoke() {
        // Called if the user turns off the VPN from Android Settings
        stopVpn()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        Log.d("GhostVPN", "VPN Tunnel Closed")
    }
}
