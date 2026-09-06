package org.coursecraft.app.ui

import androidx.compose.foundation.clickable
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
import org.coursecraft.app.data.AssessmentApi
import org.coursecraft.app.data.AssessmentSummary

/** Creator: list a course's assessments and create new ones (journey CJ-4). */
@Composable
fun CreatorAssessmentsScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onOpenAssessment: (AssessmentSummary) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<AssessmentSummary>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }
    var newTitle by remember { mutableStateOf("") }
    var passMark by remember { mutableStateOf("50") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadKey) {
        try {
            list = AssessmentApi.listForCourse(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header("$title — assessments", onBack)

        OutlinedTextField(newTitle, { newTitle = it }, label = { Text("New assessment title") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(passMark, { passMark = it.filter { c -> c.isDigit() } },
            label = { Text("Pass mark %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val t = newTitle.trim()
                val pm = passMark.toIntOrNull() ?: 50
                if (t.isNotEmpty()) {
                    error = null
                    scope.launch {
                        try {
                            AssessmentApi.createAssessment(accessToken, courseId, t, pm)
                            newTitle = ""; reloadKey++
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Create assessment") }

        list.forEach { a ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenAssessment(a) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(a.title, style = MaterialTheme.typography.titleMedium)
                    Text("Pass mark: ${a.passMark}%  ·  tap to add questions",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
