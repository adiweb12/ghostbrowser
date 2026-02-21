package com.ghost.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val vpnRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) startService(Intent(this, GhostVpnService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // GHOST MODE: Wipe all RAM/Cookies on launch
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()

        setContent {
            GhostBrowserApp(onVpnToggle = { prepareVpn() })
        }
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
    var urlInput by remember { mutableStateOf("https://google.com") }
    val context = LocalContext.current
    
    // Remember WebView across recompositions
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false // Memory only
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            // User-Agent Spoofing (Desktop Mode for better privacy)
            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url.toString()
                    // MEDIA SNIFFER: Detects mp4/m3u8 even in iframes via network logs
                    if (url.contains(".mp4") || url.contains(".m3u8")) {
                        println("Ghost Sniffer Found Media: $url")
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            setOnLongClickListener {
                val result = hitTestResult
                if (result.extra != null) {
                    downloadFile(context, result.extra!!)
                    Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = urlInput, onValueChange = { urlInput = it }, Modifier.weight(1f))
            Button(onClick = { webView.loadUrl(if(urlInput.contains(".")) urlInput else "https://google.com/search?q=$urlInput") }) {
                Text("Go")
            }
            IconButton(onClick = onVpnToggle) {
                Text("VPN", color = Color.Red)
            }
        }
        AndroidView(factory = { webView }, modifier = Modifier.weight(1f))
    }
}

fun downloadFile(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ghost_${System.currentTimeMillis()}.mp4")
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
}
