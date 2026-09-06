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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.CourseApi
import org.coursecraft.app.data.CourseDetailView

/** Creator course management: add sections, add video lectures, publish (journeys CJ-2, CJ-5). */
@Composable
fun ManageCourseScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CourseDetailView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    var newSection by remember { mutableStateOf("") }
    val lectureTitle = remember { mutableStateMapOf<String, String>() }
    val lectureVideo = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(reloadKey) {
        try {
            detail = CourseApi.courseDetail(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun submit(block: suspend () -> Unit) {
        error = null
        scope.launch {
            try {
                block()
                reloadKey++
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(title, onBack)

        detail?.let { d ->
            Text("Status: ${d.course.status}", style = MaterialTheme.typography.bodyMedium)

            // Add a section
            OutlinedTextField(newSection, { newSection = it }, label = { Text("New section title") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val t = newSection.trim()
                    if (t.isNotEmpty()) submit { CourseApi.addSection(accessToken, courseId, t); newSection = "" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add section") }

            // Sections with lectures + inline add-lecture form
            d.sections.forEach { section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        section.lectures.forEach { lec ->
                            Text("• ${lec.title}", style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedTextField(
                            lectureTitle[section.id] ?: "",
                            { lectureTitle[section.id] = it },
                            label = { Text("Lecture title") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            lectureVideo[section.id] ?: "",
                            { lectureVideo[section.id] = it },
                            label = { Text("YouTube id or URL") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = {
                                val lt = (lectureTitle[section.id] ?: "").trim()
                                val lv = (lectureVideo[section.id] ?: "").trim()
                                if (lt.isNotEmpty() && lv.isNotEmpty()) submit {
                                    CourseApi.addLecture(accessToken, courseId, section.id, lt, lv)
                                    lectureTitle[section.id] = ""
                                    lectureVideo[section.id] = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add video lecture") }
                    }
                }
            }

            Button(
                onClick = { submit { CourseApi.publish(accessToken, courseId) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Publish course") }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
