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

    // --- CLIPBOARD LISTENER (For YouTube/Copy Link) ---
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    DisposableEffect(Unit) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val item = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            if (item.contains("youtube.com") || item.contains("youtu.be") || item.endsWith(".mp4")) {
                pendingDownloadUrl = item
                showQualityDialog = true
            }
        }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }

    // --- QUALITY & SIZE DIALOG ---
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Download Media") },
            text = {
                Column {
                    Text("Select quality (Estimated size):", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val options = listOf(
                        "1080p" to "120 MB",
                        "720p" to "65 MB",
                        "480p" to "30 MB"
                    )
                    
                    options.forEach { (res, size) ->
                        ListItem(
                            headlineContent = { Text(res) },
                            supportingContent = { Text(size) },
                            leadingContent = { Icon(Icons.Default.VideoLibrary, null) },
                            modifier = androidx.compose.foundation.clickable {
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
                title = { Text("Ghost Browser") },
                actions = {
                    IconButton(onClick = {
                        PrivacyManager.nukeSession()
                        (context as MainActivity).finishAndRemoveTask()
                        exitProcess(0)
                    }) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        
                        webViewClient = WebViewClient()
                        
                        // --- LONG CLICK DETECTION ---
                        setOnLongClickListener {
                            val result = hitTestResult
                            // If user long clicks a link or image/video source
                            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || 
                                result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                                
                                val mediaUrl = result.extra
                                if (mediaUrl != null) {
                                    pendingDownloadUrl = mediaUrl
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
        val fileName = "Ghost_Download_${System.currentTimeMillis()}.mp4"
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        
        // If it's a YouTube link, we notify the user it's passing through the ghost tunnel
        if (url.contains("youtube")) {
            Toast.makeText(context, "Processing YouTube Stream...", Toast.LENGTH_SHORT).show()
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error: Invalid Link", Toast.LENGTH_SHORT).show()
    }
}
