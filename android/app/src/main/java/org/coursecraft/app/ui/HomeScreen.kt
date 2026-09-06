package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.coursecraft.app.BuildConfig
import org.coursecraft.app.data.BackendClient

/**
 * Phase 0 home screen: names the app, stubs sign-in (Supabase Auth lands next), and proves the
 * app can reach the backend by calling GET /api/health.
 */
@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Not checked yet") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CourseCraft", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Learn and teach — video lectures, live sessions, and assessments.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Button(onClick = { /* TODO: Supabase Auth sign-in (next in Phase 0). */ }) {
                Text("Sign in (coming soon)")
            }

            OutlinedButton(onClick = {
                status = "Checking…"
                scope.launch {
                    status = try {
                        "Backend OK: " + BackendClient.health()
                    } catch (e: Exception) {
                        "Backend unreachable: ${e.message}"
                    }
                }
            }) {
                Text("Check backend connection")
            }

            Text(status, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Text(
                "API: ${BuildConfig.BACKEND_BASE_URL}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
