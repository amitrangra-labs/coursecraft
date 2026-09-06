package org.coursecraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.coursecraft.app.ui.CourseDetailScreen
import org.coursecraft.app.ui.CourseListScreen
import org.coursecraft.app.ui.CreateCourseScreen
import org.coursecraft.app.ui.HomeScreen
import org.coursecraft.app.ui.ManageCourseScreen
import org.coursecraft.app.ui.PlayerScreen
import org.coursecraft.app.ui.Screen
import org.coursecraft.app.ui.SignInScreen
import org.coursecraft.app.data.CourseApi

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
    var accessToken by rememberSaveable { mutableStateOf<String?>(null) }
    val token = accessToken

    if (token == null) {
        SignInScreen(onSignedIn = { accessToken = it })
        return
    }

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    fun navigate(screen: Screen) = backStack.add(screen)
    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { back() }

    when (val screen = backStack.last()) {
        Screen.Home -> HomeScreen(
            accessToken = token,
            onSignOut = {
                accessToken = null
                backStack.clear()
                backStack.add(Screen.Home)
            },
            onNavigate = { navigate(it) }
        )

        Screen.Catalog -> CourseListScreen(
            title = "Courses",
            emptyText = "No published courses yet.",
            loader = { CourseApi.catalog(token) },
            onOpen = { navigate(Screen.CourseDetail(it.id, it.title)) },
            onBack = { back() }
        )

        Screen.MyCourses -> CourseListScreen(
            title = "My courses",
            emptyText = "You haven't created any courses yet.",
            loader = { CourseApi.myCourses(token) },
            onOpen = { navigate(Screen.ManageCourse(it.id, it.title)) },
            onBack = { back() }
        )

        Screen.CreateCourse -> CreateCourseScreen(
            accessToken = token,
            onCreated = {
                back()
                navigate(Screen.ManageCourse(it.id, it.title))
            },
            onBack = { back() }
        )

        is Screen.CourseDetail -> CourseDetailScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onPlay = { navigate(Screen.Player(it.videoId ?: "", it.title)) },
            onBack = { back() }
        )

        is Screen.ManageCourse -> ManageCourseScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onBack = { back() }
        )

        is Screen.Player -> PlayerScreen(
            videoId = screen.videoId,
            title = screen.title,
            onBack = { back() }
        )
    }
}

@Composable
fun CourseCraftTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}
