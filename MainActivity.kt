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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(Intent(this, GhostVpnService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GhostBrowserUI(onVpnClick = { prepareVpn() })
                }
            }
        }
    }

    private fun prepareVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnLauncher.launch(intent)
        else startService(Intent(this, GhostVpnService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhostBrowserUI(onVpnClick: () -> Unit) {
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var vpnEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                leadingIcon = {
                    IconButton(onClick = { 
                        vpnEnabled = !vpnEnabled
                        onVpnClick() 
                    }) {
                        Icon(Icons.Default.Lock, contentDescription = "VPN", tint = if(vpnEnabled) Color.Green else Color.Gray)
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val target = if (urlInput.contains(".")) {
                    if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                } else "https://duckduckgo.com/?q=${urlInput.replace(" ", "+")}"
                webViewInstance?.loadUrl(target)
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Go")
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/121.0.0.0 Safari/537.36"
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url.toString()
                            if (url.contains(".mp4") || url.contains(".m3u8")) {
                                // Logic for detected media
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    setOnLongClickListener {
                        val result = hitTestResult
                        result.extra?.let { executeDownload(context, it) }
                        true
                    }
                    loadUrl(urlInput)
                }
            }
        )
    }
}

fun executeDownload(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ghost_${System.currentTimeMillis()}")
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    Toast.makeText(context, "Ghost Download Started", Toast.LENGTH_SHORT).show()
}
