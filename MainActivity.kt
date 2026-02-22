package com.example.ghostbrowser

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable // FIXED: Missing import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var showQualityDialog by remember { mutableStateOf(false) }
    var pendingDownloadUrl by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // --- CLIPBOARD LISTENER ---
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    DisposableEffect(Unit) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0).text.toString()
                if (item.startsWith("http") && (item.contains("youtube") || item.contains("youtu.be") || item.contains("fb.watch"))) {
                    pendingDownloadUrl = item
                    showQualityDialog = true
                }
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }

    // --- QUALITY DIALOG ---
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Download Media") },
            text = {
                Column {
                    val options = listOf(
                        Triple("1080p", "120 MB", Color(0xFFBB86FC)),
                        Triple("720p", "65 MB", Color(0xFF03DAC6)),
                        Triple("480p", "30 MB", Color.Gray)
                    )
                    options.forEach { (res, size, color) ->
                        ListItem(
                            headlineContent = { Text(res, color = color) },
                            supportingContent = { Text(size) },
                            leadingContent = { Icon(Icons.Default.PlayCircle, null, tint = color) },
                            modifier = Modifier.clickable {
                                downloadFile(context, pendingDownloadUrl)
                                showQualityDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghost Browser", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = {
                        PrivacyManager.nukeSession()
                        (context as MainActivity).finishAndRemoveTask()
                        exitProcess(0)
                    }) {
                        Icon(Icons.Default.ExitToApp, "Exit", tint = Color(0xFFCF6679))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = false
                            // Spoof as Desktop to get direct stream links easier
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        }
                        webViewClient = WebViewClient()
                        setOnLongClickListener {
                            val result = hitTestResult
                            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || 
                                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                                pendingDownloadUrl = result.extra ?: ""
                                if (pendingDownloadUrl.isNotEmpty()) {
                                    showQualityDialog = true
                                    true
                                } else false
                            } else false
                        }
                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun downloadFile(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
        val fileName = "Ghost_${System.currentTimeMillis()}.mp4"
        
        request.apply {
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            // Add Headers to mimic a real browser to bypass YouTube/FB blocks
            addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Ghost Downloader Started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: Secure link required", Toast.LENGTH_SHORT).show()
    }
}
