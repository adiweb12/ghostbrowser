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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BrowserWithVpn(
                    onStartVpn = { 
                        val intent = VpnService.prepare(this)
                        if (intent != null) startActivityForResult(intent, 0)
                        else startService(Intent(this, GhostVpnService::class.java))
                    }
                )
            }
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
fun BrowserWithVpn(onStartVpn: () -> Unit) {
    var url by remember { mutableStateOf("https://www.google.com") }
    var menuExpanded by remember { mutableStateOf(false) }
    var vpnEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghost Browser", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    // VPN Indicator Icon
                    Icon(
                        Icons.Default.Shield, 
                        contentDescription = null, 
                        tint = if (vpnEnabled) Color.Green else Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // Three Dots Menu
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Enable Ghost VPN") },
                            onClick = { 
                                vpnEnabled = true
                                onStartVpn()
                                menuExpanded = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear & Exit") },
                            onClick = { PrivacyManager.nukeSession() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Re-using the previous WebView logic here
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        webViewClient = WebViewClient()
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
