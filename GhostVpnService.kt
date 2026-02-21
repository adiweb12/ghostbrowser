package com.ghost.browser

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class GhostVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        establishVpn()
        return START_STICKY
    }

    private fun establishVpn() {
        try {
            vpnInterface = Builder()
                .setSession("GhostTunnel")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)
                .addAllowedApplication("com.ghost.browser")
                .establish()

            // START PACKET HANDLER
            vpnThread = thread(start = true, name = "GhostPacketHandler") {
                runPacketLoop()
            }
        } catch (e: Exception) {
            Log.e("GhostVPN", "Setup Error: ${e.localizedMessage}")
        }
    }

    private fun runPacketLoop() {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val output = FileOutputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteBuffer.allocate(32768)

        try {
            while (!Thread.interrupted()) {
                val length = input.read(buffer.array())
                if (length > 0) {
                    // In a production app, you'd forward these packets 
                    // to a remote server here via a Socket.
                    Log.d("GhostVPN", "Intercepted packet of size: $length")
                }
                Thread.sleep(10) // Prevents 100% CPU usage
            }
        } catch (e: Exception) {
            Log.e("GhostVPN", "Loop Error: ${e.localizedMessage}")
        }
    }

    private fun stopVpn() {
        vpnThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }
}
