package com.example.ghostbrowser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the Ghost Service
        startService(Intent(this, GhostBrowserService::class.java))

        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                BrowserScreen()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var textFieldValue by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? = null

    Column(modifier = Modifier.fillMaxSize()) {
        // URL Bar
        Row(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search or type URL") }
            )
            Button(
                onClick = { url = textFieldValue },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Go")
            }
        }

        // Web Content
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl(url)
                    webView = this
                }
            },
            update = { view ->
                view.loadUrl(url)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
