package com.ghost.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Android 16+ Edge-to-Edge support
        enableEdgeToEdge()

        setContent {
            // Material3 Theme wrapper to prevent black screen
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GhostBrowserUI()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhostBrowserUI() {
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Fixed: Address bar won't hide under the clock
    ) {
        // --- PRO UI: CUSTOM ADDRESS BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search or type URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                trailingIcon = {
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val target = if (urlInput.contains(".")) {
                        if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                    } else {
                        "https://duckduckgo.com/?q=${urlInput.replace(" ", "+")}"
                    }
                    webViewInstance?.loadUrl(target)
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Go")
            }
        }

        // --- THE BROWSER ENGINE ---
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(), // Fixed: Home buttons won't overlap
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // PROFESSIONAL BROWSER SETTINGS
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = false // Ghost Mode (No permanent storage)
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowFileAccess = false
                        databaseEnabled = false
                        // Desktop User Agent for better sniffing/less tracking
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        // SNIFFER: Detects media inside IFRAMES by watching network requests
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url.toString()
                            if (url.contains(".mp4") || url.contains(".m3u8") || url.contains("video")) {
                                // Log found media for downloading
                                println("GhostSniffer detected: $url")
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }

                    // LONG CLICK MEDIA DOWNLOADER
                    setOnLongClickListener {
                        val result = hitTestResult
                        val mediaUrl = result.extra
                        if (mediaUrl != null && (result.type == WebView.HitTestResult.IMAGE_TYPE || 
                            result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE)) {
                            executeDownload(context, mediaUrl)
                        }
                        true
                    }

                    loadUrl(urlInput)
                }
            }
        )
    }
}

// --- SECURE DOWNLOADER ---
fun executeDownload(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Ghost Download")
            .setDescription("Securely downloading media...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GhostMedia_${System.currentTimeMillis()}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
