package com.example.ghostbrowser

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.webkit.CookieManager
import android.webkit.WebStorage

class GhostBrowserService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PrivacyManager.nukeSession()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

object PrivacyManager {
    fun nukeSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}
