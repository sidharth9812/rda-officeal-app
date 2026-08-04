package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeMb: Double,
    val publishedAt: String = ""
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFileUri: Uri, val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

class GitHubUpdateManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val repoOwner = "sidharth9812"
    private val repoName = "rda-officeal-app"
    private val latestReleaseUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

    suspend fun checkForUpdate(): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(latestReleaseUrl)
                .header("User-Agent", "RDA-Academy-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        Log.i("GitHubUpdateManager", "No releases found on GitHub repo yet.")
                        return@withContext Result.success(null)
                    }
                    return@withContext Result.failure(Exception("GitHub API error code: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val json = JSONObject(bodyString)

                val tagName = json.optString("tag_name", "")
                val releaseTitle = json.optString("name", tagName)
                val releaseNotes = json.optString("body", "No release notes provided.")
                val publishedAt = json.optString("published_at", "")

                val cleanVersion = tagName.trimStart('v', 'V')
                val currentVersion = BuildConfig.VERSION_NAME

                val assets = json.optJSONArray("assets") ?: return@withContext Result.success(null)
                var apkUrl = ""
                var apkSizeBytes = 0L

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }

                if (apkUrl.isBlank()) {
                    Log.i("GitHubUpdateManager", "Latest release found but no .apk asset attached.")
                    return@withContext Result.success(null)
                }

                val isNewer = isVersionNewer(cleanVersion, currentVersion)
                if (isNewer) {
                    val sizeMb = if (apkSizeBytes > 0) apkSizeBytes.toDouble() / (1024 * 1024) else 0.0
                    val info = ReleaseInfo(
                        tagName = tagName,
                        versionName = cleanVersion,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        apkUrl = apkUrl,
                        apkSizeMb = sizeMb,
                        publishedAt = publishedAt
                    )
                    Result.success(info)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Failed to check update: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        versionName: String,
        onProgress: (UpdateDownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress(UpdateDownloadState.Downloading(0, 0, 0))

            val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val apkFile = File(downloadDir, "rda_academy_v${versionName}.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "RDA-Academy-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    onProgress(UpdateDownloadState.Error("HTTP error during download: ${response.code}"))
                    return@withContext
                }

                val body = response.body ?: run {
                    onProgress(UpdateDownloadState.Error("Download body was empty"))
                    return@withContext
                }

                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L

                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    val percent = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1
                    onProgress(UpdateDownloadState.Downloading(percent, bytesDownloaded, totalBytes))
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                onProgress(UpdateDownloadState.ReadyToInstall(apkUri, apkFile))
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Download error: ${e.message}", e)
            onProgress(UpdateDownloadState.Error(e.localizedMessage ?: "Download failed. Check network connection."))
        }
    }

    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun promptUnknownAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("GitHubUpdateManager", "Unable to open Unknown App Sources settings: ${e.message}")
            }
        }
    }

    fun installApk(apkUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Failed to launch package installer: ${e.message}", e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return latest != current
        }
    }
}
