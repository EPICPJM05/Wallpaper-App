package com.poojan.pathkeeper

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var repo: PrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = PrefsRepository(applicationContext)

        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            MaterialTheme {
                PathKeeperScreen(repo = repo)
            }
        }
    }
}

@Composable
fun PathKeeperScreen(repo: PrefsRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val folderUri by repo.folderUri.collectAsState(initial = null)
    val hour by repo.scheduleHour.collectAsState(initial = 6)
    val minute by repo.scheduleMinute.collectAsState(initial = 0)
    val lastChangedAt by repo.lastChangedAt.collectAsState(initial = 0L)

    var statusMessage by remember { mutableStateOf("") }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch {
                repo.setFolderUri(uri.toString())
                Scheduler.scheduleDaily(context, repo.getScheduleHour(), repo.getScheduleMinute())
                statusMessage = "Folder selected. Daily rotation scheduled."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PathKeeper", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Daily wallpaper rotation to keep you focused: spiritual, disciplined, motivated, grateful.",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text("Wallpaper folder", style = MaterialTheme.typography.titleMedium)
        Text(
            text = folderUri?.let { Uri.parse(it).lastPathSegment ?: it } ?: "No folder selected",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = { folderPicker.launch(null) }) {
            Text(if (folderUri == null) "Choose folder" else "Change folder")
        }

        HorizontalDivider()

        Text("Daily change time", style = MaterialTheme.typography.titleMedium)
        Text(
            text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = {
            TimePickerDialog(
                context,
                { _, h, m ->
                    scope.launch {
                        repo.setSchedule(h, m)
                        Scheduler.scheduleDaily(context, h, m)
                        statusMessage = "Schedule updated."
                    }
                },
                hour,
                minute,
                true
            ).show()
        }) {
            Text("Set time")
        }

        HorizontalDivider()

        Text("Status", style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (lastChangedAt > 0L)
                "Last changed: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastChangedAt))
            else "Not changed yet",
            style = MaterialTheme.typography.bodySmall
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (folderUri == null) {
                        statusMessage = "Pick a folder first."
                    } else {
                        statusMessage = "Changing wallpaper now..."
                        scope.launch {
                            statusMessage = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                WallpaperChanger.changeNow(context.applicationContext, repo)
                            }
                        }
                    }
                },
                enabled = folderUri != null
            ) {
                Text("Change now")
            }
        }

        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}
