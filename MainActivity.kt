package com.example.ghostbrowser

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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    BrowserScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var menuExpanded by remember { mutableStateOf(false) }
    var vpnEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    
    val focusManager = LocalFocusManager.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghost Browser") },
                actions = {
                    Icon(Icons.Default.Shield, null, tint = if (vpnEnabled) Color.Green else Color.Gray)
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (vpnEnabled) "Stop VPN" else "Start VPN") },
                            onClick = {
                                if (vpnEnabled) {
                                    context.stopService(Intent(context, GhostVpnService::class.java))
                                } else {
                                    val vpnIntent = VpnService.prepare(context)
                                    if (vpnIntent != null) (context as ComponentActivity).startActivityForResult(vpnIntent, 0)
                                    else context.startService(Intent(context, GhostVpnService::class.java))
                                }
                                vpnEnabled = !vpnEnabled
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(text = { Text("Downloads") }, onClick = {
                            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                            menuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Clear & Exit") }, onClick = {
                            context.stopService(Intent(context, GhostVpnService::class.java))
                            PrivacyManager.nukeSession()
                            (context as MainActivity).finishAndRemoveTask()
                            exitProcess(0)
                        })
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    url = if (inputUrl.contains(".") && !inputUrl.contains(" ")) {
                        if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                    } else {
                        "https://www.google.com/search?q=$inputUrl"
                    }
                    focusManager.clearFocus()
                })
            )

            if (isLoading) {
                LinearProgressIndicator(progress = loadProgress, modifier = Modifier.fillMaxWidth())
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = false
                            mediaPlaybackRequiresUserGesture = false
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onVideoFound(videoUrl: String) {
                                (context as MainActivity).runOnUiThread {
                                    Toast.makeText(context, "Video detected!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, "VideoDetector")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, u: String?, fav: android.graphics.Bitmap?) {
                                isLoading = true
                                inputUrl = u ?: ""
                            }
                            override fun onPageFinished(view: WebView?, u: String?) {
                                isLoading = false
                                view?.evaluateJavascript(
                                    "(function() { " +
                                    "  var vids = document.getElementsByTagName('video');" +
                                    "  if(vids.length > 0) VideoDetector.onVideoFound(vids[0].src);" +
                                    "})();", null
                                )
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newP: Int) {
                                loadProgress = newP / 100f
                            }
                        }

                        setDownloadListener { dUrl, userAgent, contentDisp, mime, _ ->
                            downloadFile(ctx, dUrl, contentDisp, mime)
                        }

                        setOnLongClickListener {
                            val result = hitTestResult
                            val type = result.type
                            // Removed VIDEO_TYPE to fix compilation error
                            if (type == WebView.HitTestResult.IMAGE_TYPE || 
                                type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE ||
                                type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                                
                                val mediaUrl = result.extra
                                if (mediaUrl != null) {
                                    downloadFile(ctx, mediaUrl, null, null)
                                    true
                                } else false
                            } else false
                        }
                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                update = { it.loadUrl(url) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun downloadFile(context: Context, url: String, contentDisp: String?, mime: String?) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
        val fileName = URLUtil.guessFileName(url, contentDisp, mime)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Downloading: $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
    }
}
