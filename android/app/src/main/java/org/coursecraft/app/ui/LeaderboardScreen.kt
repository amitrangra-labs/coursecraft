package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.coursecraft.app.data.AssessmentApi
import org.coursecraft.app.data.LeaderboardEntry

/** Leaderboard: best score per learner, ranked (journey LJ-5). */
@Composable
fun LeaderboardScreen(accessToken: String, assessmentId: String, title: String, onBack: () -> Unit) {
    var rows by remember { mutableStateOf<List<LeaderboardEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(assessmentId) {
        try {
            rows = AssessmentApi.leaderboard(accessToken, assessmentId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Header("$title — leaderboard", onBack)
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            rows == null -> Text("Loading…")
            rows!!.isEmpty() -> Text("No attempts yet — be the first!")
            else -> rows!!.forEachIndexed { i, row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("#${i + 1}  ${row.displayName}", style = MaterialTheme.typography.titleMedium)
                        Text("${row.bestScore} / ${row.maxScore}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
