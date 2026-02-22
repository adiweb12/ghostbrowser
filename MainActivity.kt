package com.example.ghostbrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    ModernBrowser()
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ModernBrowser() {
    var url by remember { mutableStateOf("https://www.google.com") }
    var inputUrl by remember { mutableStateOf("https://www.google.com") }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    val focusManager = LocalFocusManager.current
    val rememberedWebView = remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (rememberedWebView.value?.canGoBack() == true) {
            rememberedWebView.value?.goBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // Address Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp)
                .background(Color(0xFF2C2C2C), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            TextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    url = if (inputUrl.contains(".") && !inputUrl.contains(" ")) {
                        if (inputUrl.startsWith("http")) inputUrl else "https://$inputUrl"
                    } else {
                        "https://www.google.com/search?q=$inputUrl"
                    }
                    focusManager.clearFocus()
                })
            )
            IconButton(onClick = { rememberedWebView.value?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    // PRIVACY SETTINGS
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = false // No local data storage
                        saveFormData = false      // No auto-fill saving
                        cacheMode = WebSettings.LOAD_NO_CACHE // No disk cache
                        databaseEnabled = false
                    }
                    
                    // Clear any existing cookies on launch
                    CookieManager.getInstance().setAcceptCookie(false)
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) {
                            isLoading = true
                            inputUrl = u ?: ""
                            view?.clearHistory() // Keep history clean
                        }
                        override fun onPageFinished(view: WebView?, u: String?) {
                            isLoading = false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newP: Int) {
                            progress = newP / 100f
                        }
                    }
                    loadUrl(url)
                    rememberedWebView.value = this
                }
            },
            update = { view ->
                if (view.url != url) { view.loadUrl(url) }
            }
        )
    }
}
