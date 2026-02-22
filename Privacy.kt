/*package com.example.ghostbrowser

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView

object PrivacyManager {
    
    // Call this if you want to wipe everything via a button click
    fun nukeSession() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }

    // Force strict incognito mode on any WebView instance
    fun enableStrictPrivacy(webView: WebView) {
        webView.settings.apply {
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = false
            saveFormData = false
            databaseEnabled = false
        }
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
    }
}
*/
