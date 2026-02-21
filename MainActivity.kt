package com.example.kotlinbrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Essential for Android 15 & 16 (Edge-to-Edge)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                BrowserScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputTxt by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    var progress by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    // Handle System Back Button to go back in browser history
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                TextField(
                    value = inputTxt,
                    onValueChange = { inputTxt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("Search Google or type URL") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val formattedUrl = if (inputTxt.contains(".")) {
                                if (inputTxt.startsWith("http")) inputTxt else "https://$inputTxt"
                            } else {
                                "https://www.google.com/search?q=$inputTxt"
                            }
                            url = formattedUrl
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                // Progress Bar
                if (progress < 100) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ComposeWebView(
                url = url,
                onProgressChanged = { progress = it },
                onUrlChanged = { 
                    inputTxt = it 
                    url = it
                },
                onWebViewCreated = { webView = it }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWebView(
    url: String,
    onProgressChanged: (Int) -> Unit,
    onUrlChanged: (String) -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        url?.let { onUrlChanged(it) }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false // Let WebView handle the link
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                }
                
                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { view ->
            // Only load if the URL is different to prevent infinite loops
            if (view.url != url) {
                view.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
