package org.coursecraft.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.coursecraft.app.data.CourseApi
import org.coursecraft.app.data.CourseDetailView

/** Creator course management: add/rename/delete/reorder sections & lectures, publish, delete course. */
@Composable
fun ManageCourseScreen(
    accessToken: String,
    courseId: String,
    title: String,
    onDeleted: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CourseDetailView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    var newSection by remember { mutableStateOf("") }
    val lectureTitle = remember { mutableStateMapOf<String, String>() }
    val lectureVideo = remember { mutableStateMapOf<String, String>() }

    var editing by remember { mutableStateOf<Editing?>(null) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    LaunchedEffect(reloadKey) {
        try {
            detail = CourseApi.courseDetail(accessToken, courseId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    fun submit(block: suspend () -> Unit) {
        error = null
        scope.launch {
            try {
                block(); reloadKey++
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun reorderSections(ids: List<String>) = submit { CourseApi.reorderSections(accessToken, courseId, ids) }
    fun reorderLectures(sectionId: String, ids: List<String>) =
        submit { CourseApi.reorderLectures(accessToken, courseId, sectionId, ids) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(title, onBack)

        detail?.let { d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status: ${d.course.status}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    editing = Editing.CourseInfo(d.course.title, d.course.subject ?: "", d.course.level ?: "")
                }) { Text("Edit") }
                TextButton(onClick = { confirm = Confirm.DeleteCourse }) { Text("Delete") }
            }

            // Add a section
            OutlinedTextField(newSection, { newSection = it }, label = { Text("New section title") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    val t = newSection.trim()
                    if (t.isNotEmpty()) submit { CourseApi.addSection(accessToken, courseId, t); newSection = "" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add section") }

            val sectionIds = d.sections.map { it.id }
            d.sections.forEachIndexed { i, section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(enabled = i > 0, onClick = {
                                reorderSections(sectionIds.swapped(i, i - 1))
                            }) { Text("↑") }
                            TextButton(enabled = i < d.sections.lastIndex, onClick = {
                                reorderSections(sectionIds.swapped(i, i + 1))
                            }) { Text("↓") }
                            TextButton(onClick = { editing = Editing.SectionRename(section.id, section.title) }) { Text("Edit") }
                            TextButton(onClick = { confirm = Confirm.DeleteSection(section.id) }) { Text("Delete") }
                        }

                        val lectureIds = section.lectures.map { it.id }
                        section.lectures.forEachIndexed { j, lec ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("• ${lec.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                TextButton(enabled = j > 0, onClick = {
                                    reorderLectures(section.id, lectureIds.swapped(j, j - 1))
                                }) { Text("↑") }
                                TextButton(enabled = j < section.lectures.lastIndex, onClick = {
                                    reorderLectures(section.id, lectureIds.swapped(j, j + 1))
                                }) { Text("↓") }
                                TextButton(onClick = {
                                    editing = Editing.LectureEdit(section.id, lec.id, lec.title, lec.videoId ?: "")
                                }) { Text("Edit") }
                                TextButton(onClick = { confirm = Confirm.DeleteLecture(section.id, lec.id) }) { Text("Delete") }
                            }
                        }

                        OutlinedTextField(lectureTitle[section.id] ?: "", { lectureTitle[section.id] = it },
                            label = { Text("Lecture title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(lectureVideo[section.id] ?: "", { lectureVideo[section.id] = it },
                            label = { Text("YouTube id or URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(
                            onClick = {
                                val lt = (lectureTitle[section.id] ?: "").trim()
                                val lv = (lectureVideo[section.id] ?: "").trim()
                                if (lt.isNotEmpty() && lv.isNotEmpty()) submit {
                                    CourseApi.addLecture(accessToken, courseId, section.id, lt, lv)
                                    lectureTitle[section.id] = ""; lectureVideo[section.id] = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add video lecture") }
                    }
                }
            }

            Button(onClick = { submit { CourseApi.publish(accessToken, courseId) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Publish course")
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    // --- dialogs ---
    when (val e = editing) {
        is Editing.CourseInfo -> ThreeFieldDialog(
            "Edit course", e.title, e.subject, e.level,
            onSave = { t, s, l -> editing = null; submit { CourseApi.renameCourse(accessToken, courseId, t, s, l) } },
            onDismiss = { editing = null }
        )
        is Editing.SectionRename -> OneFieldDialog(
            "Rename section", "Title", e.title,
            onSave = { t -> editing = null; submit { CourseApi.renameSection(accessToken, courseId, e.id, t) } },
            onDismiss = { editing = null }
        )
        is Editing.LectureEdit -> TwoFieldDialog(
            "Edit lecture", "Title", e.title, "YouTube id or URL", e.video,
            onSave = { t, v -> editing = null; submit { CourseApi.updateLecture(accessToken, courseId, e.sectionId, e.id, t, v) } },
            onDismiss = { editing = null }
        )
        null -> {}
    }

    when (val c = confirm) {
        is Confirm.DeleteCourse -> ConfirmDialog(
            "Delete this course?",
            onConfirm = { confirm = null; scope.launch { try { CourseApi.deleteCourse(accessToken, courseId); onDeleted() } catch (ex: Exception) { error = ex.message } } },
            onDismiss = { confirm = null }
        )
        is Confirm.DeleteSection -> ConfirmDialog(
            "Delete this section and its lectures?",
            onConfirm = { confirm = null; submit { CourseApi.deleteSection(accessToken, courseId, c.sectionId) } },
            onDismiss = { confirm = null }
        )
        is Confirm.DeleteLecture -> ConfirmDialog(
            "Delete this lecture?",
            onConfirm = { confirm = null; submit { CourseApi.deleteLecture(accessToken, courseId, c.sectionId, c.lectureId) } },
            onDismiss = { confirm = null }
        )
        null -> {}
    }
}

private fun List<String>.swapped(a: Int, b: Int): List<String> =
    toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

private sealed interface Editing {
    data class CourseInfo(val title: String, val subject: String, val level: String) : Editing
    data class SectionRename(val id: String, val title: String) : Editing
    data class LectureEdit(val sectionId: String, val id: String, val title: String, val video: String) : Editing
}

private sealed interface Confirm {
    data object DeleteCourse : Confirm
    data class DeleteSection(val sectionId: String) : Confirm
    data class DeleteLecture(val sectionId: String, val lectureId: String) : Confirm
}

@Composable
private fun OneFieldDialog(title: String, label: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { onSave(value.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TwoFieldDialog(
    title: String, label1: String, initial1: String, label2: String, initial2: String,
    onSave: (String, String) -> Unit, onDismiss: () -> Unit
) {
    var v1 by remember { mutableStateOf(initial1) }
    var v2 by remember { mutableStateOf(initial2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(v1, { v1 = it }, label = { Text(label1) }, singleLine = true)
                OutlinedTextField(v2, { v2 = it }, label = { Text(label2) }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = v1.isNotBlank() && v2.isNotBlank(), onClick = { onSave(v1.trim(), v2.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThreeFieldDialog(
    title: String, initialTitle: String, initialSubject: String, initialLevel: String,
    onSave: (String, String, String) -> Unit, onDismiss: () -> Unit
) {
    var t by remember { mutableStateOf(initialTitle) }
    var s by remember { mutableStateOf(initialSubject) }
    var l by remember { mutableStateOf(initialLevel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(t, { t = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(s, { s = it }, label = { Text("Subject") }, singleLine = true)
                OutlinedTextField(l, { l = it }, label = { Text("Level") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { onSave(t.trim(), s.trim(), l.trim()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConfirmDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
