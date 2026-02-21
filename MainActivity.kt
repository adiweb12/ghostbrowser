package com.ghost.browser // Ensure this matches your folder structure

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // PRIVACY: Wipe all traces from RAM/Disk on startup
        clearBrowserData()

        setContent {
            MaterialTheme {
                GhostBrowserScreen()
            }
        }
    }

    private fun clearBrowserData() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GhostBrowserScreen() {
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Custom UI Header ---
        Row(modifier = Modifier.padding(8.dp)) {
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.LightGray)
            )
            Button(onClick = { currentUrl = formatUrl(urlInput) }) {
                Text("Go")
            }
        }

        // --- The Stealth Engine ---
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                WebView(context).apply {
                    webViewInstance = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false // No permanent storage
                    settings.databaseEnabled = false
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // Automatically inject the iFrame sniffer
                            injectIframeSniffer(view)
                        }
                    }

                    // LONG CLICK MEDIA DOWNLOADER
                    setOnLongClickListener {
                        val result = hitTestResult
                        val downloadUrl = result.extra
                        if (downloadUrl != null) {
                            startDownload(context, downloadUrl)
                            Toast.makeText(context, "Downloading Media...", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    loadUrl(currentUrl)
                }
            },
            update = { it.loadUrl(currentUrl) }
        )
    }
}

fun formatUrl(input: String): String {
    return if (input.startsWith("http")) input else "https://www.google.com/search?q=$input"
}

// --- The JS Sniffer for Iframes & Hidden Videos ---
fun injectIframeSniffer(view: WebView?) {
    val snifferScript = """
        (function() {
            let media = [];
            // Scan Main DOM
            document.querySelectorAll('video, source, a[href$=".mp4"]').forEach(el => media.push(el.src || el.href));
            
            // Scan iFrames (X-Frame bypass attempt)
            let frames = document.getElementsByTagName('iframe');
            for (let i = 0; i < frames.size; i++) {
                try {
                    let frameDocs = frames[i].contentDocument.querySelectorAll('video');
                    frameDocs.forEach(v => media.push(v.src));
                } catch(e) { console.log("Frame blocked by policy"); }
            }
            return media;
        })();
    """.trimIndent()
    view?.evaluateJavascript(snifferScript) { results ->
        // Results contains the list of found media URLs
    }
}

fun startDownload(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Ghost Download")
        .setDescription("Downloading file from browser")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "GhostMedia_${System.currentTimeMillis()}")
    
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}
