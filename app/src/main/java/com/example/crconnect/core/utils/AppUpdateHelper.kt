package com.example.crconnect.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateHelper {
    private const val TAG = "AppUpdateHelper"
    private const val APK_FILE_NAME = "CRConnect-latest.apk"

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open unknown app sources settings", e)
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (progress: Float, downloadedMb: Float, totalMb: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var currentUrl = downloadUrl
            var redirectCount = 0
            val maxRedirects = 6

            // Follow HTTP Redirects (GitHub Release assets redirect to AWS S3)
            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "CRConnect-App-Updater")
                    setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308
                ) {
                    val newLocation = connection.getHeaderField("Location")
                    if (newLocation != null && newLocation.isNotBlank()) {
                        currentUrl = newLocation
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            if (connection == null || connection.responseCode != HttpURLConnection.HTTP_OK) {
                val code = connection?.responseCode ?: -1
                return@withContext Result.failure(Exception("Server returned HTTP $code"))
            }

            val totalBytes = connection.contentLength.toLong()
            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            val destFile = File(destDir, APK_FILE_NAME)

            if (destFile.exists()) {
                destFile.delete()
            }

            inputStream = connection.inputStream
            outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalRead: Long = 0L
            var lastReportTime = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastReportTime > 80 || totalRead == totalBytes) {
                    lastReportTime = now
                    val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes.toFloat() else 0f
                    val downloadedMb = totalRead / (1024f * 1024f)
                    val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
                    withContext(Dispatchers.Main) {
                        onProgress(progress.coerceIn(0f, 1f), downloadedMb, totalMb)
                    }
                }
            }

            outputStream.flush()
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK from $downloadUrl", e)
            Result.failure(e)
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (ignored: Exception) {}
        }
    }

    fun launchPackageInstaller(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "Cannot install: APK file does not exist or is empty")
            return false
        }

        return try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            false
        }
    }
}
