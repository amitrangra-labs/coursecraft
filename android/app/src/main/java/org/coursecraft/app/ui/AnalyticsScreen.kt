package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import org.coursecraft.app.data.CourseAnalytics
import org.coursecraft.app.data.CourseApi

/** Creator analytics dashboard (journey CJ-6). */
@Composable
fun AnalyticsScreen(accessToken: String, courseId: String, title: String, onBack: () -> Unit) {
    var data by remember { mutableStateOf<CourseAnalytics?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        try {
            data = CourseApi.courseAnalytics(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header("$title — analytics", onBack)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            data == null -> Text("Loading…")
            else -> {
                Text("Enrollments: ${data!!.enrollments}", style = MaterialTheme.typography.titleMedium)
                Text("Assessments", style = MaterialTheme.typography.titleMedium)
                if (data!!.assessments.isEmpty()) {
                    Text("No assessments yet.")
                } else {
                    data!!.assessments.forEach { a ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(a.title, style = MaterialTheme.typography.titleSmall)
                                Text("Attempts: ${a.attempts}  ·  Avg score: ${a.averagePercent}%",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
