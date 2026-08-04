package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.update.GitHubUpdateManager
import com.example.update.ReleaseInfo
import com.example.update.UpdateDownloadState
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    releaseInfo: ReleaseInfo,
    updateManager: GitHubUpdateManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var needsInstallPermission by remember { mutableStateOf(!updateManager.canInstallUnknownApps()) }

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BentoPurpleCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = BentoPurpleOn,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NEW UPDATE AVAILABLE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Version ${releaseInfo.versionName} is ready to install",
                        fontSize = 12.sp,
                        color = BentoPurpleOn,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Version Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBackground),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyberBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Version", fontSize = 11.sp, color = TextSecondary)
                            Text("v${BuildConfig.VERSION_NAME}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BentoNavy, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Latest Version", fontSize = 11.sp, color = TextSecondary)
                            Text("v${releaseInfo.versionName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                        }
                        Column {
                            Text("APK Size", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                if (releaseInfo.apkSizeMb > 0) String.format("%.1f MB", releaseInfo.apkSizeMb) else "Standard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // What's New Section
                Text(
                    text = "WHAT'S NEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoNavy,
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = releaseInfo.releaseNotes.ifBlank { "Performance improvements and bug fixes." },
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Unknown Apps Permission Warning
                if (needsInstallPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "App Install Permission Required",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Text(
                                    "Enable 'Install Unknown Apps' setting to complete in-app update.",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            TextButton(
                                onClick = {
                                    updateManager.promptUnknownAppsPermission()
                                    needsInstallPermission = false
                                }
                            ) {
                                Text("ALLOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                        }
                    }
                }

                // Download Progress
                when (val state = downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (state.progressPercent >= 0) "Downloading update... ${state.progressPercent}%" else "Downloading update...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPurpleOn
                                )
                                if (state.totalBytes > 0) {
                                    val downloadedMb = state.bytesDownloaded.toDouble() / (1024 * 1024)
                                    val totalMb = state.totalBytes.toDouble() / (1024 * 1024)
                                    Text(
                                        text = String.format("%.1f / %.1f MB", downloadedMb, totalMb),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { if (state.progressPercent >= 0) state.progressPercent / 100f else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100.dp)),
                                color = BentoPurpleOn,
                                trackColor = CyberBorder
                            )
                        }
                    }

                    is UpdateDownloadState.ReadyToInstall -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BentoMintCard)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoMintOn, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Download Complete! Launching Package Installer...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoMintOn
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Download Error", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    Text(state.message, fontSize = 10.sp, color = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }

                    UpdateDownloadState.Idle -> {}
                }
            }
        },
        confirmButton = {
            when (val state = downloadState) {
                is UpdateDownloadState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BentoPurpleOn,
                        strokeWidth = 2.dp
                    )
                }

                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            updateManager.installApk(state.apkFileUri)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoMintOn),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("INSTALL NOW", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            if (!updateManager.canInstallUnknownApps()) {
                                updateManager.promptUnknownAppsPermission()
                                needsInstallPermission = true
                            }
                            scope.launch {
                                updateManager.downloadApk(releaseInfo.apkUrl, releaseInfo.versionName) { newState ->
                                    downloadState = newState
                                    if (newState is UpdateDownloadState.ReadyToInstall) {
                                        updateManager.installApk(newState.apkFileUri)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (downloadState is UpdateDownloadState.Error) "RETRY UPDATE" else "UPDATE NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text("LATER", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
