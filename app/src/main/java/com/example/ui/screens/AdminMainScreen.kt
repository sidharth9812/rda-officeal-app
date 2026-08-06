package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.AuthViewModel
import com.example.model.*
import com.example.pdf.PdfReportGenerator
import com.example.repository.AcademyRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AdminMainScreen(
    user: User,
    authViewModel: AuthViewModel,
    repository: AcademyRepository
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: DASHBOARD, 1: BATCHES, 2: GROUPS, 3: STUDENTS, 4: ATTENDANCE, 5: NOTICES, 6: REPORTS

    val batches by repository.batches.collectAsState()
    val groups by repository.groups.collectAsState()
    val students by repository.students.collectAsState()
    val attendanceList by repository.attendance.collectAsState()
    val noticesList by repository.notices.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val developerInfo by repository.developerInfo.collectAsState()
    var showEditDeveloperDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                CyberHeaderCard(
                    title = "Academy Admin Panel",
                    subtitle = "RDA Physical Training System",
                    role = UserRole.ADMIN,
                    photoUrl = user.photoUrl,
                    isOffline = isOffline,
                    onLogoutClick = { authViewModel.logout() }
                )
            }
        },
        bottomBar = {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberSurface,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BentoCoralOn
                    )
                },
                modifier = Modifier
                    .border(1.dp, CyberBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                listOf(
                    "DASHBOARD" to Icons.Default.Dashboard,
                    "BATCHES" to Icons.Default.Folder,
                    "GROUPS" to Icons.Default.Groups,
                    "STUDENTS" to Icons.Default.People,
                    "ATTENDANCE" to Icons.Default.Event,
                    "NOTICES" to Icons.Default.Campaign,
                    "REPORTS" to Icons.Default.PictureAsPdf
                ).forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) },
                        selectedContentColor = BentoCoralOn,
                        unselectedContentColor = TextSecondary
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> AdminDashboardContent(
                    batches = batches,
                    groups = groups,
                    students = students,
                    attendanceList = attendanceList,
                    noticesList = noticesList,
                    developerInfo = developerInfo,
                    repository = repository,
                    onEditDeveloper = { showEditDeveloperDialog = true },
                    onNavigateTab = { selectedTab = it }
                )
                1 -> AdminBatchesContent(batches = batches, groups = groups, students = students, repository = repository)
                2 -> AdminGroupsContent(batches = batches, groups = groups, students = students, repository = repository)
                3 -> AdminStudentsContent(batches = batches, groups = groups, students = students, attendanceList = attendanceList, repository = repository)
                4 -> AdminAttendanceContent(batches = batches, groups = groups, students = students, attendanceList = attendanceList, user = user, repository = repository)
                5 -> AdminNoticesContent(batches = batches, groups = groups, noticesList = noticesList, user = user, repository = repository)
                6 -> AdminReportsContent(batches = batches, groups = groups, students = students, attendanceList = attendanceList)
            }
        }

        if (showEditDeveloperDialog) {
            AdminEditDeveloperDialog(
                currentInfo = developerInfo,
                repository = repository,
                onDismiss = { showEditDeveloperDialog = false }
            )
        }
    }
}

@Composable
fun AdminDashboardContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    attendanceList: List<AttendanceRecord>,
    noticesList: List<Notice> = emptyList(),
    developerInfo: DeveloperInfo = DeveloperInfo(),
    repository: AcademyRepository,
    onEditDeveloper: () -> Unit = {},
    onNavigateTab: (Int) -> Unit
) {
    val activeStudents = students.count { it.status == "ACTIVE" }
    val activeBatches = batches.count { it.status == "ACTIVE" }

    val appUpdateConfig by repository.appUpdateConfig.collectAsState()
    var showPushUpdateDialog by remember { mutableStateOf(false) }

    val today = DateUtils.getTodayString()
    val todayRecords = attendanceList.filter { it.date == today }
    val todayPresent = todayRecords.count { it.status == AttendanceStatus.PRESENT }
    val todayAbsent = todayRecords.count { it.status == AttendanceStatus.ABSENT }
    val todayTotal = todayRecords.size
    val todayPercentage = if (todayTotal > 0) (todayPresent.toFloat() / todayTotal * 100).toInt() else 0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text("ACADEMY KEY METRICS (CLICK TO VIEW)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("ACTIVE STUDENTS", "$activeStudents", Icons.Default.People, NeonCyan, Modifier.weight(1f), onClick = { onNavigateTab(3) })
                        StatBox("ACTIVE BATCHES", "$activeBatches", Icons.Default.Folder, ElectricPurple, Modifier.weight(1f), onClick = { onNavigateTab(1) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("GROUPS", "${groups.size}", Icons.Default.Groups, NeonPink, Modifier.weight(1f), onClick = { onNavigateTab(2) })
                        StatBox("TODAY PRESENT", "$todayPresent", Icons.Default.CheckCircle, NeonGreen, Modifier.weight(1f), onClick = { onNavigateTab(4) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("TODAY ABSENT", "$todayAbsent", Icons.Default.Cancel, NeonRed, Modifier.weight(1f), onClick = { onNavigateTab(4) })
                        StatBox("ATTENDANCE RATE", "$todayPercentage%", Icons.Default.PieChart, BentoYellowOn, Modifier.weight(1f), onClick = { onNavigateTab(4) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("NOTICES", "${noticesList.size}", Icons.Default.Campaign, BentoPurpleOn, Modifier.weight(1f), onClick = { onNavigateTab(5) })
                        StatBox("REPORTS", "PDF Export", Icons.Default.PictureAsPdf, BentoCoralOn, Modifier.weight(1f), onClick = { onNavigateTab(6) })
                    }
                }
            }

            item {
                AdminPushUpdateCard(
                    currentConfig = appUpdateConfig,
                    onOpenPushDialog = { showPushUpdateDialog = true }
                )
            }

            item {
                Text("QUICK MANAGEMENT ACTIONS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberButton("ADD STUDENT", { onNavigateTab(3) }, Modifier.weight(1f), Icons.Default.PersonAdd, containerColor = BentoNavy, contentColor = Color.White)
                    CyberButton("CREATE BATCH", { onNavigateTab(1) }, Modifier.weight(1f), Icons.Default.CreateNewFolder, containerColor = BentoPurpleOn, contentColor = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberButton("MARK ATTENDANCE", { onNavigateTab(4) }, Modifier.weight(1f), Icons.Default.Event, containerColor = BentoMintOn, contentColor = Color.White)
                    CyberButton("GENERATE PDF", { onNavigateTab(6) }, Modifier.weight(1f), Icons.Default.PictureAsPdf, containerColor = BentoCoralOn, contentColor = Color.White)
                }
            }

            item {
                DeveloperCard(
                    developerInfo = developerInfo,
                    onEditClick = onEditDeveloper
                )
            }

            item {
                Text("ACTIVE TRAINING BATCHES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            items(batches.filter { it.status == "ACTIVE" }) { batch ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp))
                        .clickable { onNavigateTab(1) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(batch.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                            BatchBadge(batch.type, BentoPurpleOn)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Schedule: ${batch.schedule}", fontSize = 12.sp, color = TextSecondary)
                        Text("Start: ${batch.startDate} @ ${batch.startTime}", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        if (showPushUpdateDialog) {
            AdminPushUpdateDialog(
                currentConfig = appUpdateConfig,
                repository = repository,
                onDismiss = { showPushUpdateDialog = false }
            )
        }
    }
}

@Composable
fun AdminPushUpdateCard(
    currentConfig: AppUpdateConfig?,
    onOpenPushDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Push Updates",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DIRECT APP UPDATE BROADCAST",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Installed App: v${com.example.BuildConfig.VERSION_NAME} (${com.example.BuildConfig.VERSION_CODE})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = if (currentConfig?.active == true) NeonGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (currentConfig?.active == true) "PUSHED LIVE" else "IDLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentConfig?.active == true) NeonGreen else TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (currentConfig != null && currentConfig.active) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBackground)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active Push Target: v${currentConfig.versionName} (Code ${currentConfig.versionCode})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            if (currentConfig.isMandatory) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = NeonRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "FORCED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentConfig.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = currentConfig.releaseNotes,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenPushDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PUSH NEW APP UPDATE TO ALL DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun AdminPushUpdateDialog(
    currentConfig: AppUpdateConfig?,
    repository: AcademyRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var versionName by remember { mutableStateOf(currentConfig?.versionName ?: com.example.BuildConfig.VERSION_NAME) }
    var versionCodeStr by remember { mutableStateOf((currentConfig?.versionCode ?: (com.example.BuildConfig.VERSION_CODE + 1)).toString()) }
    var title by remember { mutableStateOf(currentConfig?.title ?: "New App Update & Features Available") }
    var releaseNotes by remember { mutableStateOf(currentConfig?.releaseNotes ?: "• Added direct update push system\n• Instant real-time Firestore sync across devices\n• Performance optimizations and bug fixes") }
    var downloadUrl by remember { mutableStateOf(currentConfig?.downloadUrl ?: "https://github.com/sidharth9812/rda-officeal-app/releases/latest") }
    var isMandatory by remember { mutableStateOf(currentConfig?.isMandatory ?: false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
                .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Push App Update to Devices", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Broadcast an app update to all registered student and instructor devices via Firestore in real time.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            placeholder = { Text("1.1.0") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version Code", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = versionCodeStr,
                            onValueChange = { versionCodeStr = it.filter { char -> char.isDigit() } },
                            placeholder = { Text("2") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Update Headline / Title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Major Feature & Bug Fix Update") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Release Notes / What's New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    placeholder = { Text("Describe changes and new features...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("APK Download / Share Link", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = downloadUrl,
                    onValueChange = { downloadUrl = it },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Mandatory / Forced Update", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Require students to update immediately", fontSize = 10.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isMandatory,
                        onCheckedChange = { isMandatory = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentConfig?.active == true) {
                        OutlinedButton(
                            onClick = {
                                isSubmitting = true
                                val deactivated = currentConfig.copy(active = false, updatedAt = System.currentTimeMillis())
                                repository.pushAppUpdate(deactivated) { success, err ->
                                    isSubmitting = false
                                    if (success) {
                                        Toast.makeText(context, "Update alert deactivated.", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        ) {
                            Text("Deactivate Alert", fontSize = 11.sp, color = NeonRed)
                        }
                    }

                    Button(
                        onClick = {
                            val code = versionCodeStr.toIntOrNull() ?: (com.example.BuildConfig.VERSION_CODE + 1)
                            if (versionName.isBlank()) {
                                Toast.makeText(context, "Please enter version name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSubmitting = true
                            val config = AppUpdateConfig(
                                configId = "latest",
                                versionCode = code,
                                versionName = versionName.trim(),
                                title = title.trim().ifBlank { "New App Update Available" },
                                releaseNotes = releaseNotes.trim().ifBlank { "Features updated and bug fixes applied." },
                                downloadUrl = downloadUrl.trim().ifBlank { "https://github.com/sidharth9812/rda-officeal-app/releases/latest" },
                                isMandatory = isMandatory,
                                active = true,
                                pushedByAdmin = "Academy Admin",
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.pushAppUpdate(config) { success, err ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "Update pushed to all devices via Firestore!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Failed to push update: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("PUSH LIVE UPDATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AdminBatchesContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    repository: AcademyRepository
) {
    val context = LocalContext.current

    // State Dialogs for Batches
    var showCreateBatchDialog by remember { mutableStateOf(false) }
    var batchToEdit by remember { mutableStateOf<Batch?>(null) }
    var batchToDelete by remember { mutableStateOf<Batch?>(null) }
    var batchToManage by remember { mutableStateOf<Batch?>(null) }

    // State Dialogs for Groups & Students inside Batches
    var showCreateGroupForBatch by remember { mutableStateOf<Batch?>(null) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var groupLeaderToAssign by remember { mutableStateOf<Group?>(null) }
    var groupForDetailsInBatch by remember { mutableStateOf<Group?>(null) }

    var showAddStudentsToBatch by remember { mutableStateOf<Batch?>(null) }
    var studentToMoveGroup by remember { mutableStateOf<Student?>(null) }
    var studentToRemoveFromBatch by remember { mutableStateOf<Student?>(null) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BATCH MANAGEMENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${batches.size} Total Batches • Realtime Firestore Sync", fontSize = 11.sp, color = TextSecondary)
                }
                Button(
                    onClick = { showCreateBatchDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CREATE BATCH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (batches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Batches Found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Click 'CREATE BATCH' above to create your first physical training batch.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        items(batches) { batch ->
            val isClosed = batch.status == "CLOSED"
            val batchGroups = groups.filter { it.batchId == batch.batchId }
            val batchStudents = students.filter { it.batchId == batch.batchId }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isClosed) CyberBorder else BentoNavy.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isClosed) CyberSurface.copy(alpha = 0.6f) else CyberSurface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(batch.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isClosed) TextMuted else BentoNavy)
                            Text("Exam Type: ${batch.type}", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                        StatusDotBadge(
                            status = batch.status,
                            activeLabel = "ACTIVE BATCH",
                            closedLabel = "CLOSED BATCH"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Schedule: ${batch.schedule}", fontSize = 12.sp, color = TextSecondary)
                    if (batch.startDate.isNotBlank()) {
                        Text("Start Date: ${batch.startDate}", fontSize = 11.sp, color = TextMuted)
                    }
                    if (batch.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(batch.description, fontSize = 12.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Metrics Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatchBadge("${batchGroups.size} Groups", BentoPurpleOn)
                        BatchBadge("${batchStudents.size} Students", BentoNavy)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { batchToManage = batch },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MANAGE BATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { batchToEdit = batch }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Batch", tint = BentoNavy, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { batchToDelete = batch }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Batch", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                            }
                            if (!isClosed) {
                                TextButton(
                                    onClick = {
                                        repository.closeBatch(batch.batchId) {
                                            Toast.makeText(context, "${batch.name} is now CLOSED & archived.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("ARCHIVE", fontSize = 11.sp, color = BentoCoralOn, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE BATCH DIALOG ---
    if (showCreateBatchDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("MP Police") }
        var schedule by remember { mutableStateOf("Mon - Sat (5:30 AM - 8:30 AM)") }
        var startDate by remember { mutableStateOf(DateUtils.getTodayString()) }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateBatchDialog = false },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = BentoNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREATE NEW BATCH", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Batch Name (e.g. MP Police Batch 2026)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Exam / Category (e.g. MP Police, Army, Defence)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoPurpleOn, unfocusedBorderColor = CyberBorder)
                    )
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Training Schedule (Time/Days)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
                    )
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start Date (YYYY-MM-DD)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Training Goals", color = TextSecondary) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val newBatch = Batch(
                                name = name,
                                type = type,
                                schedule = schedule,
                                startDate = startDate,
                                description = description,
                                status = "ACTIVE"
                            )
                            repository.createBatch(newBatch) { success, err ->
                                if (success) {
                                    showCreateBatchDialog = false
                                    Toast.makeText(context, "Batch '$name' created and synced to Firebase!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error: ${err ?: "Failed to create batch"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a batch name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CREATE BATCH", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBatchDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- EDIT BATCH DIALOG ---
    batchToEdit?.let { target ->
        var name by remember { mutableStateOf(target.name) }
        var type by remember { mutableStateOf(target.type) }
        var schedule by remember { mutableStateOf(target.schedule) }
        var startDate by remember { mutableStateOf(target.startDate) }
        var description by remember { mutableStateOf(target.description) }
        var status by remember { mutableStateOf(target.status) }

        AlertDialog(
            onDismissRequest = { batchToEdit = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = BentoNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDIT BATCH DETAILS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Batch Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Exam / Category", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoPurpleOn)
                    )
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Schedule", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description", color = TextSecondary) },
                        maxLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = status == "ACTIVE",
                                onClick = { status = "ACTIVE" },
                                label = { Text("ACTIVE") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoMintOn, selectedLabelColor = Color.White)
                            )
                            FilterChip(
                                selected = status == "CLOSED",
                                onClick = { status = "CLOSED" },
                                label = { Text("CLOSED") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoCoralOn, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(
                            name = name,
                            type = type,
                            schedule = schedule,
                            startDate = startDate,
                            description = description,
                            status = status
                        )
                        repository.updateBatch(updated) { success, _ ->
                            if (success) {
                                batchToEdit = null
                                Toast.makeText(context, "Batch '$name' updated in Firebase!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { batchToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- DELETE BATCH CONFIRMATION DIALOG ---
    batchToDelete?.let { target ->
        val impactedStudents = students.count { it.batchId == target.batchId }
        val impactedGroups = groups.count { it.batchId == target.batchId }

        AlertDialog(
            onDismissRequest = { batchToDelete = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DELETE BATCH CONFIRMATION", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently DELETE '${target.name}'?\n\n" +
                    "⚠️ Impact:\n" +
                    "• $impactedGroups Groups inside this batch will be deleted.\n" +
                    "• $impactedStudents Students will be unassigned from this batch.\n\n" +
                    "This action will sync instantly across all devices via Firebase.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bId = target.batchId
                        val bName = target.name
                        batchToDelete = null
                        repository.deleteBatch(bId) { _, _ ->
                            Toast.makeText(context, "Batch '$bName' and associated data deleted.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DELETE BATCH", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { batchToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- MANAGE BATCH & GROUPS DIALOG (DETAILED SHEET) ---
    batchToManage?.let { activeBatch ->
        var selectedManageTab by remember { mutableStateOf(0) } // 0: GROUPS, 1: STUDENTS
        val batchGroups = groups.filter { it.batchId == activeBatch.batchId }
        val batchStudents = students.filter { it.batchId == activeBatch.batchId }

        AlertDialog(
            onDismissRequest = { batchToManage = null },
            containerColor = CyberSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BATCH: ${activeBatch.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                        IconButton(onClick = { batchToManage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                    Text("${batchGroups.size} Groups • ${batchStudents.size} Students Assigned", fontSize = 11.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedManageTab == 0,
                            onClick = { selectedManageTab = 0 },
                            label = { Text("GROUPS (${batchGroups.size})", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoPurpleOn,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                        FilterChip(
                            selected = selectedManageTab == 1,
                            onClick = { selectedManageTab = 1 },
                            label = { Text("STUDENTS (${batchStudents.size})", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BentoNavy,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                    }
                }
            },
            text = {
                Box(modifier = Modifier.height(380.dp)) {
                    if (selectedManageTab == 0) {
                        // GROUPS TAB INSIDE BATCH
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Groups in Batch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Button(
                                    onClick = { showCreateGroupForBatch = activeBatch },
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ADD GROUP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (batchGroups.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No groups created in this batch yet.", fontSize = 12.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Click 'ADD GROUP' to divide students into squads.", fontSize = 11.sp, color = TextSecondary)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(batchGroups) { group ->
                                        val groupMembers = batchStudents.filter { it.groupId == group.groupId }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(group.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                                                        StatusDotBadge(
                                                            status = if (activeBatch.status == "CLOSED") "CLOSED" else "ACTIVE",
                                                            activeLabel = "ACTIVE GROUP",
                                                            closedLabel = "CLOSED GROUP"
                                                        )
                                                    }
                                                    BatchBadge("${groupMembers.size} members", BentoNavy)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Leader: ${group.leaderName.ifBlank { "Unassigned" }}",
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Action Buttons for Group
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { groupForDetailsInBatch = group },
                                                        shape = RoundedCornerShape(100.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    OutlinedButton(
                                                        onClick = { groupLeaderToAssign = group },
                                                        shape = RoundedCornerShape(100.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("LEADER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(onClick = { groupToEdit = group }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BentoNavy, modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(onClick = { groupToDelete = group }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // STUDENTS TAB INSIDE BATCH
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Students in Batch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Button(
                                    onClick = { showAddStudentsToBatch = activeBatch },
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ADD STUDENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (batchStudents.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No students assigned to this batch.", fontSize = 12.sp, color = TextMuted)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Click 'ADD STUDENTS' to add academy trainees.", fontSize = 11.sp, color = TextSecondary)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(batchStudents) { student ->
                                        val assignedGroup = groups.find { it.groupId == student.groupId }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        ProfileAvatar(photoUrl = student.photoUrl, size = 32.dp, iconSize = 16.dp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                            Text("ID: ${student.studentId} • ${student.mobile}", fontSize = 10.sp, color = TextSecondary)
                                                        }
                                                    }

                                                    if (assignedGroup != null) {
                                                        Surface(
                                                            onClick = { groupForDetailsInBatch = assignedGroup },
                                                            shape = RoundedCornerShape(100.dp),
                                                            color = BentoPurpleOn
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = assignedGroup.name,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                                Spacer(modifier = Modifier.width(2.dp))
                                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                    } else {
                                                        Surface(
                                                            shape = RoundedCornerShape(100.dp),
                                                            color = CyberBorder.copy(alpha = 0.4f)
                                                        ) {
                                                            Text(
                                                                text = "Unassigned",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = TextMuted,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Student Batch Actions
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = { studentToMoveGroup = student },
                                                        shape = RoundedCornerShape(100.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("MOVE GROUP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    OutlinedButton(
                                                        onClick = { studentToRemoveFromBatch = student },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                                        shape = RoundedCornerShape(100.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("REMOVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { batchToManage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- CREATE GROUP IN BATCH DIALOG ---
    showCreateGroupForBatch?.let { activeBatch ->
        var groupName by remember { mutableStateOf("") }
        var leaderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateGroupForBatch = null },
            containerColor = CyberSurface,
            title = {
                Text("CREATE GROUP IN ${activeBatch.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name (e.g. Group Alpha)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoPurpleOn)
                    )
                    OutlinedTextField(
                        value = leaderName,
                        onValueChange = { leaderName = it },
                        label = { Text("Assigned Group Leader Name (Optional)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            val newGroup = Group(
                                batchId = activeBatch.batchId,
                                name = groupName,
                                leaderName = leaderName
                            )
                            repository.createGroup(newGroup) { success, _ ->
                                if (success) {
                                    showCreateGroupForBatch = null
                                    Toast.makeText(context, "Group '$groupName' created in Firebase!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CREATE GROUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupForBatch = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- EDIT GROUP DIALOG ---
    groupToEdit?.let { targetGroup ->
        var name by remember { mutableStateOf(targetGroup.name) }
        var leaderName by remember { mutableStateOf(targetGroup.leaderName) }

        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            containerColor = CyberSurface,
            title = { Text("EDIT GROUP DETAILS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = leaderName,
                        onValueChange = { leaderName = it },
                        label = { Text("Group Leader Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = targetGroup.copy(name = name, leaderName = leaderName)
                        repository.updateGroup(updated) { success, _ ->
                            if (success) {
                                groupToEdit = null
                                Toast.makeText(context, "Group '$name' updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- DELETE GROUP CONFIRMATION DIALOG ---
    groupToDelete?.let { targetGroup ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor = CyberSurface,
            title = { Text("DELETE GROUP CONFIRMATION", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete group '${targetGroup.name}'?\n\nStudents assigned to this group will become unassigned from the group.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gId = targetGroup.groupId
                        val gName = targetGroup.name
                        groupToDelete = null
                        repository.deleteGroup(gId) { _, _ ->
                            Toast.makeText(context, "Group '$gName' deleted.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DELETE GROUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- ASSIGN / CHANGE GROUP LEADER DIALOG ---
    groupLeaderToAssign?.let { targetGroup ->
        val groupMembers = students.filter { it.groupId == targetGroup.groupId || it.batchId == targetGroup.batchId }
        var selectedLeaderStudent by remember { mutableStateOf<Student?>(null) }
        var customLeaderName by remember { mutableStateOf(targetGroup.leaderName) }

        // Find if selected student is already leading another group
        val selectedExistingLeaderGroup = remember(selectedLeaderStudent, groups) {
            selectedLeaderStudent?.let { st ->
                groups.find { g ->
                    g.groupId != targetGroup.groupId &&
                    ((st.uid.isNotBlank() && g.leaderId == st.uid) ||
                     (st.studentId.isNotBlank() && g.leaderId == st.studentId) ||
                     (g.leaderName.equals(st.name, ignoreCase = true) && g.leaderName.isNotBlank()))
                }
            }
        }

        AlertDialog(
            onDismissRequest = { groupLeaderToAssign = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = BentoPurpleOn)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ASSIGN GROUP LEADER", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a leader for '${targetGroup.name}'. Note: 1 student can only be leader of 1 group.", fontSize = 11.sp, color = TextSecondary)

                    if (groupMembers.isNotEmpty()) {
                        Text("Group & Batch Trainees:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                        LazyColumn(modifier = Modifier.height(150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(groupMembers) { st ->
                                val isSelected = selectedLeaderStudent?.studentId == st.studentId
                                val existingGroupForSt = groups.find { g ->
                                    g.groupId != targetGroup.groupId &&
                                    ((st.uid.isNotBlank() && g.leaderId == st.uid) ||
                                     (st.studentId.isNotBlank() && g.leaderId == st.studentId) ||
                                     (g.leaderName.equals(st.name, ignoreCase = true) && g.leaderName.isNotBlank()))
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLeaderStudent = st
                                            customLeaderName = st.name
                                        }
                                        .border(
                                            1.dp,
                                            if (isSelected) BentoPurpleOn else CyberBorder,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) BentoPurpleOn.copy(alpha = 0.15f) else CyberBackground
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(st.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("ID: ${st.studentId}", fontSize = 10.sp, color = TextSecondary)
                                            if (existingGroupForSt != null) {
                                                Text(
                                                    "⚠️ Already Leader of: ${existingGroupForSt.name}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BentoCoralOn
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoPurpleOn, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedExistingLeaderGroup != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BentoCoralCard)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = BentoCoralOn, modifier = Modifier.size(16.dp))
                                Text(
                                    "${selectedLeaderStudent?.name} is leader of '${selectedExistingLeaderGroup.name}'. Assigning them here will transfer leadership to '${targetGroup.name}'.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BentoCoralOn
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customLeaderName,
                        onValueChange = {
                            customLeaderName = it
                            selectedLeaderStudent = null
                        },
                        label = { Text("Group Leader Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy)
                    )

                    if (targetGroup.leaderName.isNotBlank()) {
                        TextButton(
                            onClick = {
                                repository.assignGroupLeader(targetGroup.groupId, "", "") { success ->
                                    if (success) {
                                        groupLeaderToAssign = null
                                        Toast.makeText(context, "Leader unassigned from '${targetGroup.name}'", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("UNASSIGN / REMOVE LEADER", fontSize = 11.sp, color = BentoCoralOn, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val leaderUid = selectedLeaderStudent?.uid?.ifBlank { selectedLeaderStudent?.studentId } ?: "leader_${System.currentTimeMillis()}"
                        val leaderName = customLeaderName.ifBlank { selectedLeaderStudent?.name ?: "Group Leader" }

                        repository.assignGroupLeader(targetGroup.groupId, leaderUid, leaderName) { success ->
                            if (success) {
                                groupLeaderToAssign = null
                                Toast.makeText(context, "'$leaderName' assigned as Leader for '${targetGroup.name}'!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CONFIRM LEADER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupLeaderToAssign = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- ADD STUDENTS TO BATCH DIALOG ---
    showAddStudentsToBatch?.let { activeBatch ->
        val availableStudents = students.filter { it.batchId != activeBatch.batchId }
        var searchQuery by remember { mutableStateOf("") }

        val filteredAvailable = availableStudents.filter { s ->
            searchQuery.isBlank() ||
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.mobile.contains(searchQuery) ||
                s.studentId.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAddStudentsToBatch = null },
            containerColor = CyberSurface,
            title = {
                Text("ADD STUDENTS TO ${activeBatch.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search trainee name/ID...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredAvailable.isEmpty()) {
                        Text("No eligible unassigned students found.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredAvailable) { st ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(st.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("ID: ${st.studentId} • ${st.city}", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                repository.moveStudentGroup(st.studentId, activeBatch.batchId, "") { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "${st.name} added to ${activeBatch.name}!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                            shape = RoundedCornerShape(100.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddStudentsToBatch = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- MOVE STUDENT GROUP DIALOG ---
    studentToMoveGroup?.let { targetStudent ->
        val batchGroups = groups.filter { it.batchId == targetStudent.batchId }
        var selectedGroupId by remember { mutableStateOf(targetStudent.groupId) }

        AlertDialog(
            onDismissRequest = { studentToMoveGroup = null },
            containerColor = CyberSurface,
            title = {
                Text("MOVE ${targetStudent.name} TO GROUP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select target squad/group within the batch:", fontSize = 12.sp, color = TextSecondary)

                    // Option: Unassigned
                    FilterChip(
                        selected = selectedGroupId.isBlank(),
                        onClick = { selectedGroupId = "" },
                        label = { Text("Unassigned (No Group)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoNavy, selectedLabelColor = Color.White)
                    )

                    batchGroups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroupId == grp.groupId,
                            onClick = { selectedGroupId = grp.groupId },
                            label = { Text(grp.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoPurpleOn, selectedLabelColor = Color.White)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newGrpName = groups.find { it.groupId == selectedGroupId }?.name ?: "No Group"
                        repository.moveStudentGroup(targetStudent.studentId, targetStudent.batchId, selectedGroupId) { success ->
                            if (success) {
                                studentToMoveGroup = null
                                Toast.makeText(context, "${targetStudent.name} moved to '$newGrpName'!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("MOVE STUDENT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToMoveGroup = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- REMOVE STUDENT FROM BATCH CONFIRMATION DIALOG ---
    studentToRemoveFromBatch?.let { targetStudent ->
        AlertDialog(
            onDismissRequest = { studentToRemoveFromBatch = null },
            containerColor = CyberSurface,
            title = {
                Text("REMOVE FROM BATCH?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to remove ${targetStudent.name} (${targetStudent.studentId}) from this batch?\n\nTheir student profile remains intact, but they will be unassigned from batch training.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stName = targetStudent.name
                        studentToRemoveFromBatch = null
                        repository.removeStudentFromBatch(targetStudent.studentId) { _ ->
                            Toast.makeText(context, "$stName removed from batch.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("REMOVE STUDENT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToRemoveFromBatch = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- GROUP DETAILS DIALOG (OPENED FROM BATCH MANAGEMENT) ---
    groupForDetailsInBatch?.let { activeGroup ->
        val activeBatch = batches.find { it.batchId == activeGroup.batchId }
        Dialog(
            onDismissRequest = { groupForDetailsInBatch = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(24.dp),
                color = CyberSurface
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    AdminGroupDetailContent(
                        group = activeGroup,
                        batch = activeBatch,
                        allStudents = students,
                        allGroups = groups,
                        allBatches = batches,
                        repository = repository,
                        onBack = { groupForDetailsInBatch = null },
                        onChangeLeader = { groupLeaderToAssign = activeGroup },
                        onEditGroup = { groupToEdit = activeGroup },
                        onDeleteGroup = { groupToDelete = activeGroup }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminGroupsContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var selectedFilterBatch by remember { mutableStateOf("ALL") }
    var selectedGroupDetailsId by remember { mutableStateOf<String?>(null) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var groupLeaderToAssign by remember { mutableStateOf<Group?>(null) }

    val activeGroup = groups.find { it.groupId == selectedGroupDetailsId }

    if (activeGroup != null) {
        // --- GROUP DETAILS PAGE ---
        AdminGroupDetailContent(
            group = activeGroup,
            batch = batches.find { it.batchId == activeGroup.batchId },
            allStudents = students,
            allGroups = groups,
            allBatches = batches,
            repository = repository,
            onBack = { selectedGroupDetailsId = null },
            onChangeLeader = { groupLeaderToAssign = activeGroup },
            onEditGroup = { groupToEdit = activeGroup },
            onDeleteGroup = { groupToDelete = activeGroup }
        )
    } else {
        // --- GROUPS LIST PAGE ---
        val filteredGroups = remember(groups, selectedFilterBatch) {
            if (selectedFilterBatch == "ALL") groups
            else groups.filter { it.batchId == selectedFilterBatch }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("GROUP MANAGEMENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${groups.size} Total Groups • Realtime Firebase Sync", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CREATE GROUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Batch Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterBatch == "ALL",
                        onClick = { selectedFilterBatch = "ALL" },
                        label = { Text("ALL BATCHES", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoNavy, selectedLabelColor = Color.White)
                    )
                    batches.forEach { b ->
                        FilterChip(
                            selected = selectedFilterBatch == b.batchId,
                            onClick = { selectedFilterBatch = b.batchId },
                            label = { Text(b.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoPurpleOn, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            if (filteredGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No Groups Found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Select a batch filter or create a new group.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            items(filteredGroups) { group ->
                val b = batches.find { it.batchId == group.batchId }
                val groupStudents = students.filter { it.groupId == group.groupId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedGroupDetailsId = group.groupId }
                        .border(1.dp, BentoPurpleOn.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val isGroupClosed = b?.status == "CLOSED"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                                StatusDotBadge(
                                    status = if (isGroupClosed) "CLOSED" else "ACTIVE",
                                    activeLabel = "ACTIVE GROUP",
                                    closedLabel = "CLOSED GROUP"
                                )
                            }
                            BatchBadge(b?.name ?: "No Batch", BentoPurpleOn)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Group Leader: ${group.leaderName.ifBlank { "Unassigned" }}", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Text("Assigned Members: ${groupStudents.size} Trainees", fontSize = 11.sp, color = TextMuted)

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = CyberBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Group Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { selectedGroupDetailsId = group.groupId },
                                colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VIEW GROUP DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { groupLeaderToAssign = group },
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("LEADER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { groupToEdit = group }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = BentoNavy, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { groupToDelete = group }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE GROUP DIALOG ---
    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var selectedBatchId by remember { mutableStateOf(batches.firstOrNull()?.batchId ?: "") }
        var leaderName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CyberSurface,
            title = { Text("CREATE GROUP INSIDE BATCH", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target Batch:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(batches) { batch ->
                            FilterChip(
                                selected = selectedBatchId == batch.batchId,
                                onClick = { selectedBatchId = batch.batchId },
                                label = { Text(batch.name) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BentoNavy, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name (e.g. Squad Alpha)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoPurpleOn)
                    )
                    OutlinedTextField(
                        value = leaderName,
                        onValueChange = { leaderName = it },
                        label = { Text("Assigned Group Leader Name (Optional)", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && selectedBatchId.isNotBlank()) {
                            val newGroup = Group(
                                batchId = selectedBatchId,
                                name = groupName,
                                leaderName = leaderName
                            )
                            repository.createGroup(newGroup) { success, _ ->
                                if (success) {
                                    showCreateDialog = false
                                    Toast.makeText(context, "Group '$groupName' Created & Synced to Firestore!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter group name and select batch", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CREATE GROUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- EDIT GROUP DIALOG ---
    groupToEdit?.let { targetGroup ->
        var name by remember { mutableStateOf(targetGroup.name) }
        var leaderName by remember { mutableStateOf(targetGroup.leaderName) }

        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            containerColor = CyberSurface,
            title = { Text("EDIT GROUP DETAILS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = leaderName,
                        onValueChange = { leaderName = it },
                        label = { Text("Group Leader Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = targetGroup.copy(name = name, leaderName = leaderName)
                        repository.updateGroup(updated) { success, _ ->
                            if (success) {
                                groupToEdit = null
                                Toast.makeText(context, "Group '$name' updated in Firestore!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("SAVE CHANGES", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- DELETE GROUP CONFIRMATION ---
    groupToDelete?.let { targetGroup ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            containerColor = CyberSurface,
            title = { Text("DELETE GROUP CONFIRMATION", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete group '${targetGroup.name}'?\n\nStudents assigned to this group will become unassigned from the group in Firestore.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val gId = targetGroup.groupId
                        val gName = targetGroup.name
                        groupToDelete = null
                        if (selectedGroupDetailsId == gId) selectedGroupDetailsId = null
                        repository.deleteGroup(gId) { _, _ ->
                            Toast.makeText(context, "Group '$gName' deleted.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DELETE GROUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // --- ASSIGN GROUP LEADER ---
    groupLeaderToAssign?.let { targetGroup ->
        val groupMembers = students.filter { it.groupId == targetGroup.groupId || it.batchId == targetGroup.batchId }
        var selectedLeaderStudent by remember { mutableStateOf<Student?>(null) }
        var customLeaderName by remember { mutableStateOf(targetGroup.leaderName) }

        AlertDialog(
            onDismissRequest = { groupLeaderToAssign = null },
            containerColor = CyberSurface,
            title = { Text("ASSIGN GROUP LEADER", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (groupMembers.isNotEmpty()) {
                        Text("Pick leader from trainees:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                        LazyColumn(modifier = Modifier.height(130.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(groupMembers) { st ->
                                val isSelected = selectedLeaderStudent?.studentId == st.studentId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLeaderStudent = st
                                            customLeaderName = st.name
                                        }
                                        .border(1.dp, if (isSelected) BentoPurpleOn else CyberBorder, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) BentoPurpleOn.copy(alpha = 0.15f) else CyberBackground)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(st.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (isSelected) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoPurpleOn, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customLeaderName,
                        onValueChange = {
                            customLeaderName = it
                            selectedLeaderStudent = null
                        },
                        label = { Text("Group Leader Name", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val leaderUid = selectedLeaderStudent?.uid ?: "leader_${System.currentTimeMillis()}"
                        val leaderName = customLeaderName.ifBlank { selectedLeaderStudent?.name ?: "Group Leader" }

                        repository.assignGroupLeader(targetGroup.groupId, leaderUid, leaderName) { success ->
                            if (success) {
                                groupLeaderToAssign = null
                                Toast.makeText(context, "'$leaderName' assigned as Leader for '${targetGroup.name}'!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CONFIRM LEADER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupLeaderToAssign = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }
}

// --- FULL GROUP DETAILS PAGE CONTENT ---
@Composable
fun AdminGroupDetailContent(
    group: Group,
    batch: Batch?,
    allStudents: List<Student>,
    allGroups: List<Group>,
    allBatches: List<Batch>,
    repository: AcademyRepository,
    onBack: () -> Unit,
    onChangeLeader: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val context = LocalContext.current
    var studentSearchQuery by remember { mutableStateOf("") }

    // Dialog state inside Group Details Page
    var showAddStudentsDialog by remember { mutableStateOf(false) }
    var studentForDetailsModal by remember { mutableStateOf<Student?>(null) }
    var studentToRemove by remember { mutableStateOf<Student?>(null) }

    val groupStudents = remember(allStudents, group.groupId) {
        allStudents.filter { it.groupId == group.groupId }
    }

    val filteredGroupStudents = remember(groupStudents, studentSearchQuery) {
        if (studentSearchQuery.isBlank()) groupStudents
        else groupStudents.filter { st ->
            st.name.contains(studentSearchQuery, ignoreCase = true) ||
            st.studentId.contains(studentSearchQuery, ignoreCase = true) ||
            st.mobile.contains(studentSearchQuery) ||
            st.city.contains(studentSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // Top Nav & Title Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BentoNavy, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                val isGroupClosed = batch?.status == "CLOSED"
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("GROUP DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                        StatusDotBadge(
                            status = if (isGroupClosed) "CLOSED" else "ACTIVE",
                            activeLabel = "ACTIVE",
                            closedLabel = "CLOSED"
                        )
                    }
                    Text(group.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                BatchBadge(batch?.name ?: "No Batch", BentoNavy)
            }
        }

        // Group Leader & Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoPurpleOn.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Group Leader", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = BentoPurpleOn, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    group.leaderName.ifBlank { "Unassigned" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoNavy
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onChangeLeader,
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("CHANGE LEADER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BatchBadge("${groupStudents.size} Students", BentoNavy)
                            BatchBadge("Realtime Sync", BentoMintOn)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onEditGroup) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Group", tint = BentoNavy, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDeleteGroup) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Action Toolbar & Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ASSIGNED STUDENTS (${groupStudents.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Button(
                    onClick = { showAddStudentsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD STUDENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = studentSearchQuery,
                onValueChange = { studentSearchQuery = it },
                label = { Text("Search student in ${group.name}...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
            )
        }

        // Student List
        if (filteredGroupStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.GroupOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (studentSearchQuery.isNotBlank()) "No students match '$studentSearchQuery'"
                            else "No students assigned to ${group.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Click 'ADD STUDENTS' above to assign trainees to this squad.", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredGroupStudents) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatar(photoUrl = student.photoUrl, size = 42.dp, iconSize = 20.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("ID: ${student.studentId} • 📞 ${student.mobile}", fontSize = 11.sp, color = TextSecondary)
                                    if (student.fatherName.isNotBlank() || student.city.isNotBlank()) {
                                        Text(
                                            "Father: ${student.fatherName.ifBlank { "N/A" }} • 📍 ${student.city.ifBlank { "N/A" }}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            BatchBadge(student.status, if (student.isBlocked) BentoCoralOn else BentoMintOn)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = CyberBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { studentForDetailsModal = student },
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VIEW PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedButton(
                                onClick = { studentToRemove = student },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REMOVE FROM GROUP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD STUDENTS TO GROUP DIALOG ---
    if (showAddStudentsDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val availableCandidates = remember(allStudents, group.groupId) {
            allStudents.filter { it.groupId != group.groupId }
        }
        val filteredCandidates = remember(availableCandidates, searchQuery) {
            if (searchQuery.isBlank()) availableCandidates
            else availableCandidates.filter { s ->
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.studentId.contains(searchQuery, ignoreCase = true) ||
                s.mobile.contains(searchQuery) ||
                s.city.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddStudentsDialog = false },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BentoNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD STUDENTS TO ${group.name}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search candidate name, ID, or phone...", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredCandidates.isEmpty()) {
                        Text("No eligible students available to add.", fontSize = 12.sp, color = TextMuted)
                    } else {
                        LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredCandidates) { st ->
                                val currentGrpName = allGroups.find { it.groupId == st.groupId }?.name ?: "Unassigned"
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ProfileAvatar(photoUrl = st.photoUrl, size = 32.dp, iconSize = 16.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(st.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("ID: ${st.studentId} • Current: $currentGrpName", fontSize = 10.sp, color = TextSecondary)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                repository.moveStudentGroup(st.studentId, group.batchId, group.groupId) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "${st.name} added to ${group.name}!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                            shape = RoundedCornerShape(100.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("ADD TO GROUP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddStudentsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- STUDENT PROFILE DETAILS MODAL ---
    studentForDetailsModal?.let { st ->
        val currentBatchName = allBatches.find { it.batchId == st.batchId }?.name ?: "Unassigned"
        val currentGroupName = allGroups.find { it.groupId == st.groupId }?.name ?: "Unassigned"

        AlertDialog(
            onDismissRequest = { studentForDetailsModal = null },
            containerColor = CyberSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STUDENT PROFILE", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                    BatchBadge(st.status, if (st.isBlocked) BentoCoralOn else BentoMintOn)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(photoUrl = st.photoUrl, size = 56.dp, iconSize = 28.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(st.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("ID: ${st.studentId}", fontSize = 12.sp, color = BentoPurpleOn, fontWeight = FontWeight.Bold)
                            Text("Target: ${st.targetExam}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Divider(color = CyberBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📞 Mobile: ${st.mobile.ifBlank { "N/A" }}", fontSize = 12.sp, color = TextPrimary)
                        Text("👤 Father's Name: ${st.fatherName.ifBlank { "N/A" }}", fontSize = 12.sp, color = TextPrimary)
                        Text("📍 City: ${st.city.ifBlank { "Indore" }}", fontSize = 12.sp, color = TextPrimary)
                        Text("🎂 DOB / Age: ${st.dob.ifBlank { "N/A" }} (${if (st.calculatedAge() > 0) "${st.calculatedAge()} yrs" else "Age N/A"})", fontSize = 12.sp, color = TextPrimary)
                        Text("🚻 Gender: ${st.gender}", fontSize = 12.sp, color = TextPrimary)
                    }

                    Divider(color = CyberBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📚 Batch: $currentBatchName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                        Text("👥 Group: $currentGroupName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                        Text("🏃 Target Run: ${st.targetRunTime}", fontSize = 11.sp, color = TextSecondary)
                        Text("🏋️ Shot Put: ${st.targetShotPut} • Long Jump: ${st.targetLongJump}", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { studentForDetailsModal = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- REMOVE STUDENT CONFIRMATION DIALOG ---
    studentToRemove?.let { st ->
        AlertDialog(
            onDismissRequest = { studentToRemove = null },
            containerColor = CyberSurface,
            title = {
                Text("REMOVE STUDENT FROM GROUP", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to remove '${st.name}' (${st.studentId}) from group '${group.name}'?\n\nThis change will update Firestore real-time immediately.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val stName = st.name
                        studentToRemove = null
                        repository.moveStudentGroup(st.studentId, st.batchId, "") { success ->
                            if (success) {
                                Toast.makeText(context, "$stName removed from ${group.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("REMOVE STUDENT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToRemove = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun AdminStudentsContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    attendanceList: List<AttendanceRecord> = emptyList(),
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterBatch by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, BLOCKED

    var showAddDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var studentToBlock by remember { mutableStateOf<Student?>(null) }
    var studentForProgressModal by remember { mutableStateOf<Student?>(null) }
    var studentForLeaderRole by remember { mutableStateOf<Student?>(null) }

    var name by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Indore") }

    val filteredStudents = remember(students, searchQuery, selectedFilterBatch, selectedStatusFilter) {
        students.filter { s ->
            val matchSearch = searchQuery.isBlank() ||
                s.name.contains(searchQuery, ignoreCase = true) ||
                s.fatherName.contains(searchQuery, ignoreCase = true) ||
                s.mobile.contains(searchQuery) ||
                s.city.contains(searchQuery, ignoreCase = true) ||
                s.studentId.contains(searchQuery, ignoreCase = true)

            val matchBatch = selectedFilterBatch == "ALL" || s.batchId == selectedFilterBatch

            val matchStatus = when (selectedStatusFilter) {
                "ACTIVE" -> !s.isBlocked
                "BLOCKED" -> s.isBlocked
                else -> true
            }

            matchSearch && matchBatch && matchStatus
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("STUDENT MANAGEMENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${students.size} Total • ${students.count { it.isBlocked }} Blocked", fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Student", tint = BentoMintOn, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name, Student ID, Mobile, City...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BentoNavy) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips (ALL, ACTIVE, BLOCKED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "ACTIVE", "BLOCKED").forEach { status ->
                    val isSelected = selectedStatusFilter == status
                    val activeBg = if (status == "BLOCKED") Color(0xFFD32F2F) else BentoNavy
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeBg,
                            selectedLabelColor = Color.White,
                            containerColor = CyberSurface,
                            labelColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            item { EmptyStateCard("No students found matching query.") }
        } else {
            items(filteredStudents) { student ->
                val b = batches.find { it.batchId == student.batchId }
                val g = groups.find { it.groupId == student.groupId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { studentForProgressModal = student }
                        .border(
                            1.dp,
                            if (student.isBlocked) Color(0xFFEF5350) else CyberBorder,
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (student.isBlocked) Color(0xFFFFEBEE) else CyberSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProfileAvatar(
                                    photoUrl = student.photoUrl,
                                    size = 42.dp,
                                    iconSize = 22.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("ID: ${student.studentId}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BentoNavy)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (student.isLeader) {
                                    BatchBadge("LEADER", BentoPurpleOn)
                                }
                                if (student.isBlocked) {
                                    BatchBadge("BLOCKED", Color(0xFFD32F2F))
                                } else {
                                    BatchBadge("ACTIVE", BentoMintOn)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Father: ${student.fatherName} • Mobile: ${student.mobile}", fontSize = 12.sp, color = TextSecondary)
                        Text("City: ${student.city} • Age: ${student.calculatedAge()} yrs", fontSize = 11.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BatchBadge(b?.name ?: "No Batch", BentoPurpleOn)
                            BatchBadge(g?.name ?: "No Group", BentoNavy)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CyberBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons: VIEW PROGRESS, LEADER ROLE, BLOCK/UNBLOCK, DELETE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { studentForProgressModal = student },
                                colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            if (student.isLeader) {
                                OutlinedButton(
                                    onClick = {
                                        repository.removeLeaderRoleFromStudent(student) {
                                            Toast.makeText(context, "Leader authority removed from ${student.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoPurpleOn),
                                    border = BorderStroke(1.dp, BentoPurpleOn),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("REMOVE LEADER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { studentForLeaderRole = student },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoPurpleOn),
                                    border = BorderStroke(1.dp, BentoPurpleOn),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MAKE LEADER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (student.isBlocked) {
                                OutlinedButton(
                                    onClick = {
                                        repository.blockStudent(student.studentId, student.uid, block = false) { _, _ ->
                                            Toast.makeText(context, "${student.name} is UNBLOCKED", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoMintOn),
                                    border = BorderStroke(1.dp, BentoMintOn),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("UNBLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { studentToBlock = student },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                    border = BorderStroke(1.dp, Color(0xFFE65100)),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { studentToDelete = student },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DELETE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // STUDENT PROGRESS & INFO MODAL
    studentForProgressModal?.let { st ->
        AdminStudentProgressModal(
            student = st,
            batches = batches,
            groups = groups,
            attendanceList = attendanceList,
            repository = repository,
            onDismiss = { studentForProgressModal = null }
        )
    }

    // BLOCK CONFIRMATION DIALOG
    studentToBlock?.let { target ->
        AlertDialog(
            onDismissRequest = { studentToBlock = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BLOCK STUDENT?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to BLOCK ${target.name} (${target.studentId})?\n\nThey will be immediately blocked from logging in or using the app, synced in real-time across devices.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val st = target
                        studentToBlock = null
                        repository.blockStudent(st.studentId, st.uid, block = true) { _, _ ->
                            Toast.makeText(context, "${st.name} has been BLOCKED.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("BLOCK STUDENT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToBlock = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    studentToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DELETE STUDENT RECORD?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently DELETE ${target.name} (${target.studentId})?\n\nThis will automatically clear their profile, user credentials, and ALL attendance records from Firebase Realtime sync.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val st = target
                        studentToDelete = null
                        repository.deleteStudent(st.studentId, st.uid) { _, _ ->
                            Toast.makeText(context, "Student ${st.name} deleted & all data cleared.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("DELETE ALL DATA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // ASSIGN LEADER ROLE DIALOG
    studentForLeaderRole?.let { targetStudent ->
        var selectedGroupId by remember { mutableStateOf(targetStudent.groupId.ifBlank { groups.firstOrNull()?.groupId ?: "" }) }

        AlertDialog(
            onDismissRequest = { studentForLeaderRole = null },
            containerColor = CyberSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = BentoPurpleOn)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APPOINT AS GROUP LEADER", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Appoint '${targetStudent.name}' (${targetStudent.studentId}) as Group Leader.\n\n" +
                        "Their existing student account will automatically gain Leader permissions and sync instantly across devices without needing separate registration.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Text("Select Group to Lead:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)

                    LazyColumn(modifier = Modifier.height(140.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(groups) { g ->
                            val isSel = selectedGroupId == g.groupId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGroupId = g.groupId }
                                    .border(1.dp, if (isSel) BentoPurpleOn else CyberBorder, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSel) BentoPurpleOn.copy(alpha = 0.15f) else CyberBackground)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(g.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (g.leaderName.isNotBlank()) {
                                            Text("Current Leader: ${g.leaderName}", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                    if (isSel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoPurpleOn, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val st = targetStudent
                        val grpId = selectedGroupId
                        studentForLeaderRole = null
                        val leaderUid = st.uid.ifBlank { st.studentId }
                        repository.assignGroupLeader(grpId, leaderUid, st.name) { success ->
                            if (success) {
                                val targetGrpName = groups.find { it.groupId == grpId }?.name ?: "Group"
                                Toast.makeText(context, "${st.name} is now Leader of '$targetGrpName'!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPurpleOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("CONFIRM LEADER ROLE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForLeaderRole = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CyberSurface,
            title = { Text("ENROLL NEW STUDENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name", color = TextSecondary) }, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Father's Name", color = TextSecondary) }, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number", color = TextSecondary) }, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City", color = TextSecondary) }, shape = RoundedCornerShape(16.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val newStudent = Student(
                                name = name,
                                fatherName = fatherName,
                                mobile = mobile,
                                city = city,
                                dob = "2001-01-01",
                                batchId = batches.firstOrNull()?.batchId ?: "",
                                groupId = groups.firstOrNull()?.groupId ?: "",
                                profileCompleted = true
                            )
                            repository.addOrUpdateStudent(newStudent) { _, _ ->
                                showAddDialog = false
                                name = ""
                                fatherName = ""
                                mobile = ""
                                Toast.makeText(context, "Student Enrolled!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoMintOn),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("ENROLL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun AdminStudentProgressModal(
    student: Student,
    batches: List<Batch>,
    groups: List<Group>,
    attendanceList: List<AttendanceRecord>,
    repository: AcademyRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: PROFILE, 1: FITNESS & PROGRESS, 2: ATTENDANCE

    var isEditingFitness by remember { mutableStateOf(false) }
    var targetExam by remember(student) { mutableStateOf(student.targetExam) }
    var targetRunTime by remember(student) { mutableStateOf(student.targetRunTime) }
    var targetLongJump by remember(student) { mutableStateOf(student.targetLongJump) }
    var targetShotPut by remember(student) { mutableStateOf(student.targetShotPut) }
    var fitnessNotes by remember(student) { mutableStateOf(student.fitnessNotes) }

    val studentAttendance = remember(attendanceList, student.studentId) {
        attendanceList.filter { it.studentId == student.studentId }.sortedByDescending { it.date }
    }
    val totalDays = studentAttendance.size
    val presentDays = studentAttendance.count { it.status == AttendanceStatus.PRESENT }
    val absentDays = studentAttendance.count { it.status == AttendanceStatus.ABSENT }
    val attendancePct = if (totalDays > 0) ((presentDays.toFloat() / totalDays) * 100).toInt() else 0

    val b = batches.find { it.batchId == student.batchId }
    val g = groups.find { it.groupId == student.groupId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = CyberSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(photoUrl = student.photoUrl, size = 48.dp, iconSize = 24.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(student.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("ID: ${student.studentId} • ${student.targetExam}", fontSize = 11.sp, color = BentoNavy, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BatchBadge(b?.name ?: "No Batch", BentoPurpleOn)
                                BatchBadge(g?.name ?: "No Group", BentoNavy)
                                BatchBadge(student.status, if (student.isBlocked) BentoCoralOn else BentoMintOn)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = CyberBackground,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = BentoNavy
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("INFO & PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = BentoNavy,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("FITNESS & PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = BentoNavy,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("ATTENDANCE ($totalDays)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = BentoNavy,
                        unselectedContentColor = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> {
                            // INFO & PROFILE TAB
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("PERSONAL DETAILS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                                            Divider(color = CyberBorder.copy(alpha = 0.5f))
                                            Text("👤 Full Name: ${student.name}", fontSize = 12.sp, color = TextPrimary)
                                            Text("👨‍👦 Father's Name: ${student.fatherName.ifBlank { "N/A" }}", fontSize = 12.sp, color = TextPrimary)
                                            Text("📞 Mobile Number: ${student.mobile.ifBlank { "N/A" }}", fontSize = 12.sp, color = TextPrimary)
                                            Text("📍 City: ${student.city.ifBlank { "Indore" }}", fontSize = 12.sp, color = TextPrimary)
                                            Text("🎂 Date of Birth: ${student.dob.ifBlank { "N/A" }} (${if (student.calculatedAge() > 0) "${student.calculatedAge()} yrs" else "Age N/A"})", fontSize = 12.sp, color = TextPrimary)
                                            Text("🚻 Gender: ${student.gender}", fontSize = 12.sp, color = TextPrimary)
                                            Text("🆔 Student ID: ${student.studentId}", fontSize = 12.sp, color = TextPrimary)
                                            Text("🔑 User UID: ${student.uid.ifBlank { "Not Linked" }}", fontSize = 11.sp, color = TextMuted)
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("ACADEMY ALIGNMENT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                                            Divider(color = CyberBorder.copy(alpha = 0.5f))
                                            Text("📚 Assigned Batch: ${b?.name ?: "Unassigned"}", fontSize = 12.sp, color = TextPrimary)
                                            Text("👥 Assigned Group: ${g?.name ?: "Unassigned"}", fontSize = 12.sp, color = TextPrimary)
                                            Text("🎯 Target Exam: ${student.targetExam}", fontSize = 12.sp, color = TextPrimary)
                                            Text("⚡ Account Status: ${student.status}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (student.isBlocked) BentoCoralOn else BentoMintOn)
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // FITNESS & PROGRESS TAB
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("PHYSICAL FITNESS TARGETS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                                                IconButton(onClick = { isEditingFitness = !isEditingFitness }) {
                                                    Icon(if (isEditingFitness) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Fitness", tint = BentoNavy)
                                                }
                                            }
                                            Divider(color = CyberBorder.copy(alpha = 0.5f))

                                            if (!isEditingFitness) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    StatBox("RUN TARGET", student.targetRunTime, Icons.Default.DirectionsRun, BentoNavy, Modifier.weight(1f))
                                                    StatBox("SHOT PUT", student.targetShotPut, Icons.Default.FitnessCenter, BentoPurpleOn, Modifier.weight(1f))
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    StatBox("LONG JUMP", student.targetLongJump, Icons.Default.Height, BentoMintOn, Modifier.weight(1f))
                                                    StatBox("EXAM", student.targetExam, Icons.Default.School, BentoCoralOn, Modifier.weight(1f))
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("🏋️ Fitness Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = CyberSurface
                                                ) {
                                                    Text(
                                                        student.fitnessNotes.ifBlank { "No fitness notes added." },
                                                        fontSize = 12.sp,
                                                        color = TextSecondary,
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            } else {
                                                // EDIT FORM
                                                OutlinedTextField(
                                                    value = targetExam,
                                                    onValueChange = { targetExam = it },
                                                    label = { Text("Target Exam (e.g. MP Police Constable)", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = targetRunTime,
                                                    onValueChange = { targetRunTime = it },
                                                    label = { Text("Target 800m Run Time (e.g. 02:40 min)", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = targetLongJump,
                                                    onValueChange = { targetLongJump = it },
                                                    label = { Text("Target Long Jump (e.g. 15.0 ft)", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = targetShotPut,
                                                    onValueChange = { targetShotPut = it },
                                                    label = { Text("Target Shot Put (e.g. 25.0 ft)", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = fitnessNotes,
                                                    onValueChange = { fitnessNotes = it },
                                                    label = { Text("Fitness Assessment & Notes", fontSize = 11.sp) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Button(
                                                    onClick = {
                                                        val updated = student.copy(
                                                            targetExam = targetExam,
                                                            targetRunTime = targetRunTime,
                                                            targetLongJump = targetLongJump,
                                                            targetShotPut = targetShotPut,
                                                            fitnessNotes = fitnessNotes
                                                        )
                                                        repository.addOrUpdateStudent(updated) { _, _ ->
                                                            isEditingFitness = false
                                                            Toast.makeText(context, "Fitness Targets Saved!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                                                    shape = RoundedCornerShape(100.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("SAVE FITNESS TARGETS", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // ATTENDANCE TAB
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("ATTENDANCE PERFORMANCE SUMMARY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                                            Divider(color = CyberBorder.copy(alpha = 0.5f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                StatBox("RECORDED DAYS", "$totalDays", Icons.Default.Event, BentoNavy, Modifier.weight(1f))
                                                StatBox("PRESENT", "$presentDays", Icons.Default.CheckCircle, BentoMintOn, Modifier.weight(1f))
                                                StatBox("ABSENT", "$absentDays", Icons.Default.Cancel, BentoCoralOn, Modifier.weight(1f))
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Attendance Rate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("$attendancePct%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (attendancePct >= 75) BentoMintOn else BentoCoralOn)
                                            }

                                            LinearProgressIndicator(
                                                progress = { if (totalDays > 0) presentDays.toFloat() / totalDays else 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = if (attendancePct >= 75) BentoMintOn else BentoCoralOn,
                                                trackColor = CyberBorder
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text("RECENT ATTENDANCE LOGS (${studentAttendance.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }

                                if (studentAttendance.isEmpty()) {
                                    item {
                                        Text("No attendance records logged for this trainee.", fontSize = 12.sp, color = TextMuted)
                                    }
                                } else {
                                    items(studentAttendance) { rec ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = CyberBackground)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("📅 Date: ${rec.date}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Marked by: ${rec.markedByRole}", fontSize = 10.sp, color = TextSecondary)
                                                }
                                                BatchBadge(
                                                    text = if (rec.status == AttendanceStatus.PRESENT) "PRESENT" else "ABSENT",
                                                    color = if (rec.status == AttendanceStatus.PRESENT) BentoMintOn else BentoCoralOn
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modal Footer
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoNavy),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE PROFILE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminAttendanceContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    attendanceList: List<AttendanceRecord>,
    user: User,
    repository: AcademyRepository
) {
    val context = LocalContext.current

    // 1. Selected Batch State
    var selectedBatchId by remember(batches) {
        mutableStateOf(batches.firstOrNull { it.status == "ACTIVE" }?.batchId ?: batches.firstOrNull()?.batchId ?: "")
    }

    // Selected Batch Object
    val selectedBatch = batches.find { it.batchId == selectedBatchId }

    // Students filtered by selected batch
    val batchStudents = remember(students, selectedBatchId) {
        students.filter { it.batchId == selectedBatchId }
    }

    // 2. Calendar Month and Date Selection State
    val todayStr = DateUtils.getTodayString()
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateString by remember { mutableStateOf(todayStr) }

    // Student search within batch
    var searchQuery by remember { mutableStateOf("") }
    val filteredStudents = remember(batchStudents, searchQuery) {
        if (searchQuery.isBlank()) batchStudents
        else batchStudents.filter { s ->
            s.name.contains(searchQuery, ignoreCase = true) ||
            s.studentId.contains(searchQuery, ignoreCase = true) ||
            s.mobile.contains(searchQuery)
        }
    }

    // 3. Current Attendance Map for Selected Date
    val attendanceMap = remember { mutableStateMapOf<String, AttendanceStatus>() }

    LaunchedEffect(selectedDateString, batchStudents, attendanceList) {
        attendanceMap.clear()
        batchStudents.forEach { s ->
            val rec = attendanceList.find { it.studentId == s.studentId && it.date == selectedDateString }
            if (rec != null) {
                attendanceMap[s.studentId] = rec.status
            }
        }
    }

    // Calculate metrics for selected date & batch
    val totalInBatch = batchStudents.size
    val presentCount = batchStudents.count { attendanceMap[it.studentId] == AttendanceStatus.PRESENT }
    val absentCount = batchStudents.count { attendanceMap[it.studentId] == AttendanceStatus.ABSENT }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // --- 1. HEADER & BATCH SELECTOR ---
        item {
            Column {
                Text("BATCH ATTENDANCE MANAGEMENT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Select a batch to view trainees, calendar, and real-time attendance.", fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (batches.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No batches found. Please create a batch first.", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                Text("SELECT BATCH:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(batches) { batch ->
                        val isSelected = batch.batchId == selectedBatchId
                        val count = students.count { it.batchId == batch.batchId }
                        val isClosed = batch.status == "CLOSED"

                        Card(
                            modifier = Modifier
                                .clickable { selectedBatchId = batch.batchId }
                                .border(
                                    2.dp,
                                    if (isSelected) BentoNavy else CyberBorder,
                                    RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BentoNavy.copy(alpha = 0.12f) else CyberSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isClosed) BentoCoralOn else BentoMintOn)
                                )
                                Column {
                                    Text(
                                        batch.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BentoNavy else TextPrimary
                                    )
                                    Text(
                                        "$count Students • ${if (isClosed) "CLOSED" else "ACTIVE"}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. MONTHLY CALENDAR CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month Navigation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val prev = selectedCalendar.clone() as Calendar
                                prev.add(Calendar.MONTH, -1)
                                selectedCalendar = prev
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = BentoNavy)
                        }

                        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
                        val monthTitle = monthFormat.format(selectedCalendar.time)
                        Text(
                            text = monthTitle.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoNavy
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    selectedCalendar = Calendar.getInstance()
                                    selectedDateString = todayStr
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("TODAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                            }

                            IconButton(
                                onClick = {
                                    val next = selectedCalendar.clone() as Calendar
                                    next.add(Calendar.MONTH, 1)
                                    selectedCalendar = next
                                }
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = BentoNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days of Week Header
                    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = CyberBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Days Grid
                    val calCopy = selectedCalendar.clone() as Calendar
                    calCopy.set(Calendar.DAY_OF_MONTH, 1)
                    val daysInMonth = calCopy.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = calCopy.get(Calendar.DAY_OF_WEEK)
                    val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

                    val totalSlots = startOffset + daysInMonth
                    val rows = (totalSlots + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (c in 0 until 7) {
                                    val index = r * 7 + c
                                    val dayNum = index - startOffset + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val dayDateStr = DateUtils.formatDate(
                                            calCopy.get(Calendar.YEAR),
                                            calCopy.get(Calendar.MONTH),
                                            dayNum
                                        )
                                        val isSelectedDay = dayDateStr == selectedDateString
                                        val isToday = dayDateStr == todayStr

                                        // Check if attendance exists for this day in selected batch
                                        val hasAttendanceRecords = attendanceList.any { att ->
                                            att.date == dayDateStr && batchStudents.any { st -> st.studentId == att.studentId }
                                        }

                                        val isFutureDay = DateUtils.isAfter(dayDateStr, todayStr)

                                        val cellBg = when {
                                            isSelectedDay -> BentoNavy
                                            isToday -> BentoPurpleOn.copy(alpha = 0.15f)
                                            isFutureDay -> CyberSurfaceVariant.copy(alpha = 0.3f)
                                            else -> Color.Transparent
                                        }

                                        val textColor = when {
                                            isSelectedDay -> Color.White
                                            isToday -> BentoPurpleOn
                                            isFutureDay -> TextMuted.copy(alpha = 0.35f)
                                            else -> TextPrimary
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(cellBg)
                                                .border(
                                                    width = if (isToday && !isSelectedDay) 1.5.dp else 0.dp,
                                                    color = if (isToday && !isSelectedDay) BentoPurpleOn else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (isFutureDay) {
                                                        Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        selectedDateString = dayDateStr
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelectedDay || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = textColor
                                                )
                                                if (hasAttendanceRecords) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelectedDay) Color.Yellow else BentoMintOn)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ATTENDANCE METRICS FOR SELECTED DATE ---
        item {
            val formattedSelectedDate = selectedDateString
            val percentage = if (totalInBatch > 0) (presentCount.toFloat() / totalInBatch * 100).toInt() else 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoNavy.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📅 $formattedSelectedDate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoNavy
                            )
                            Text(
                                "Batch: ${selectedBatch?.name ?: "All Batches"}",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        BatchBadge("$percentage% Present", if (percentage >= 75) BentoMintOn else BentoCoralOn)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox("TOTAL", "$totalInBatch", Icons.Default.People, BentoNavy, Modifier.weight(1f))
                        StatBox("PRESENT", "$presentCount", Icons.Default.CheckCircle, BentoMintOn, Modifier.weight(1f))
                        StatBox("ABSENT", "$absentCount", Icons.Default.Cancel, BentoCoralOn, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bulk Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (DateUtils.isAfter(selectedDateString, DateUtils.getTodayString())) {
                                    Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (batchStudents.isNotEmpty()) {
                                    val records = batchStudents.associate { it.studentId to AttendanceStatus.PRESENT }
                                    repository.saveAttendanceBatch(
                                        date = selectedDateString,
                                        batchId = selectedBatchId,
                                        groupId = "",
                                        recordsMap = records,
                                        markedByUid = user.uid,
                                        markedByRole = "ADMIN",
                                        onComplete = { _, _ ->
                                            Toast.makeText(context, "All ${batchStudents.size} marked PRESENT!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoMintOn),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ALL PRESENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (DateUtils.isAfter(selectedDateString, DateUtils.getTodayString())) {
                                    Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (batchStudents.isNotEmpty()) {
                                    val records = batchStudents.associate { it.studentId to AttendanceStatus.ABSENT }
                                    repository.saveAttendanceBatch(
                                        date = selectedDateString,
                                        batchId = selectedBatchId,
                                        groupId = "",
                                        recordsMap = records,
                                        markedByUid = user.uid,
                                        markedByRole = "ADMIN",
                                        onComplete = { _, _ ->
                                            Toast.makeText(context, "All ${batchStudents.size} marked ABSENT!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoCoralOn),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.RemoveDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ALL ABSENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- 4. STUDENT ATTENDANCE LIST IN BATCH ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TRAINEE LIST (${filteredStudents.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filter trainee name or ID...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BentoNavy, unfocusedBorderColor = CyberBorder)
            )
        }

        if (filteredStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No students match '$searchQuery'"
                            else "No students enrolled in this batch yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add students to '${selectedBatch?.name ?: "Batch"}' from Student Management.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredStudents) { student ->
                val currentStatus = attendanceMap[student.studentId]
                val groupObj = groups.find { it.groupId == student.groupId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when (currentStatus) {
                                AttendanceStatus.PRESENT -> BentoMintOn.copy(alpha = 0.5f)
                                AttendanceStatus.ABSENT -> BentoCoralOn.copy(alpha = 0.5f)
                                else -> CyberBorder
                            },
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (currentStatus) {
                            AttendanceStatus.PRESENT -> BentoMintCard.copy(alpha = 0.3f)
                            AttendanceStatus.ABSENT -> BentoCoralCard.copy(alpha = 0.3f)
                            else -> CyberSurface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(photoUrl = student.photoUrl, size = 42.dp, iconSize = 20.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("ID: ${student.studentId} • Group: ${groupObj?.name ?: "Unassigned"}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = currentStatus == AttendanceStatus.PRESENT,
                                onClick = {
                                    if (DateUtils.isAfter(selectedDateString, DateUtils.getTodayString())) {
                                        Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
                                        return@FilterChip
                                    }
                                    attendanceMap[student.studentId] = AttendanceStatus.PRESENT
                                    repository.saveAttendanceBatch(
                                        date = selectedDateString,
                                        batchId = student.batchId,
                                        groupId = student.groupId,
                                        recordsMap = mapOf(student.studentId to AttendanceStatus.PRESENT),
                                        markedByUid = user.uid,
                                        markedByRole = "ADMIN",
                                        onComplete = { _, _ -> }
                                    )
                                },
                                label = { Text("PRESENT", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoMintCard,
                                    selectedLabelColor = BentoMintOn,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(100.dp)
                            )

                            FilterChip(
                                selected = currentStatus == AttendanceStatus.ABSENT,
                                onClick = {
                                    if (DateUtils.isAfter(selectedDateString, DateUtils.getTodayString())) {
                                        Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
                                        return@FilterChip
                                    }
                                    attendanceMap[student.studentId] = AttendanceStatus.ABSENT
                                    repository.saveAttendanceBatch(
                                        date = selectedDateString,
                                        batchId = student.batchId,
                                        groupId = student.groupId,
                                        recordsMap = mapOf(student.studentId to AttendanceStatus.ABSENT),
                                        markedByUid = user.uid,
                                        markedByRole = "ADMIN",
                                        onComplete = { _, _ -> }
                                    )
                                },
                                label = { Text("ABSENT", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoCoralCard,
                                    selectedLabelColor = BentoCoralOn,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(100.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminNoticesContent(
    batches: List<Batch>,
    groups: List<Group>,
    noticesList: List<Notice>,
    user: User,
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf(NoticeTargetType.ALL) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("PUBLISH ACADEMY NOTICE BOARD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notice Title", color = TextSecondary) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Notice Message", color = TextSecondary) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CyberButton(
                        text = "PUBLISH NOTICE",
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                val newNotice = Notice(
                                    title = title,
                                    message = message,
                                    targetType = targetType,
                                    createdBy = user.name
                                )
                                repository.createNotice(newNotice) { _, _ ->
                                    title = ""
                                    message = ""
                                    Toast.makeText(context, "Notice Published!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        containerColor = BentoCoralOn,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(noticesList) { notice ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notice.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                        Text(notice.message, fontSize = 12.sp, color = TextSecondary)
                    }
                    IconButton(onClick = { repository.deleteNotice(notice.noticeId) {} }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BentoCoralOn)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsContent(
    batches: List<Batch>,
    groups: List<Group>,
    students: List<Student>,
    attendanceList: List<AttendanceRecord>
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableStateOf("ALL_STUDENTS") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("ACADEMY ATTENDANCE REPORTS & PDF EXPORT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Generate official attendance PDF documents according to academy PRD rules.", fontSize = 11.sp, color = TextSecondary)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL_STUDENTS" to "All Students Comprehensive Report",
                    "WEEKLY" to "Weekly Training Attendance Report",
                    "MONTHLY" to "Monthly Attendance Summary",
                    "YEARLY" to "Yearly Attendance Audit",
                    "CUSTOM" to "Custom Date Range Report"
                ).forEach { (typeKey, typeLabel) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReportType = typeKey }
                            .border(1.dp, if (selectedReportType == typeKey) BentoCoralOn else CyberBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(typeLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Format: Official PDF Document", fontSize = 11.sp, color = TextMuted)
                            }
                            RadioButton(
                                selected = selectedReportType == typeKey,
                                onClick = { selectedReportType = typeKey },
                                colors = RadioButtonDefaults.colors(selectedColor = BentoCoralOn)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            CyberButton(
                text = "GENERATE & DOWNLOAD PDF REPORT",
                onClick = {
                    val batchName = batches.firstOrNull()?.name ?: "All Batches"
                    val groupName = groups.firstOrNull()?.name ?: "All Groups"

                    PdfReportGenerator.generateAndOpenPdfReport(
                        context = context,
                        reportTitle = selectedReportType.replace("_", " "),
                        batchName = batchName,
                        groupName = groupName,
                        dateRangeText = "Current Session (${DateUtils.getTodayString()})",
                        students = students,
                        attendanceList = attendanceList
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.PictureAsPdf,
                containerColor = BentoCoralOn,
                contentColor = Color.White
            )
        }
    }
}

