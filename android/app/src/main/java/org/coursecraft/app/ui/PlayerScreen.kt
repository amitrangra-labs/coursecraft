package org.coursecraft.app.ui

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.coursecraft.app.data.CourseApi

/**
 * Plays a YouTube lecture and saves playback progress (journeys LJ-2/LJ-3). Resumes from the last
 * saved position, throttles position saves to ~every 10s, and marks the lecture complete near the
 * end (~95%). (YouTube video does not play in the Android emulator — verify on a device.)
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Header(title, onBack)
        if (videoId.isBlank()) {
            Text("No video for this lecture.", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val io = CoroutineScope(Dispatchers.IO)
                YouTubePlayerView(context).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        private var duration = 0f
                        private var lastSavedSec = -100
                        private var markedComplete = false

                        private val trackProgress = lectureId.isNotBlank()

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
                                        // best-effort; will retry on next tick
                                    }
                                }
                            }
                        }
                    })
                }
            }
        )
    }
}
