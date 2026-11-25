package com.example.crconnect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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

    fun sanitizeIdentifier(id: String?): String {
        if (id.isNullOrBlank() || id == "--" || id.equals("null", ignoreCase = true)) return ""
        return id.trim()
    }

    private fun getSanitizedKey(raw: String): String {
        val clean = sanitizeIdentifier(raw)
        return clean.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
    }

    fun getCachedProfileFile(context: Context, userIdentifier: String): File {
        val cacheDir = File(context.filesDir, "profile_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val safeKey = getSanitizedKey(userIdentifier).ifBlank { "default_user" }
        return File(cacheDir, "profile_$safeKey.jpg")
    }

    fun getValidLocalProfilePath(context: Context, vararg identifiers: String?): String? {
        for (raw in identifiers) {
            val clean = sanitizeIdentifier(raw)
            if (clean.isNotEmpty()) {
                val file = getCachedProfileFile(context, clean)
                if (file.exists() && file.length() > 0) {
                    return file.absolutePath
                }
            }
        }
        val defaultFile = getCachedProfileFile(context, "default_user")
        if (defaultFile.exists() && defaultFile.length() > 0) {
            return defaultFile.absolutePath
        }
        return null
    }

    fun saveAndCacheProfileImage(
        context: Context,
        imageUrlOrPath: String?,
        vararg userIdentifiers: String?,
        onResult: (String) -> Unit
    ) {
        val trimmed = imageUrlOrPath?.trim() ?: ""
        if (trimmed.isBlank()) {
            onResult("")
            return
        }

        val nonNullIds = userIdentifiers.mapNotNull {
            val clean = sanitizeIdentifier(it)
            if (clean.isNotEmpty()) clean else null
        }.distinct().toMutableList()

        if (!nonNullIds.contains("default_user")) {
            nonNullIds.add("default_user")
        }

        // If it's already a valid local file on disk, copy to all cache targets and return primary target
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("content://") && !trimmed.startsWith("data:")) {
            val localFile = File(trimmed)
            if (localFile.exists() && localFile.length() > 0) {
                var primaryTargetPath = localFile.absolutePath
                try {
                    for ((index, id) in nonNullIds.withIndex()) {
                        val targetFile = getCachedProfileFile(context, id)
                        if (localFile.absolutePath != targetFile.absolutePath) {
                            localFile.copyTo(targetFile, overwrite = true)
                        }
                        if (index == 0) {
                            primaryTargetPath = targetFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update target cache files", e)
                }
                onResult(primaryTargetPath)
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val primaryId = nonNullIds.firstOrNull() ?: "default_user"
            val primaryTargetFile = getCachedProfileFile(context, primaryId)
            val cachedPath = downloadOrCopyToFile(context, trimmed, primaryTargetFile)

            // Replicate the valid primary file to all other identifiers
            val savedFile = File(cachedPath)
            if (savedFile.exists() && savedFile.length() > 0) {
                for (id in nonNullIds) {
                    if (id != primaryId) {
                        try {
                            val replica = getCachedProfileFile(context, id)
                            if (savedFile.absolutePath != replica.absolutePath) {
                                savedFile.copyTo(replica, overwrite = true)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error replicating cached profile to $id", e)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(cachedPath)
            }
        }
    }

    fun loadBitmapFromPathOrBase64(pathOrBase64: String?): Bitmap? {
        val str = pathOrBase64?.trim() ?: return null
        if (str.isBlank()) return null
        return try {
            if (str.startsWith("data:") || str.contains("base64,")) {
                val clean = if (str.contains("base64,")) str.substringAfter("base64,") else str
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else if (!str.startsWith("http://") && !str.startsWith("https://") && !str.startsWith("content://")) {
                val file = File(str)
                if (file.exists() && file.length() > 0) BitmapFactory.decodeFile(file.absolutePath) else null
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from path/base64", e)
            null
        }
    }

    private fun downloadOrCopyToFile(context: Context, source: String, targetFile: File): String {
        return try {
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")

            if (source.startsWith("data:") || source.contains("base64,")) {
                val cleanBase64 = if (source.contains("base64,")) source.substringAfter("base64,") else source
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                FileOutputStream(tempFile).use { output ->
                    output.write(decodedBytes)
                }
            } else if (source.startsWith("http://") || source.startsWith("https://")) {
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
