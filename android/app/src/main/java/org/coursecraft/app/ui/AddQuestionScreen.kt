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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.AssessmentApi

/** Creator: add auto-gradable questions to an assessment (journey CJ-4). */
@Composable
fun AddQuestionScreen(accessToken: String, assessmentId: String, title: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("SINGLE") }
    var points by remember { mutableStateOf("1") }
    val optionText = remember { mutableStateListOf("", "") }
    val optionCorrect = remember { mutableStateListOf(false, false) }
    var addedCount by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header(title, onBack)
        if (addedCount > 0) {
            Text("$addedCount question(s) added", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(text, { text = it }, label = { Text("Question") },
            modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SINGLE", "MULTIPLE", "TRUE_FALSE").forEach { t ->
                if (t == type) {
                    Button(onClick = { type = t }) { Text(label(t)) }
                } else {
                    OutlinedButton(onClick = { type = t }) { Text(label(t)) }
                }
            }
        }

        OutlinedTextField(points, { points = it.filter { c -> c.isDigit() } },
            label = { Text("Points") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Text("Options (check the correct one(s))", style = MaterialTheme.typography.bodyMedium)
        optionText.indices.forEach { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = optionCorrect[i], onCheckedChange = { optionCorrect[i] = it })
                OutlinedTextField(optionText[i], { optionText[i] = it },
                    label = { Text("Option ${i + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
        OutlinedButton(onClick = { optionText.add(""); optionCorrect.add(false) },
            modifier = Modifier.fillMaxWidth()) { Text("Add option") }

        Button(
            enabled = !busy,
            onClick = {
                val opts = optionText.indices
                    .map { optionText[it].trim() to optionCorrect[it] }
                    .filter { it.first.isNotEmpty() }
                error = null; busy = true
                scope.launch {
                    try {
                        AssessmentApi.addQuestion(accessToken, assessmentId, text.trim(), type,
                            points.toIntOrNull() ?: 1, opts)
                        addedCount++
                        text = ""; type = "SINGLE"; points = "1"
                        optionText.clear(); optionText.addAll(listOf("", ""))
                        optionCorrect.clear(); optionCorrect.addAll(listOf(false, false))
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add question") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun label(type: String) = when (type) {
    "SINGLE" -> "Single"
    "MULTIPLE" -> "Multiple"
    else -> "True/False"
}
