package com.example.ghostbrowser

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                            // Nuke data and kill process
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
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            startService(Intent(this, GhostVpnService::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            startService(Intent(this, GhostVpnService::class.java))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserWithVpn(onStartVpn: () -> Unit, onStopVpn: () -> Unit, onExit: () -> Unit) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var menuExpanded by remember { mutableStateOf(false) }
    var vpnEnabled by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghost Browser", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    Icon(
                        Icons.Default.Shield, 
                        contentDescription = null, 
                        tint = if (vpnEnabled) Color.Green else Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(if (vpnEnabled) "Stop VPN" else "Start VPN") },
                            onClick = { 
                                if (vpnEnabled) onStopVpn() else onStartVpn()
                                vpnEnabled = !vpnEnabled
                                menuExpanded = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear & Exit") },
                            onClick = { onExit() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Address Bar (FIXED SEARCH)
            TextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                placeholder = { Text("Search or type URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val target = if (inputUrl.contains(".") && !inputUrl.contains(" ")) {
                        if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                    } else {
                        "https://www.google.com/search?q=$inputUrl"
                    }
                    url = target
                    focusManager.clearFocus()
                }),
                trailingIcon = {
                    IconButton(onClick = { webViewRef.value?.reload() }) {
                        Icon(Icons.Default.Refresh, null)
                    }
                }
            )

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        webViewClient = WebViewClient()
                        loadUrl(url)
                        webViewRef.value = this
                    }
                },
                update = { it.loadUrl(url) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
