package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.CourseApi
import org.coursecraft.app.data.CourseSummary

/** Create a new (DRAFT) course, then jump into managing it (journey CJ-2). */
@Composable
fun CreateCourseScreen(
    accessToken: String,
    onCreated: (CourseSummary) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header("Create a course", onBack)
        OutlinedTextField(title, { title = it }, label = { Text("Title") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(subject, { subject = it }, label = { Text("Subject (optional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(level, { level = it }, label = { Text("Level (optional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        onCreated(CourseApi.createCourse(accessToken, title.trim(), subject.trim(), level.trim()))
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && title.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create")
        }
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
