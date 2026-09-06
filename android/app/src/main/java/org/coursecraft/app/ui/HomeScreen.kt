package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.coursecraft.app.data.BackendClient
import org.json.JSONObject

/**
 * Signed-in home. Proves the end-to-end path: Supabase JWT -> backend verifies it -> returns the
 * provisioned profile from GET /api/me.
 */
@Composable
fun HomeScreen(accessToken: String, onSignOut: () -> Unit) {
    var state by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }

    LaunchedEffect(accessToken) {
        state = try {
            val json = JSONObject(BackendClient.me(accessToken))
            ProfileState.Loaded(
                displayName = json.optString("displayName"),
                email = json.optString("email"),
                role = json.optString("role")
            )
        } catch (e: Exception) {
            ProfileState.Error(e.message ?: "Failed to load profile")
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                is ProfileState.Loading -> CircularProgressIndicator()

                is ProfileState.Loaded -> {
                    Text("Welcome, ${s.displayName}", style = MaterialTheme.typography.headlineSmall)
                    Text(s.email, style = MaterialTheme.typography.bodyMedium)
                    Text("Role: ${s.role}", style = MaterialTheme.typography.bodyMedium)
                }

                is ProfileState.Error -> Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }
    }
}

private sealed interface ProfileState {
    data object Loading : ProfileState
    data class Loaded(val displayName: String, val email: String, val role: String) : ProfileState
    data class Error(val message: String) : ProfileState
}
