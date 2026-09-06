package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.BackendClient
import org.coursecraft.app.data.CourseApi
import org.json.JSONObject

/**
 * Signed-in hub. Shows the profile and routes to the learner catalog and (for creators) their
 * course management. Learners can upgrade to creator here (journey CJ-1).
 */
@Composable
fun HomeScreen(
    accessToken: String,
    onSignOut: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var continueItem by remember { mutableStateOf<org.coursecraft.app.data.ContinueItem?>(null) }

    LaunchedEffect(accessToken) {
        try {
            val json = JSONObject(BackendClient.me(accessToken))
            displayName = json.optString("displayName")
            role = json.optString("role")
        } catch (e: Exception) {
            error = e.message
        }
        continueItem = try {
            CourseApi.continueLearning(accessToken)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (role == null && error == null) {
            CircularProgressIndicator()
            return@Column
        }

        Text("Hi, $displayName", style = MaterialTheme.typography.headlineSmall)
        Text("Role: ${role ?: "?"}", style = MaterialTheme.typography.bodyMedium)

        continueItem?.let { c ->
            Button(
                onClick = { onNavigate(Screen.Player(c.videoId, c.lectureTitle, c.lectureId)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("▶ Continue: ${c.lectureTitle}") }
        }

        Button(onClick = { onNavigate(Screen.Catalog) }, modifier = Modifier.fillMaxWidth()) {
            Text("Browse courses")
        }
        Button(onClick = { onNavigate(Screen.MyLearning) }, modifier = Modifier.fillMaxWidth()) {
            Text("My Learning")
        }

        if (role == "CREATOR") {
            Button(onClick = { onNavigate(Screen.MyCourses) }, modifier = Modifier.fillMaxWidth()) {
                Text("My courses")
            }
            OutlinedButton(onClick = { onNavigate(Screen.CreateCourse) }, modifier = Modifier.fillMaxWidth()) {
                Text("Create a course")
            }
        } else {
            OutlinedButton(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            role = CourseApi.becomeCreator(accessToken)
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Become a creator")
            }
        }

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
