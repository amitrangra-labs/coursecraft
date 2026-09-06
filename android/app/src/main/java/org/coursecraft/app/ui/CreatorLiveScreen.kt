package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.LiveApi
import org.coursecraft.app.data.LiveSession

/** Creator: schedule and run live lectures (journey CJ-7). */
@Composable
fun CreatorLiveScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onOpenVideo: (LiveSession) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<LiveSession>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }
    var newTitle by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        try {
            sessions = LiveApi.listForCourse(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun run(block: suspend () -> Unit) {
        error = null
        scope.launch {
            try {
                block(); reloadKey++
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header("$title — live", onBack)

        OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Session title") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(videoId, { videoId = it }, label = { Text("YouTube (Live) id or URL") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(minutes, { minutes = it.filter { c -> c.isDigit() } },
            label = { Text("Starts in (minutes from now)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val t = newTitle.trim(); val v = videoId.trim()
                if (t.isNotEmpty() && v.isNotEmpty()) {
                    val startsAt = System.currentTimeMillis() + (minutes.toLongOrNull() ?: 0L) * 60_000L
                    run { LiveApi.schedule(accessToken, courseId, t, v, startsAt); newTitle = ""; videoId = ""; minutes = "0" }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Schedule live session") }

        sessions.forEach { s ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${s.status}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (s.status) {
                            "SCHEDULED" -> {
                                Button(onClick = { run { LiveApi.goLive(accessToken, s.id) } }) { Text("Go live") }
                                OutlinedButton(onClick = { run { LiveApi.cancel(accessToken, s.id) } }) { Text("Cancel") }
                            }
                            "LIVE" -> {
                                Button(onClick = { run { LiveApi.end(accessToken, s.id) } }) { Text("End") }
                                OutlinedButton(onClick = { onOpenVideo(s) }) { Text("Preview") }
                            }
                            else -> OutlinedButton(onClick = { onOpenVideo(s) }) { Text("Watch") }
                        }
                    }
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
