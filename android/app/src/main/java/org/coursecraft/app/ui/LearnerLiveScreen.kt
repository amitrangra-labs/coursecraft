package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.coursecraft.app.data.LiveApi
import org.coursecraft.app.data.LiveSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Learner: attend live lectures or watch recordings (journey LJ-7). */
@Composable
fun LearnerLiveScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onOpenVideo: (LiveSession) -> Unit,
    onBack: () -> Unit
) {
    var sessions by remember { mutableStateOf<List<LiveSession>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        try {
            sessions = LiveApi.listForCourse(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header("$title — live", onBack)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            sessions == null -> Text("Loading…")
            sessions!!.isEmpty() -> Text("No live sessions scheduled.")
            else -> sessions!!.forEach { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.title, style = MaterialTheme.typography.titleMedium)
                        when (s.status) {
                            "LIVE" -> {
                                Text("🔴 Live now", style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { onOpenVideo(s) }) { Text("Join") }
                            }
                            "SCHEDULED" -> Text("Starts ${formatTime(s.startsAtEpochMs)}",
                                style = MaterialTheme.typography.bodyMedium)
                            "ENDED" -> {
                                Text("Ended", style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = { onOpenVideo(s) }) { Text("Watch recording") }
                            }
                            else -> Text("Canceled", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(epochMs))
