package org.coursecraft.app.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Plays a YouTube lecture via an embedded player (journey LJ-2). MVP uses the YouTube IFrame in a
 * WebView — free hosting/delivery; we only hold the video id. (Media3/HLS is for provider-hosted
 * video later.)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(videoId: String, title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Header(title, onBack)
        if (videoId.isBlank()) {
            Text("No video for this lecture.", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    // A WebChromeClient is required for the YouTube IFrame video surface to render
                    // (without it the player stays black).
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    val html = """
                        <html style="height:100%">
                        <body style="height:100%;margin:0;background:#000">
                          <iframe width="100%" height="100%"
                                  src="https://www.youtube.com/embed/$videoId?playsinline=1&autoplay=1"
                                  frameborder="0" allowfullscreen
                                  allow="autoplay; encrypted-media; fullscreen"></iframe>
                        </body></html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
                }
            }
        )
    }
}
