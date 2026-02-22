package com.example.ghostbrowser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.CookieManager
import android.webkit.WebStorage

class GhostBrowserService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // High Privacy Purge
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies {
            cookieManager.flush()
            WebStorage.getInstance().deleteAllData()
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
