package com.example.ghostbrowser

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    
    // Download States
    var pendingUrl by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // --- CLIPBOARD LISTENER FIX ---
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    LaunchedEffect(Unit) {
        clipboard.addPrimaryClipChangedListener {
            val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (item.startsWith("http")) {
                pendingUrl = item
                showDownloadDialog = true
            }
        }
    }

    // --- SMART BACK LOGIC ---
    BackHandler {
        if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack()
        else (context as MainActivity).moveTaskToBack(true)
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Top URL Bar
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp).statusBarsPadding(), 
                    verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { 
                                currentUrl = if (inputUrl.startsWith("http")) inputUrl else "https://google.com/search?q=$inputUrl"
                                webViewInstance?.loadUrl(currentUrl)
                            }) { Icon(Icons.Default.Search, null) }
                        }
                    )
                    IconButton(onClick = {
                        PrivacyManager.nukeSession()
                        (context as MainActivity).finishAndRemoveTask()
                        exitProcess(0)
                    }) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) }
                }

                // DOWNLOAD PROGRESS (VOLUME SLIDER STYLE)
                if (isDownloading) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("Downloading...", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = downloadProgress,
                            onValueChange = {},
                            modifier = Modifier.height(20.dp),
                            colors = SliderDefaults.colors(thumbColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Download Dialog
        if (showDownloadDialog) {
            AlertDialog(
                onDismissRequest = { showDownloadDialog = false },
                title = { Text("Ghost Downloader") },
                text = { Text("Do you want to download this link?") },
                confirmButton = {
                    Button(onClick = {
                        isDownloading = true
                        downloadFile(context, pendingUrl) { progress -> downloadProgress = progress }
                        showDownloadDialog = false
                    }) { Text("Download") }
                }
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            inputUrl = url ?: ""
                        }
                    }
                    // Long Click Download
                    setOnLongClickListener {
                        val result = hitTestResult
                        if (result.type != WebView.HitTestResult.UNKNOWN_TYPE) {
                            pendingUrl = result.extra ?: ""
                            showDownloadDialog = true
                            true
                        } else false
                    }
                    loadUrl(currentUrl)
                    webViewInstance = this
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        )
    }
}

private fun downloadFile(context: Context, url: String, onProgress: (Float) -> Unit) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ghost_${System.currentTimeMillis()}.mp4")
        
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // Mock Progress Tracking (For a real one, you'd query the Cursor)
        Thread {
            var progress = 0f
            while (progress < 1f) {
                Thread.sleep(500)
                progress += 0.1f
                onProgress(progress)
            }
        }.start()

        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Link Error", Toast.LENGTH_SHORT).show()
    }
}
