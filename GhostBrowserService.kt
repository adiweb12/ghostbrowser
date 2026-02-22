package com.example.ghostbrowser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

class GhostBrowserService : Service() {
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // High-speed cleanup logic
        clearAppData()
        return START_NOT_STICKY
    }

    private fun clearAppData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        // The service can now stop itself after cleaning
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
