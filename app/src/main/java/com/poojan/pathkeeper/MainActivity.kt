package com.poojan.pathkeeper

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = PrefsRepository(applicationContext)

        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            PathKeeperTheme {
                PathKeeperScreen(repo = repo)
            }
        }
    }
}

private suspend fun loadPreview(context: android.content.Context, uri: Uri, targetPx: Int = 900): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outHeight / sample > targetPx * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?.asImageBitmap()
        } catch (e: Exception) {
            null
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
    val currentUri by repo.currentWallpaperUri.collectAsState(initial = null)

    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    val preview by produceState<ImageBitmap?>(initialValue = null, currentUri, lastChangedAt) {
        value = currentUri?.let { loadPreview(context, Uri.parse(it)) }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            scope.launch {
                repo.setFolderUri(uri.toString())
                Scheduler.scheduleDaily(context, repo.getScheduleHour(), repo.getScheduleMinute())
                status = "Folder saved. Daily rotation is on."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Night, NightSoft)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PATHKEEPER", style = MaterialTheme.typography.labelSmall, color = AccentWarm)
            Spacer(Modifier.height(4.dp))
            Text("Stay on your path", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Card),
                contentAlignment = Alignment.Center
            ) {
                val img = preview
                if (img != null) {
                    Image(
                        bitmap = img,
                        contentDescription = "Current wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        if (folderUri == null) "Choose a folder to begin" else "No wallpaper set yet",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (lastChangedAt > 0L)
                    "Changed " + SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(lastChangedAt))
                else "Not changed yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    busy = true
                    status = ""
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            WallpaperChanger.changeNow(context.applicationContext, repo)
                        }
                        busy = false
                    }
                },
                enabled = folderUri != null && !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Night)
            ) {
                Text(if (busy) "Changing..." else "Change now", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(12.dp))

            SettingRow(
                label = "FOLDER",
                value = folderUri?.let { Uri.parse(it).lastPathSegment?.substringAfterLast(':') ?: "Selected" }
                    ?: "Not selected",
                onClick = { folderPicker.launch(null) }
            )
            Spacer(Modifier.height(8.dp))
            SettingRow(
                label = "DAILY AT",
                value = String.format(Locale.getDefault(), "%02d:%02d", hour, minute),
                onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        scope.launch {
                            repo.setSchedule(h, m)
                            if (repo.getFolderUri() != null) Scheduler.scheduleDaily(context, h, m)
                            status = "Time updated."
                        }
                    }, hour, minute, true).show()
                }
            )

            if (status.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(status, style = MaterialTheme.typography.bodyMedium, color = AccentWarm, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
    }
}
