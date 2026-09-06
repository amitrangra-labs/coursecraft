package org.coursecraft.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.AssessmentApi
import org.coursecraft.app.data.SubmitResult
import org.coursecraft.app.data.TakeView

/** Learner: take an assessment and see the graded result (journeys LJ-4/LJ-5). */
@Composable
fun TakeAssessmentScreen(
    accessToken: String,
    assessmentId: String,
    title: String,
    onViewLeaderboard: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<TakeView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SubmitResult?>(null) }
    // questionId -> selected optionIds
    val answers = remember { mutableStateMapOf<String, Set<String>>() }

    LaunchedEffect(assessmentId) {
        try {
            view = AssessmentApi.take(accessToken, assessmentId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun toggle(questionId: String, optionId: String, single: Boolean) {
        val current = answers[questionId] ?: emptySet()
        answers[questionId] = if (single) {
            setOf(optionId)
        } else {
            if (optionId in current) current - optionId else current + optionId
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(title, onBack)

        val r = result
        when {
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            r != null -> {
                Text(if (r.passed) "Passed 🎉" else "Not passed",
                    style = MaterialTheme.typography.headlineSmall)
                Text("Score: ${r.score} / ${r.maxScore}", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onViewLeaderboard, modifier = Modifier.fillMaxWidth()) { Text("View leaderboard") }
            }
            view == null -> CircularProgressIndicator()
            else -> {
                view!!.questions.forEach { q ->
                    val single = q.type != "MULTIPLE"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${q.text}  (${q.points} pts)", style = MaterialTheme.typography.titleSmall)
                            q.options.forEach { opt ->
                                val selected = (answers[q.id] ?: emptySet()).contains(opt.id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable { toggle(q.id, opt.id, single) }
                                ) {
                                    if (single) {
                                        RadioButton(selected = selected, onClick = { toggle(q.id, opt.id, true) })
                                    } else {
                                        Checkbox(checked = selected, onCheckedChange = { toggle(q.id, opt.id, false) })
                                    }
                                    Text(opt.text)
                                }
                            }
                        }
                    }
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        error = null; busy = true
                        scope.launch {
                            try {
                                result = AssessmentApi.submit(accessToken, assessmentId,
                                    answers.mapValues { it.value.toList() })
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit") }
            }
        }
    }
}
