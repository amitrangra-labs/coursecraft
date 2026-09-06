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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.coursecraft.app.data.AssessmentApi
import org.coursecraft.app.data.AssessmentSummary

/** Learner: a course's assessments, with Take and Leaderboard actions (journeys LJ-4, LJ-5). */
@Composable
fun LearnerAssessmentsScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onTake: (AssessmentSummary) -> Unit,
    onLeaderboard: (AssessmentSummary) -> Unit,
    onBack: () -> Unit
) {
    var list by remember { mutableStateOf<List<AssessmentSummary>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(courseId) {
        try {
            list = AssessmentApi.listForCourse(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header("$title — assessments", onBack)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            list == null -> Text("Loading…")
            list!!.isEmpty() -> Text("No assessments yet.")
            else -> list!!.forEach { a ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        Text("Pass mark: ${a.passMark}%", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onTake(a) }) { Text("Take") }
                            OutlinedButton(onClick = { onLeaderboard(a) }) { Text("Leaderboard") }
                        }
                    }
                }
            }
        }
    }
}
