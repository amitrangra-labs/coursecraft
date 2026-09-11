package org.coursecraft.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.coursecraft.app.data.CourseApi

/**
 * Plays a YouTube lecture and saves playback progress (journeys LJ-2/LJ-3). Resumes from the last
 * saved position, throttles saves to ~10s, and marks complete near the end (~95%). If the embedded
 * player can't render on this device (old WebView, DRM/certification, embedding disabled), it
 * degrades gracefully to an "Open in YouTube" button instead of a dead error.
 */
@Composable
fun PlayerScreen(
    accessToken: String,
    videoId: String,
    title: String,
    lectureId: String,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }

    fun openInYouTube() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        context.startActivity(intent)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Header(title, onBack)

        if (videoId.isBlank()) {
            Text("No video for this lecture.", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        if (failed) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "This video can't play in the in-app player on this device.",
                    textAlign = TextAlign.Center
                )
                Button(onClick = { openInYouTube() }) { Text("Open in YouTube") }
            }
            return@Column
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                val io = CoroutineScope(Dispatchers.IO)
                YouTubePlayerView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        private val trackProgress = lectureId.isNotBlank()
                        private var duration = 0f
                        private var lastSavedSec = -100
                        private var markedComplete = false

                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            if (!trackProgress) {
                                youTubePlayer.cueVideo(videoId, 0f)
                                return
                            }
                            io.launch {
                                val start = try {
                                    CourseApi.getProgress(accessToken, lectureId).positionSec.toFloat()
                                } catch (e: Exception) {
                                    0f
                                }
                                youTubePlayer.cueVideo(videoId, start)
                            }
                        }

                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            failed = true
                        }

                        override fun onVideoDuration(youTubePlayer: YouTubePlayer, d: Float) {
                            duration = d
                        }

                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                            if (!trackProgress) return
                            val sec = second.toInt()
                            val nearEnd = duration > 0 && second / duration >= 0.95f
                            if (sec - lastSavedSec >= 10 || (nearEnd && !markedComplete)) {
                                lastSavedSec = sec
                                if (nearEnd) markedComplete = true
                                io.launch {
                                    try {
                                        CourseApi.saveProgress(accessToken, lectureId, sec, nearEnd)
                                    } catch (e: Exception) {
                                        // best-effort
                                    }
                                }
                            }
                        }
                    })
                }
            }
        )

        // Always available, even when the embed does render — handy on any device.
        TextButton(onClick = { openInYouTube() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Open in YouTube ↗")
        }
    }
}
