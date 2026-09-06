package org.coursecraft.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import org.coursecraft.app.data.CourseSummary

/** Generic list of courses; the loader decides catalog vs. my-courses. */
@Composable
fun CourseListScreen(
    title: String,
    emptyText: String,
    loader: suspend () -> List<CourseSummary>,
    onOpen: (CourseSummary) -> Unit,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<ListState>(ListState.Loading) }

    LaunchedEffect(title) {
        state = try {
            ListState.Loaded(loader())
        } catch (e: Exception) {
            ListState.Error(e.message ?: "Failed to load")
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Header(title, onBack)
        when (val s = state) {
            is ListState.Loading -> CircularProgressIndicator()
            is ListState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            is ListState.Loaded ->
                if (s.courses.isEmpty()) {
                    Text(emptyText, style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn {
                        items(s.courses) { course ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { onOpen(course) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                                    val meta = listOfNotNull(course.subject, course.level, course.status)
                                        .joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Text(meta, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }
}

private sealed interface ListState {
    data object Loading : ListState
    data class Loaded(val courses: List<CourseSummary>) : ListState
    data class Error(val message: String) : ListState
}
