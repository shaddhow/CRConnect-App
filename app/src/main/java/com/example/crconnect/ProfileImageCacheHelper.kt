package com.example.crconnect

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ProfileImageCacheHelper {

    private const val TAG = "ProfileImageCacheHelper"

    private fun getSanitizedKey(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
    }

    fun getCachedProfileFile(context: Context, userIdentifier: String): File {
        val cacheDir = File(context.filesDir, "profile_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val safeKey = getSanitizedKey(userIdentifier.ifBlank { "default_user" })
        return File(cacheDir, "profile_$safeKey.jpg")
    }

    fun getValidLocalProfilePath(context: Context, userIdentifier: String): String? {
        val file = getCachedProfileFile(context, userIdentifier)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    fun saveAndCacheProfileImage(
        context: Context,
        imageUrlOrPath: String,
        userIdentifier: String,
        onResult: (String) -> Unit
    ) {
        val trimmed = imageUrlOrPath.trim()
        if (trimmed.isBlank()) {
            onResult("")
            return
        }

        // If it's already a valid local file on disk, return it immediately
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("content://")) {
            val localFile = File(trimmed)
            if (localFile.exists() && localFile.length() > 0) {
                onResult(localFile.absolutePath)
                return
            }
        }

        val targetFile = getCachedProfileFile(context, userIdentifier)

        CoroutineScope(Dispatchers.IO).launch {
            val cachedPath = downloadOrCopyToFile(context, trimmed, targetFile)
            withContext(Dispatchers.Main) {
                onResult(cachedPath)
            }
        }
    }

    private fun downloadOrCopyToFile(context: Context, source: String, targetFile: File): String {
        return try {
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

            if (source.startsWith("http://") || source.startsWith("https://")) {
                val url = URL(source)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    Log.e(TAG, "HTTP error downloading profile photo: ${conn.responseCode}")
                    return if (targetFile.exists() && targetFile.length() > 0) targetFile.absolutePath else source
                }
            } else if (source.startsWith("content://")) {
                val uri = Uri.parse(source)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                val srcFile = File(source)
                if (srcFile.exists()) {
                    srcFile.inputStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    return source
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tempFile.renameTo(targetFile)
                Log.d(TAG, "Profile image saved locally at: ${targetFile.absolutePath}")
                targetFile.absolutePath
            } else {
                if (targetFile.exists() && targetFile.length() > 0) targetFile.absolutePath else source
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching profile image locally", e)
            if (targetFile.exists() && targetFile.length() > 0) targetFile.absolutePath else source
        }
    }
}
