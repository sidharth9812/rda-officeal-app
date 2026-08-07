package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
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

    private val prefs = context.getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
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
        private const val PREF_DISMISSED_VERSION = "dismissed_version"
    }

    private fun isVersionDismissed(version: String): Boolean {
        val cleanTarget = version.replace("v", "").replace("V", "").trim()
        val dismissed = prefs.getString(PREF_DISMISSED_VERSION, null) ?: return false
        val cleanDismissed = dismissed.replace("v", "").replace("V", "").trim()
        return cleanDismissed.equals(cleanTarget, ignoreCase = true)
    }

    private fun markVersionAsDismissed(version: String) {
        val cleanTarget = version.replace("v", "").replace("V", "").trim()
        prefs.edit().putString(PREF_DISMISSED_VERSION, cleanTarget).apply()
        Log.d(TAG, "Marked version $cleanTarget as dismissed")
    }

    suspend fun checkForUpdates(silent: Boolean = false) {
        _updateState.value = UpdateState.Idle
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
                val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.getExternalCacheDir()
                    ?: File(context.filesDir, "updates")

                val updateDir = try {
                    if (publicDownloadDir != null && (publicDownloadDir.exists() || publicDownloadDir.mkdirs())) {
                        publicDownloadDir
                    } else {
                        appDownloadDir
                    }
                } catch (_: Exception) {
                    appDownloadDir
                }
                updateDir.mkdirs()
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
                "com.miui.packageinstaller",
                "com.coloros.packageinstaller",
                "com.oppo.packageinstaller",
                "com.vivo.packageinstaller"
            )
            for (pkg in knownInstallers) {
                try {
                    context.grantUriPermission(pkg, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            val resolveInfos = context.packageManager.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
            for (resolveInfo in resolveInfos) {
                val pkgName = resolveInfo.activityInfo.packageName
                try {
                    context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install failed: ${e.message}", e)
            _updateState.value = UpdateState.Error("Failed to launch package installer: ${e.localizedMessage ?: e.message}. Please enable 'Install unknown apps' in Settings or try again.")
        }
    }

    fun uninstallApp() {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch uninstaller: ${e.message}", e)
            openAppSettings()
        }
    }

    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings: ${e.message}")
        }
    }

    fun dismissUpdate() {
        val currentState = _updateState.value
        if (currentState is UpdateState.UpdateAvailable) {
            markVersionAsDismissed(currentState.release.version)
        }
        _updateState.value = UpdateState.Idle
    }

    fun triggerFirestoreUpdate(config: com.example.model.AppUpdateConfig) {
        _updateState.value = UpdateState.Idle
    }
}

