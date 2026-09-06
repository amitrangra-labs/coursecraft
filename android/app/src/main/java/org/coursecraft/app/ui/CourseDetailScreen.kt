package org.coursecraft.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.coursecraft.app.data.CourseApi
import org.coursecraft.app.data.CourseDetailView
import org.coursecraft.app.data.LectureView

/** Learner course detail: sections + lectures; tap a video lecture to play it (journey LJ-1/LJ-2). */
@Composable
fun CourseDetailScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onPlay: (LectureView) -> Unit,
    onBack: () -> Unit
) {
    var detail by remember { mutableStateOf<CourseDetailView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        detail = try {
            CourseApi.courseDetail(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message; null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Header(title, onBack)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            detail == null -> CircularProgressIndicator()
            else -> detail!!.sections.forEach { section ->
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                section.lectures.forEach { lecture ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = lecture.videoId != null) { onPlay(lecture) }
                    ) {
                        Text(
                            "▶  ${lecture.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
