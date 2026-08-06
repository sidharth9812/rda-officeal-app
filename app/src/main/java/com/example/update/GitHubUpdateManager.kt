package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val version: String,
    val releaseName: String,
    val body: String,
    val downloadUrl: String,
    val apkSizeFormatted: String,
    val publishedAt: String
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateState()
    data class Downloading(val progress: Int, val downloadedMb: String, val totalMb: String) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
    object UpToDate : UpdateState()
}

class GitHubUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/sidharth9812/rda-officeal-app/releases/latest"
        private const val TAG = "GitHubUpdateManager"
    }

    suspend fun checkForUpdates(silent: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                if (!silent) {
                    _updateState.value = UpdateState.Checking
                }

                val request = Request.Builder()
                    .url(GITHUB_API_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "RDA-App-Android")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub API status ${response.code}")
                    if (!silent) {
                        _updateState.value = UpdateState.UpToDate
                    }
                    return@withContext
                }

                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isBlank()) {
                    if (!silent) _updateState.value = UpdateState.UpToDate
                    return@withContext
                }

                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", "").trim()
                val name = json.optString("name", "New RDA Release").ifBlank { "New RDA Release" }
                val body = json.optString("body", "Bug fixes and performance improvements.").ifBlank { "Bug fixes and performance improvements." }
                val publishedAt = json.optString("published_at", "")

                // Find APK asset in assets array
                var apkUrl = ""
                var apkSizeBytes = 0L
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val downloadUrl = asset.optString("browser_download_url", "")
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk") || downloadUrl.endsWith(".apk") || assetName.contains("app-") || downloadUrl.contains(".apk")) {
                            apkUrl = downloadUrl
                            apkSizeBytes = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val cleanTag = tagName.removePrefix("v").removePrefix("V")
                if (apkUrl.isBlank() && tagName.isNotBlank()) {
                    apkUrl = "https://github.com/sidharth9812/rda-officeal-app/releases/download/$tagName/app-release.apk"
                }

                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(currentVersion, tagName)) {
                    val sizeMbStr = if (apkSizeBytes > 0) {
                        String.format("%.1f MB", apkSizeBytes / (1024.0 * 1024.0))
                    } else {
                        "Approx 15 MB"
                    }

                    val releaseInfo = GitHubReleaseInfo(
                        version = if (tagName.startsWith("v", ignoreCase = true)) tagName else "v$tagName",
                        releaseName = name,
                        body = body,
                        downloadUrl = apkUrl,
                        apkSizeFormatted = sizeMbStr,
                        publishedAt = publishedAt
                    )
                    _updateState.value = UpdateState.UpdateAvailable(releaseInfo)
                } else {
                    if (!silent) {
                        _updateState.value = UpdateState.UpToDate
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Check update error: ${e.message}", e)
                if (!silent) {
                    _updateState.value = UpdateState.Error(e.localizedMessage ?: "Failed to check updates")
                }
            }
        }
    }

    private fun isNewerVersion(currentVersion: String, latestTag: String): Boolean {
        try {
            val cleanCurrent = currentVersion.replace("v", "").replace("V", "").trim()
            val cleanLatest = latestTag.replace("v", "").replace("V", "").trim()

            if (cleanCurrent == cleanLatest) return false

            val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

            val length = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until length) {
                val currentPart = currentParts.getOrElse(i) { 0 }
                val latestPart = latestParts.getOrElse(i) { 0 }

                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Version comparison error: ${e.message}")
        }
        return false
    }

    private fun isApkFileValid(file: File): Boolean {
        if (!file.exists() || file.length() < 50_000) return false
        return try {
            java.util.zip.ZipFile(file).use { zip ->
                zip.getEntry("AndroidManifest.xml") != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error validating APK zip file: ${e.message}")
            false
        }
    }

    private fun getCandidateUrls(release: GitHubReleaseInfo): List<String> {
        val candidates = mutableListOf<String>()

        val primaryUrl = release.downloadUrl.trim()
        if (primaryUrl.isNotBlank() && !primaryUrl.contains("/releases/tag/")) {
            candidates.add(primaryUrl)
        }

        val rawTag = release.version.trim()
        val cleanTag = rawTag.removePrefix("v").removePrefix("V")

        val tagsToTry = listOf(rawTag, "v$cleanTag", cleanTag).distinct().filter { it.isNotBlank() }
        val filenamesToTry = listOf(
            "app-release.apk",
            "app-release-unsigned.apk",
            "rda_update.apk",
            "app-debug.apk",
            "RDA.apk"
        )

        for (t in tagsToTry) {
            for (fn in filenamesToTry) {
                val constructed = "https://github.com/sidharth9812/rda-officeal-app/releases/download/$t/$fn"
                if (!candidates.contains(constructed)) {
                    candidates.add(constructed)
                }
            }
        }

        if (primaryUrl.isNotBlank() && primaryUrl.contains("/releases/tag/")) {
            candidates.add(primaryUrl)
        }

        return candidates
    }

    suspend fun downloadAndInstallApk(release: GitHubReleaseInfo) {
        withContext(Dispatchers.IO) {
            try {
                _updateState.value = UpdateState.Downloading(0, "0 MB", release.apkSizeFormatted)

                val candidates = getCandidateUrls(release)
                val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updateDir, "rda_update.apk")

                var downloadSuccess = false
                var lastErrorMessage = ""

                for (url in candidates) {
                    Log.d(TAG, "Attempting update download from: $url")
                    if (apkFile.exists()) apkFile.delete()

                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "RDA-App-Android")
                        .header("Accept", "application/octet-stream, */*")
                        .build()

                    try {
                        val response = client.newCall(request).execute()
                        val body = response.body
                        if (!response.isSuccessful || body == null) {
                            Log.w(TAG, "Candidate $url failed with code ${response.code}")
                            response.close()
                            continue
                        }

                        val contentType = response.header("Content-Type", "") ?: ""
                        if (contentType.contains("text/html")) {
                            Log.w(TAG, "Candidate $url returned web page instead of APK stream")
                            response.close()
                            continue
                        }

                        val contentLength = body.contentLength()
                        val inputStream: InputStream = body.byteStream()
                        val outputStream = FileOutputStream(apkFile)

                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val progress = if (contentLength > 0) {
                                ((totalBytesRead * 100) / contentLength).toInt()
                            } else {
                                50
                            }

                            val downloadedMb = String.format("%.1f MB", totalBytesRead / (1024.0 * 1024.0))
                            val totalMb = if (contentLength > 0) String.format("%.1f MB", contentLength / (1024.0 * 1024.0)) else release.apkSizeFormatted

                            _updateState.value = UpdateState.Downloading(progress, downloadedMb, totalMb)
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        response.close()

                        apkFile.setReadable(true, false)

                        if (isApkFileValid(apkFile)) {
                            downloadSuccess = true
                            break
                        } else {
                            Log.w(TAG, "File downloaded from $url is not a valid APK archive")
                            if (apkFile.exists()) apkFile.delete()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed downloading from $url: ${e.message}")
                        lastErrorMessage = e.localizedMessage ?: e.message ?: "Network error"
                    }
                }

                if (!downloadSuccess) {
                    Log.e(TAG, "All candidate URLs failed to download a valid APK")
                    _updateState.value = UpdateState.Error(
                        if (lastErrorMessage.isNotBlank()) "Download failed: $lastErrorMessage" else "Failed to download update file directly in app. Please verify release APK asset is attached on GitHub."
                    )
                    return@withContext
                }

                _updateState.value = UpdateState.ReadyToInstall(apkFile)

                withContext(Dispatchers.Main) {
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download and install error: ${e.message}", e)
                _updateState.value = UpdateState.Error("Update process error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun launchBrowserDownload(url: String) {
        try {
            var validUrl = url.trim()
            if (validUrl.isBlank() || validUrl.contains("/releases/latest")) {
                validUrl = "https://github.com/sidharth9812/rda-officeal-app"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening release page in browser...", Toast.LENGTH_LONG).show()
            _updateState.value = UpdateState.Idle
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Unable to open browser download link: ${e.message}")
        }
    }

    fun installApk(apkFile: File) {
        try {
            if (!apkFile.exists()) {
                _updateState.value = UpdateState.Error("Update APK file not found. Please try updating again.")
                return
            }

            apkFile.setReadable(true, false)

            if (!isApkFileValid(apkFile)) {
                _updateState.value = UpdateState.Error("Downloaded file is incomplete or invalid APK.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please enable 'Install unknown apps' permission for RDA App, then tap Install.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Grant URI permission explicitly to system package installers
            val knownInstallers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller",
                "com.samsung.android.packageinstaller",
                "com.miui.packageinstaller"
            )
            for (pkg in knownInstallers) {
                try {
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            val resolveInfos = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(intent)
            _updateState.value = UpdateState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "Install failed: ${e.message}", e)
            _updateState.value = UpdateState.Error("Failed to launch package installer: ${e.localizedMessage ?: e.message}")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateState.Idle
    }

    fun triggerFirestoreUpdate(config: com.example.model.AppUpdateConfig) {
        if (!config.active) return
        val currentVersion = BuildConfig.VERSION_NAME
        val currentCode = BuildConfig.VERSION_CODE
        val isNewer = config.versionCode > currentCode || isNewerVersion(currentVersion, config.versionName)
        if (isNewer || config.isMandatory) {
            val releaseInfo = GitHubReleaseInfo(
                version = "v${config.versionName}",
                releaseName = config.title,
                body = config.releaseNotes,
                downloadUrl = config.downloadUrl.ifBlank { "https://github.com/sidharth9812/rda-officeal-app" },
                apkSizeFormatted = "Direct Update Push",
                publishedAt = ""
            )
            _updateState.value = UpdateState.UpdateAvailable(releaseInfo)
        }
    }
}

