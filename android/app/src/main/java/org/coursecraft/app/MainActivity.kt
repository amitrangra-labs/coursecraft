package org.coursecraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.coursecraft.app.ui.HomeScreen
import org.coursecraft.app.ui.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CourseCraftTheme {
                CourseCraftApp()
            }
        }
    }
}

@Composable
fun CourseCraftApp() {
    // Phase 0 keeps the session in memory only; persistent/refresh-token storage comes later.
    var accessToken by rememberSaveable { mutableStateOf<String?>(null) }

    val token = accessToken
    if (token == null) {
        SignInScreen(onSignedIn = { accessToken = it })
    } else {
        HomeScreen(accessToken = token, onSignOut = { accessToken = null })
    }
}

@Composable
fun CourseCraftTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}
