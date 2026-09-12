package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.coursecraft.app.data.BackendClient
import org.coursecraft.app.data.ContinueItem
import org.coursecraft.app.data.CourseApi
import org.coursecraft.app.data.LiveApi
import org.coursecraft.app.data.LiveSession
import org.json.JSONObject

/**
 * Signed-in hub. Loads the profile; if that fails because the session is no longer valid it drops
 * back to sign-in, and "Sign out" is always reachable so a stale session is never a dead end.
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
    var continueItem by remember { mutableStateOf<ContinueItem?>(null) }
    var liveNow by remember { mutableStateOf<List<LiveSession>>(emptyList()) }
    var editingName by remember { mutableStateOf(false) }

    LaunchedEffect(accessToken) {
        try {
            val json = JSONObject(BackendClient.me(accessToken))
            displayName = json.optString("displayName")
            role = json.optString("role")
        } catch (e: Exception) {
            val msg = e.message ?: "Could not load your profile"
            // A stale/expired session (401) means the token is no longer usable — sign out.
            if (msg.contains("401") || msg.contains("unauthorized", ignoreCase = true)) {
                onSignOut()
                return@LaunchedEffect
            }
            error = msg
        }
        continueItem = try {
            CourseApi.continueLearning(accessToken)
        } catch (e: Exception) {
            null
        }
        liveNow = try {
            LiveApi.liveNow(accessToken)
        } catch (e: Exception) {
            emptyList()
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
        } else {
            Text("Hi, ${displayName.ifBlank { "there" }}", style = MaterialTheme.typography.headlineSmall)
            role?.let { Text("Role: $it", style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = { editingName = true }) { Text("Edit name") }

            if (liveNow.isNotEmpty()) {
                Text("🔴 Live now", style = MaterialTheme.typography.titleMedium)
                liveNow.forEach { s ->
                    Button(
                        onClick = { onNavigate(Screen.Player(s.videoId, s.title, "")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Join: ${s.title}") }
                }
            }

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
            } else if (role == "LEARNER") {
                OutlinedButton(
                    onClick = {
                        busy = true; error = null
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
                ) { Text("Become a creator") }
            }
        }

        // Always reachable, even while loading or after an error.
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }

    if (editingName) {
        var newName by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Edit display name") },
            text = {
                OutlinedTextField(newName, { newName = it }, singleLine = true,
                    label = { Text("Display name") })
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val n = newName.trim()
                        editingName = false
                        scope.launch {
                            try {
                                CourseApi.updateDisplayName(accessToken, n)
                                displayName = n
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingName = false }) { Text("Cancel") }
            }
        )
    }
}
