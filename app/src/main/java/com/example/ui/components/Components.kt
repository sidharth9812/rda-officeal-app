package com.example.ui.components

import android.content.Intent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.example.util.CloudinaryUploader
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.DeveloperInfo
import com.example.model.AttendanceStatus
import com.example.model.UserRole
import com.example.repository.AcademyRepository
import com.example.ui.theme.*

@Composable
fun CyberHeaderCard(
    title: String,
    subtitle: String,
    role: UserRole,
    photoUrl: String = "",
    isOffline: Boolean = false,
    onLogoutClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        photoUrl = photoUrl,
                        size = 46.dp,
                        iconSize = 24.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RoleBadge(role = role)
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOffline) NeonRed else BentoMintOn)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOffline) "OFFLINE CACHE" else subtitle,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: UserRole) {
    val (label, bgColor, textColor) = when (role) {
        UserRole.ADMIN -> Triple("ADMIN", BentoCoralCard, BentoCoralOn)
        UserRole.GROUP_LEADER -> Triple("LEADER", BentoPurpleCard, BentoPurpleOn)
        UserRole.STUDENT -> Triple("STUDENT", BentoBlueCard, BentoNavy)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, contentColor) = when (accentColor) {
        NeonCyan -> BentoBlueCard to BentoNavy
        ElectricPurple -> BentoPurpleCard to BentoPurpleOn
        NeonGreen -> BentoMintCard to BentoMintOn
        NeonRed -> BentoCoralCard to BentoCoralOn
        NeonYellow -> BentoYellowCard to BentoYellowOn
        else -> CyberSurfaceVariant to TextPrimary
    }

    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onClick != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View Details",
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
fun AttendanceBadge(status: AttendanceStatus) {
    val isPresent = status == AttendanceStatus.PRESENT
    val bgColor = if (isPresent) BentoMintCard else BentoCoralCard
    val textColor = if (isPresent) BentoMintOn else BentoCoralOn
    val text = if (isPresent) "PRESENT" else "ABSENT"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun BatchBadge(text: String, color: Color = NeonCyan) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(CyberSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
fun StatusDotBadge(
    status: String,
    activeLabel: String = "ACTIVE",
    closedLabel: String = "CLOSED",
    modifier: Modifier = Modifier
) {
    val isActive = status.equals("ACTIVE", ignoreCase = true)
    val dotColor = if (isActive) Color(0xFF00E676) else Color(0xFFFF5252)
    val bgColor = if (isActive) BentoMintCard else BentoCoralCard
    val textColor = if (isActive) BentoMintOn else BentoCoralOn
    val labelText = if (isActive) activeLabel else closedLabel

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = labelText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = BentoNavy,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = CyberSurfaceVariant,
            disabledContentColor = TextMuted
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProfileAvatar(
    photoUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 30.dp,
    showEditOverlay: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = onEditClick != null) { onEditClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("data:image") || photoUrl.startsWith("content://") || photoUrl.startsWith("file://")) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val (bgColor, iconColor, icon) = when (photoUrl) {
                "preset_cadet", "avatar_cadet" -> Triple(BentoBlueCard, BentoNavy, Icons.Default.MilitaryTech)
                "preset_leader", "avatar_leader" -> Triple(BentoPurpleCard, BentoPurpleOn, Icons.Default.Shield)
                "preset_fitness", "avatar_fitness" -> Triple(BentoCoralCard, BentoCoralOn, Icons.Default.FitnessCenter)
                "preset_gold", "avatar_gold" -> Triple(BentoBlueCard, BentoNavy, Icons.Default.EmojiEvents)
                "preset_badge", "avatar_badge" -> Triple(BentoMintCard, BentoMintOn, Icons.Default.Badge)
                else -> Triple(BentoMintCard, BentoMintOn, Icons.Default.DirectionsRun)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (showEditOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Edit Photo",
                    tint = Color.White,
                    modifier = Modifier.size(size / 2.5f)
                )
            }
        }
    }
}

private fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (bitmap == null) return null

        val maxDim = 512
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val newH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private data class PresetAvatarItem(val id: String, val label: String, val icon: ImageVector, val containerColor: Color, val contentColor: Color)

@Composable
fun AvatarPickerDialog(
    currentPhotoUrl: String,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customUrl by remember { mutableStateOf(if (currentPhotoUrl.startsWith("http")) currentPhotoUrl else "") }
    var galleryPhotoUrl by remember { mutableStateOf(if (currentPhotoUrl.startsWith("http")) currentPhotoUrl else "") }
    var selectedPreset by remember { mutableStateOf(if (!currentPhotoUrl.startsWith("http") && currentPhotoUrl.isNotBlank()) currentPhotoUrl else "preset_runner") }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun startCloudinaryUpload(uri: Uri) {
        selectedUri = uri
        isUploading = true
        uploadError = null
        uploadProgress = 0.1f
        scope.launch {
            val result = CloudinaryUploader.uploadImage(context, uri) { progress ->
                uploadProgress = progress
            }
            isUploading = false
            if (result.isSuccess) {
                galleryPhotoUrl = result.getOrNull() ?: ""
                customUrl = ""
            } else {
                uploadError = result.exceptionOrNull()?.message ?: "Cloudinary Upload Failed"
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            startCloudinaryUpload(uri)
        }
    }

    val presets = listOf(
        PresetAvatarItem("preset_runner", "Runner / Athlete", Icons.Default.DirectionsRun, BentoMintCard, BentoMintOn),
        PresetAvatarItem("preset_cadet", "Academy Cadet", Icons.Default.MilitaryTech, BentoBlueCard, BentoNavy),
        PresetAvatarItem("preset_leader", "Group Commander", Icons.Default.Shield, BentoPurpleCard, BentoPurpleOn),
        PresetAvatarItem("preset_fitness", "Fitness Lead", Icons.Default.FitnessCenter, BentoCoralCard, BentoCoralOn),
        PresetAvatarItem("preset_gold", "Champion", Icons.Default.EmojiEvents, BentoBlueCard, BentoNavy),
        PresetAvatarItem("preset_badge", "Tactical Badge", Icons.Default.Badge, BentoMintCard, BentoMintOn)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BentoNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Profile Picture", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Device Gallery Upload to Cloudinary
                Text("1. UPLOAD TO CLOUDINARY (DEVICE GALLERY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurfaceVariant)
                        .border(
                            width = if (galleryPhotoUrl.startsWith("http")) 2.dp else 1.dp,
                            color = if (galleryPhotoUrl.startsWith("http")) BentoMintOn else CyberBorder,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    when {
                        isUploading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = BentoNavy, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Uploading image to Cloudinary... ${(uploadProgress * 100).toInt()}%", fontSize = 11.sp, color = BentoNavy, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = BentoNavy,
                                    trackColor = CyberBorder
                                )
                            }
                        }
                        galleryPhotoUrl.startsWith("http") -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProfileAvatar(
                                        photoUrl = galleryPhotoUrl,
                                        size = 48.dp,
                                        iconSize = 24.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoMintOn, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Uploaded to Cloudinary", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoMintOn)
                                        }
                                        Text("HTTPS Secure URL Ready", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Replace", tint = BentoNavy, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { galleryPhotoUrl = "" },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = NeonRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        uploadError != null -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(uploadError ?: "Upload failed", fontSize = 11.sp, color = NeonRed, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { selectedUri?.let { startCloudinaryUpload(it) } ?: galleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RETRY UPLOAD", fontSize = 11.sp)
                                }
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoNavy),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(BentoNavy, BentoPurpleOn)))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CHOOSE FROM GALLERY & UPLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = CyberBorder)

                // Section 2: Choose Preset Avatar
                Text("2. CHOOSE ACADEMY AVATAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presets.take(3).forEach { item ->
                        val isSelected = (selectedPreset == item.id && galleryPhotoUrl.isBlank() && customUrl.isBlank())
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) BentoNavy else CyberBorder,
                                    shape = CircleShape
                                )
                                .background(item.containerColor)
                                .clickable {
                                    selectedPreset = item.id
                                    galleryPhotoUrl = ""
                                    customUrl = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = item.contentColor, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presets.drop(3).forEach { item ->
                        val isSelected = (selectedPreset == item.id && galleryPhotoUrl.isBlank() && customUrl.isBlank())
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) BentoNavy else CyberBorder,
                                    shape = CircleShape
                                )
                                .background(item.containerColor)
                                .clickable {
                                    selectedPreset = item.id
                                    galleryPhotoUrl = ""
                                    customUrl = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = item.label, tint = item.contentColor, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Divider(color = CyberBorder)

                // Section 3: Paste Direct Image Link (URL)
                Text("3. OR PASTE IMAGE LINK (URL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        if (it.isNotBlank()) {
                            galleryPhotoUrl = ""
                        }
                    },
                    placeholder = { Text("https://res.cloudinary.com/...", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoNavy,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = when {
                        galleryPhotoUrl.isNotBlank() -> galleryPhotoUrl
                        customUrl.trim().isNotBlank() -> customUrl.trim()
                        else -> selectedPreset
                    }
                    onAvatarSelected(finalUrl)
                    onDismiss()
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BentoNavy)
            ) {
                Text("SAVE PICTURE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun CloudinaryMediaUploadDialog(
    title: String,
    categoryOptions: List<String> = emptyList(),
    onUploadSuccess: (title: String, category: String, cloudinaryUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mediaTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categoryOptions.firstOrNull() ?: "General") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var cloudinaryUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun startUpload(uri: Uri) {
        selectedUri = uri
        isUploading = true
        uploadError = null
        uploadProgress = 0.1f
        scope.launch {
            val result = CloudinaryUploader.uploadImage(context, uri) { progress ->
                uploadProgress = progress
            }
            isUploading = false
            if (result.isSuccess) {
                cloudinaryUrl = result.getOrNull() ?: ""
            } else {
                uploadError = result.exceptionOrNull()?.message ?: "Cloudinary Upload Failed"
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            startUpload(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BentoNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = mediaTitle,
                    onValueChange = { mediaTitle = it },
                    label = { Text("Title / Description", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoNavy,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (categoryOptions.isNotEmpty()) {
                    Text("SELECT CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryOptions.forEach { cat ->
                            val isSel = selectedCategory == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Text("IMAGE FILE (CLOUDINARY STORAGE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberSurfaceVariant)
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BentoNavy, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Uploading to Cloudinary... ${(uploadProgress * 100).toInt()}%", fontSize = 12.sp, color = BentoNavy, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = BentoNavy,
                                    trackColor = CyberBorder
                                )
                            }
                        }
                        cloudinaryUrl.isNotBlank() -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp, 90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BentoMintOn, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = cloudinaryUrl,
                                        contentDescription = "Cloudinary Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoMintOn, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Uploaded to Cloudinary!", fontSize = 11.sp, color = BentoMintOn, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { launcher.launch("image/*") },
                                    modifier = Modifier.padding(top = 6.dp),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("Replace Image", fontSize = 10.sp)
                                }
                            }
                        }
                        uploadError != null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = NeonRed, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uploadError ?: "Upload error", fontSize = 11.sp, color = NeonRed, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedUri?.let { startUpload(it) } ?: launcher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("RETRY UPLOAD", fontSize = 11.sp)
                                }
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SELECT IMAGE FROM GALLERY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mediaTitle.isBlank()) {
                        Toast.makeText(context, "Please enter a title.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cloudinaryUrl.isBlank()) {
                        Toast.makeText(context, "Please select and upload an image.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onUploadSuccess(mediaTitle, selectedCategory, cloudinaryUrl)
                    onDismiss()
                },
                enabled = cloudinaryUrl.isNotBlank() && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BentoNavy)
            ) {
                Text("SAVE & PUBLISH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun DeveloperCard(
    developerInfo: DeveloperInfo = DeveloperInfo(),
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val phone = developerInfo.phone.ifBlank { "7441197419" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVELOPER & SUPPORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoNavy,
                    letterSpacing = 1.sp
                )
                if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Developer Info",
                            tint = BentoNavy,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Larger Profile Picture Size (110.dp)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceVariant)
                    .border(3.dp, BentoNavy, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val photoModel: Any = if (developerInfo.photoUrl.startsWith("http://") || developerInfo.photoUrl.startsWith("https://") || developerInfo.photoUrl.startsWith("content://") || developerInfo.photoUrl.startsWith("file://")) {
                    developerInfo.photoUrl
                } else {
                    R.drawable.dev_photo
                }
                AsyncImage(
                    model = photoModel,
                    contentDescription = "Developer ${developerInfo.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = developerInfo.name.ifBlank { "Sidharth Malviya" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = developerInfo.roleTitle.ifBlank { "App Developer & Technical Lead" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone",
                    tint = BentoNavy,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+91 $phone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoNavy
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Call, WhatsApp & Share APK
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Call action unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(14.dp), tint = BentoNavy)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, color = BentoNavy, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=91$phone&text=Hello%20Developer%20Sidharth"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "RDA Physical Academy App")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "📱 *RDA Physical Academy Official App*\n\nGet attendance, physical tests, batch status & certificates!\n\nDeveloper: ${developerInfo.name}\nContact: +91 $phone\n\nDownload App / APK:\nhttps://github.com/sidharth9812/rda-officeal-app/releases/latest"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share RDA Physical Academy App"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Sharing failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share APK", modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share APK", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminEditDeveloperDialog(
    currentInfo: DeveloperInfo,
    repository: AcademyRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(currentInfo.name.ifBlank { "Sidharth Malviya" }) }
    var roleTitle by remember { mutableStateOf(currentInfo.roleTitle.ifBlank { "App Developer & Technical Lead" }) }
    var phone by remember { mutableStateOf(currentInfo.phone.ifBlank { "7441197419" }) }
    var photoUrl by remember { mutableStateOf(currentInfo.photoUrl) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                val res = CloudinaryUploader.uploadImage(context, uri) { pct ->
                    uploadProgress = pct
                }
                isUploading = false
                res.fold(
                    onSuccess = { url ->
                        photoUrl = url
                        Toast.makeText(context, "Photo uploaded to Cloudinary successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "Cloudinary Upload Failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UPDATE DEVELOPER CARD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Larger profile picture edit preview (110.dp)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceVariant)
                        .border(3.dp, BentoNavy, CircleShape)
                        .clickable { if (!isUploading) photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val photoModel: Any = if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("content://") || photoUrl.startsWith("file://")) {
                        photoUrl
                    } else {
                        R.drawable.dev_photo
                    }
                    AsyncImage(
                        model = photoModel,
                        contentDescription = "Developer Photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White)
                    }
                }

                if (isUploading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploading photo to Cloudinary...", fontSize = 11.sp, color = BentoNavy)
                    }
                } else {
                    Text("Tap photo to select & upload to Cloudinary", fontSize = 11.sp, color = TextSecondary)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Developer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it },
                    label = { Text("Role Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone / WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Please enter developer name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val info = DeveloperInfo(
                        name = name.trim(),
                        photoUrl = photoUrl,
                        roleTitle = roleTitle.trim(),
                        phone = phone.trim().ifBlank { "7441197419" }
                    )
                    repository.updateDeveloperInfo(info) { success, err ->
                        if (success) {
                            Toast.makeText(context, "Developer card updated & synced via Firestore!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = BentoNavy)
            ) {
                Text("SAVE DEVELOPER CARD", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}


