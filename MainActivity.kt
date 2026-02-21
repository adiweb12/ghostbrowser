package com.ghost.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val vpnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val intent = Intent(this, GhostVpnService::class.java)
            startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 16+ support for drawing under system bars
        enableEdgeToEdge()
        
        // GHOST MODE: Wipe RAM data
        clearGhostData()

        setContent {
            // Surface ensures the background isn't just white/black
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                GhostBrowserApp(onVpnToggle = { prepareVpn() })
            }
        }
    }

    private fun clearGhostData() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnRequest.launch(intent)
        else startService(Intent(this, GhostVpnService::class.java))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhostBrowserApp(onVpnToggle: () -> Unit) {
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var loadUrl by remember { mutableStateOf("https://www.google.com") }
    val context = LocalContext.current
    
    val webView = remember {
        WebView(context).apply {
            // FIX: Ensure WebView layout is forced to match parent
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            webViewClient = object : WebViewClient() {
                // Sniffer for iframes and media
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url.toString()
                    if (url.contains(".mp4") || url.contains(".m3u8")) {
                        println("Snooped Media: $url")
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            setOnLongClickListener {
                val result = hitTestResult
                if (result.extra != null) {
                    startGhostDownload(context, result.extra!!)
                }
                true
            }
        }
    }

    // Main Layout
    Column(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()) { // Prevents UI from hiding under the clock
        
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search or URL") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = { 
                val formatted = if(urlInput.contains(".")) {
                    if(urlInput.startsWith("http")) urlInput else "https://$urlInput"
                } else {
                    "https://www.google.com/search?q=$urlInput"
                }
                loadUrl = formatted
                webView.loadUrl(formatted)
            }) {
                Text("GO")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = onVpnToggle) {
                Text("VPN")
            }
        }

        // THE FIX: Modifier.fillMaxSize() on the AndroidView
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // Prevents UI from hiding under home buttons
        )
    }
}

fun startGhostDownload(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ghost_Media_${System.currentTimeMillis()}")
        
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
