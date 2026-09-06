package org.coursecraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.Modifier
import org.coursecraft.app.ui.AddQuestionScreen
import org.coursecraft.app.ui.AnalyticsScreen
import org.coursecraft.app.ui.CourseDetailScreen
import org.coursecraft.app.ui.CourseListScreen
import org.coursecraft.app.ui.CreateCourseScreen
import org.coursecraft.app.ui.CreatorAssessmentsScreen
import org.coursecraft.app.ui.CreatorLiveScreen
import org.coursecraft.app.ui.HomeScreen
import org.coursecraft.app.ui.LearnerLiveScreen
import org.coursecraft.app.ui.LeaderboardScreen
import org.coursecraft.app.ui.LearnerAssessmentsScreen
import org.coursecraft.app.ui.ManageCourseScreen
import org.coursecraft.app.ui.PlayerScreen
import org.coursecraft.app.ui.Screen
import org.coursecraft.app.ui.SignInScreen
import org.coursecraft.app.ui.TakeAssessmentScreen
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

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
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

        Screen.MyLearning -> CourseListScreen(
            title = "My Learning",
            emptyText = "You haven't enrolled in any courses yet.",
            loader = { CourseApi.myLearning(token) },
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
            onPlay = { navigate(Screen.Player(it.videoId ?: "", it.title, it.id)) },
            onOpenAssessments = { navigate(Screen.LearnerAssessments(screen.courseId, screen.title)) },
            onOpenLive = { navigate(Screen.LearnerLive(screen.courseId, screen.title)) },
            onBack = { back() }
        )

        is Screen.ManageCourse -> ManageCourseScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onOpenAssessments = { navigate(Screen.CreatorAssessments(screen.courseId, screen.title)) },
            onOpenLive = { navigate(Screen.CreatorLive(screen.courseId, screen.title)) },
            onOpenAnalytics = { navigate(Screen.Analytics(screen.courseId, screen.title)) },
            onDeleted = { back() },
            onBack = { back() }
        )

        is Screen.Player -> PlayerScreen(
            accessToken = token,
            videoId = screen.videoId,
            title = screen.title,
            lectureId = screen.lectureId,
            onBack = { back() }
        )

        is Screen.CreatorAssessments -> CreatorAssessmentsScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onOpenAssessment = { navigate(Screen.AddQuestion(it.id, it.title)) },
            onBack = { back() }
        )

        is Screen.AddQuestion -> AddQuestionScreen(
            accessToken = token,
            assessmentId = screen.assessmentId,
            title = screen.title,
            onBack = { back() }
        )

        is Screen.LearnerAssessments -> LearnerAssessmentsScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onTake = { navigate(Screen.TakeAssessment(it.id, it.title)) },
            onLeaderboard = { navigate(Screen.Leaderboard(it.id, it.title)) },
            onBack = { back() }
        )

        is Screen.TakeAssessment -> TakeAssessmentScreen(
            accessToken = token,
            assessmentId = screen.assessmentId,
            title = screen.title,
            onViewLeaderboard = { navigate(Screen.Leaderboard(screen.assessmentId, screen.title)) },
            onBack = { back() }
        )

        is Screen.Leaderboard -> LeaderboardScreen(
            accessToken = token,
            assessmentId = screen.assessmentId,
            title = screen.title,
            onBack = { back() }
        )

        is Screen.CreatorLive -> CreatorLiveScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onOpenVideo = { navigate(Screen.Player(it.videoId, it.title, "")) },
            onBack = { back() }
        )

        is Screen.LearnerLive -> LearnerLiveScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onOpenVideo = { navigate(Screen.Player(it.videoId, it.title, "")) },
            onBack = { back() }
        )

        is Screen.Analytics -> AnalyticsScreen(
            accessToken = token,
            courseId = screen.courseId,
            title = screen.title,
            onBack = { back() }
        )
    }
    }
}

@Composable
fun CourseCraftTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}
