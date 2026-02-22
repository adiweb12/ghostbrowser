package com.example.ghostbrowser

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.webkit.*
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
                    BrowserWithVpn(
                        onStartVpn = { startVpn() },
                        onStopVpn = { stopService(Intent(this, GhostVpnService::class.java)) },
                        onExit = {
                            stopService(Intent(this, GhostVpnService::class.java))
                            PrivacyManager.nukeSession()
                            finishAndRemoveTask()
                            exitProcess(0)
                        }
                    )
                }
            }
        }
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, 0)
        else startService(Intent(this, GhostVpnService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserWithVpn(onStartVpn: () -> Unit, onStopVpn: () -> Unit, onExit: () -> Unit) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var menuExpanded by remember { mutableStateOf(false) }
    var vpnEnabled by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

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
                                if (vpnEnabled) onStopVpn() else onStartVpn()
                                vpnEnabled = !vpnEnabled
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(text = { Text("Clear & Exit") }, onClick = onExit)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Address Bar
            TextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val formattedUrl = if (inputUrl.contains(".") && !inputUrl.contains(" ")) {
                        if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                    } else {
                        "https://www.google.com/search?q=$inputUrl"
                    }
                    url = formattedUrl
                    focusManager.clearFocus()
                })
            )

            // Loading Bar
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                loadProgress = 0f
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                            }
                        }
                        loadUrl(url)
                    }
                },
                update = { it.loadUrl(url) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
