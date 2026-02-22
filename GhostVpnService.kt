package com.example.ghostbrowser

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.webkit.CookieManager
import android.webkit.WebStorage
import java.lang.Thread

object PrivacyManager {
    fun nukeSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}

class GhostVpnService : VpnService(), Runnable {
    private var vpnThread: Thread? = null
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (vpnThread == null) {
            vpnThread = Thread(this, "GhostVpnThread")
            vpnThread?.start()
        }
        return START_STICKY
    }

    override fun run() {
        try {
            vpnInterface = Builder()
                .setSession("GhostVPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .establish()

            while (!Thread.interrupted()) {
                Thread.sleep(2000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        vpnThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        vpnThread = null
        super.onDestroy()
    }
}
