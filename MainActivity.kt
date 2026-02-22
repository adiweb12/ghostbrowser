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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var currentUrl by remember { mutableStateOf("https://www.google.com") }
    val tabs = remember { mutableStateListOf("https://www.google.com") }
    var showTabSheet by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // --- SMART BACK LOGIC ---
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            // If no more history, just minimize or ask to exit
            (context as MainActivity).moveTaskToBack(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghost Browser", style = MaterialTheme.typography.titleSmall) },
                actions = {
                    // Tab Counter Button
                    IconButton(onClick = { showTabSheet = true }) {
                        BadgedBox(badge = { Badge { Text(tabs.size.toString()) } }) {
                            Icon(Icons.Default.Tab, "Tabs")
                        }
                    }
                    IconButton(onClick = {
                        PrivacyManager.nukeSession()
                        (context as MainActivity).finishAndRemoveTask()
                        exitProcess(0)
                    }) {
                        Icon(Icons.Default.ExitToApp, "Exit", tint = Color(0xFFCF6679))
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(actions = {
                TextField(
                    value = currentUrl,
                    onValueChange = { currentUrl = it },
                    modifier = Modifier.weight(1f).padding(8.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (!tabs.contains(currentUrl)) tabs.add(currentUrl)
                            webViewInstance?.loadUrl(currentUrl)
                        }) { Icon(Icons.Default.ArrowForward, null) }
                    },
                    singleLine = true
                )
            })
        }
    ) { padding ->
        // --- TAB SELECTION SHEET ---
        if (showTabSheet) {
            ModalBottomSheet(onDismissRequest = { showTabSheet = false }) {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    items(tabs) { tabUrl ->
                        ListItem(
                            headlineContent = { Text(tabUrl, maxLines = 1) },
                            modifier = Modifier.clickable {
                                currentUrl = tabUrl
                                webViewInstance?.loadUrl(tabUrl)
                                showTabSheet = false
                            },
                            trailingContent = {
                                IconButton(onClick = { if(tabs.size > 1) tabs.remove(tabUrl) }) {
                                    Icon(Icons.Default.Close, null)
                                }
                            }
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = false
                            // FORCE MOBILE USER AGENT
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                currentUrl = request?.url.toString()
                                return false
                            }
                        }
                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = { /* Updates handled by back handler and buttons */ },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun downloadFile(context: Context, url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Ghost_${System.currentTimeMillis()}.mp4")
    (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
}
