package com.poojan.pathkeeper

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = PrefsRepository(applicationContext)
        val folderUriString = repo.getFolderUri() ?: return Result.success()

        val folderUri = Uri.parse(folderUriString)
        val images = WallpaperChanger.listImageUris(applicationContext, folderUri)
        if (images.isEmpty()) return Result.success()

        val recent = repo.getRecentUris()
        val chosen = WallpaperChanger.pickRandom(images, recent) ?: return Result.success()

        val applied = WallpaperChanger.applyWallpaper(applicationContext, chosen)
        if (applied) {
            repo.recordShown(chosen.toString())
        }
        return if (applied) Result.success() else Result.retry()
    }
}
