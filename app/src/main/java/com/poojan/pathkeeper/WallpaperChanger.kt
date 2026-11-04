package com.poojan.pathkeeper

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object WallpaperChanger {

    private val IMAGE_MIME_PREFIXES = listOf("image/")

    /**
     * Lists image files directly inside the granted folder (non-recursive).
     */
    fun listImageUris(context: Context, folderUri: Uri): List<Uri> {
        val tree = DocumentFile.fromTreeUri(context, folderUri) ?: return emptyList()
        if (!tree.isDirectory) return emptyList()
        return tree.listFiles()
            .filter { file ->
                file.isFile && (file.type?.let { type -> IMAGE_MIME_PREFIXES.any { type.startsWith(it) } } ?: false)
            }
            .mapNotNull { it.uri }
    }

    /**
     * Picks a random image, avoiding the given recently-shown set when possible.
     */
    fun pickRandom(images: List<Uri>, recentlyShown: Set<String>): Uri? {
        if (images.isEmpty()) return null
        val candidates = images.filter { it.toString() !in recentlyShown }
        val pool = if (candidates.isEmpty()) images else candidates
        return pool.random()
    }

    var lastError: String? = null

    fun applyWallpaper(context: Context, imageUri: Uri): Boolean {
        return try {
            val stream = context.contentResolver.openInputStream(imageUri)
                ?: run { lastError = "Could not open image"; return false }
            stream.use {
                WallpaperManager.getInstance(context)
                    .setStream(it, null, true, WallpaperManager.FLAG_SYSTEM)
            }
            true
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            false
        }
    }

    suspend fun changeNow(context: Context, repo: PrefsRepository): String {
        val folder = repo.getFolderUri() ?: return "Pick a folder first."
        val images = listImageUris(context, Uri.parse(folder))
        val current = repo.getCurrentWallpaperUri()
        if (current != null && images.none { it.toString() == current }) {
            // Android keeps the last applied wallpaper even after its source file is deleted.
            // Drop the stale URI so the app preview cannot keep pointing at it.
            repo.clearCurrentWallpaperUri()
        }
        if (images.isEmpty()) return "No images found in the folder."
        val chosen = pickRandom(images, repo.getRecentUris()) ?: return "No images found."
        if (!applyWallpaper(context, chosen)) return "Failed to set wallpaper: $lastError"
        repo.recordShown(chosen.toString())
        return "Wallpaper changed."
    }

    /** Reconcile the saved preview with the selected folder when the app is opened. */
    suspend fun refreshIfCurrentImageWasRemoved(context: Context, repo: PrefsRepository) {
        val current = repo.getCurrentWallpaperUri() ?: return
        val folder = repo.getFolderUri() ?: run {
            repo.clearCurrentWallpaperUri()
            return
        }
        val images = listImageUris(context, Uri.parse(folder))
        if (images.none { it.toString() == current }) {
            repo.clearCurrentWallpaperUri()
            if (images.isNotEmpty()) changeNow(context, repo)
        }
    }
}
