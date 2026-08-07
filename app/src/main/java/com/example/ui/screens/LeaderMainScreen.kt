package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.auth.AuthViewModel
import com.example.model.*
import com.example.repository.AcademyRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DateUtils
import java.util.Calendar

@Composable
fun LeaderMainScreen(
    user: User,
    authViewModel: AuthViewModel,
    repository: AcademyRepository
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: HOME, 1: MY GROUP, 2: ATTENDANCE, 3: NOTICES, 4: PROFILE

    val groups by repository.groups.collectAsState()
    val batches by repository.batches.collectAsState()
    val allStudents by repository.students.collectAsState()
    val usersList by repository.users.collectAsState()
    val attendanceList by repository.attendance.collectAsState()
    val noticesList by repository.notices.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val developerInfo by repository.developerInfo.collectAsState()

    val currentUser = remember(usersList, user) {
        usersList.find { it.uid == user.uid } ?: user
    }

    // Find assigned group for this Group Leader UID
    val assignedGroup = groups.find { it.leaderId == currentUser.uid } ?: groups.firstOrNull()
    val assignedBatch = batches.find { it.batchId == assignedGroup?.batchId }

    // STRICT SCOPE RESTRICTION: Only students in the assigned group
    val groupStudents = remember(allStudents, assignedGroup) {
        allStudents.filter { it.groupId == assignedGroup?.groupId }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                CyberHeaderCard(
                    title = currentUser.name,
                    subtitle = "Leader • ${assignedGroup?.name ?: "No Assigned Group"}",
                    role = UserRole.GROUP_LEADER,
                    photoUrl = currentUser.photoUrl,
                    isOffline = isOffline,
                    onLogoutClick = { authViewModel.logout() }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, CyberBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text("HOME", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleOn,
                        selectedTextColor = BentoPurpleOn,
                        indicatorColor = BentoPurpleCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Group") },
                    label = { Text("MY GROUP", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoNavy,
                        selectedTextColor = BentoNavy,
                        indicatorColor = BentoBlueCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Attendance") },
                    label = { Text("ATTENDANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoMintOn,
                        selectedTextColor = BentoMintOn,
                        indicatorColor = BentoMintCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = "Notices") },
                    label = { Text("NOTICES", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoCoralOn,
                        selectedTextColor = BentoCoralOn,
                        indicatorColor = BentoCoralCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("PROFILE", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleOn,
                        selectedTextColor = BentoPurpleOn,
                        indicatorColor = BentoPurpleCard
                    )
                )
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
                0 -> LeaderDashboardContent(
                    assignedGroup = assignedGroup,
                    assignedBatch = assignedBatch,
                    groupStudents = groupStudents,
                    attendanceList = attendanceList,
                    developerInfo = developerInfo,
                    onNavigateTab = { selectedTab = it }
                )
                1 -> LeaderGroupMembersContent(groupStudents = groupStudents)
                2 -> LeaderAttendanceContent(
                    user = user,
                    assignedGroup = assignedGroup,
                    assignedBatch = assignedBatch,
                    groupStudents = groupStudents,
                    attendanceList = attendanceList,
                    repository = repository
                )
                3 -> LeaderNoticesContent(
                    user = user,
                    assignedGroup = assignedGroup,
                    assignedBatch = assignedBatch,
                    noticesList = noticesList,
                    repository = repository
                )
                4 -> LeaderProfileContent(user = currentUser, assignedGroup = assignedGroup, authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun LeaderDashboardContent(
    assignedGroup: Group?,
    assignedBatch: Batch?,
    groupStudents: List<Student>,
    attendanceList: List<AttendanceRecord>,
    developerInfo: DeveloperInfo = DeveloperInfo(),
    onNavigateTab: (Int) -> Unit = {}
) {
    val today = DateUtils.getTodayString()
    val todayGroupAtt = attendanceList.filter { it.groupId == assignedGroup?.groupId && it.date == today }
    val todayPresent = todayGroupAtt.count { it.status == AttendanceStatus.PRESENT }
    val todayAbsent = todayGroupAtt.count { it.status == AttendanceStatus.ABSENT }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateTab(1) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoPurpleCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("AUTHORIZED GROUP SCOPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(assignedGroup?.name ?: "No Group Assigned", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                    Text("Batch: ${assignedBatch?.name ?: "N/A"}", fontSize = 12.sp, color = BentoPurpleOn.copy(alpha = 0.8f))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox("GROUP MEMBERS", "${groupStudents.size}", Icons.Default.Group, NeonCyan, Modifier.weight(1f), onClick = { onNavigateTab(1) })
                StatBox("TODAY PRESENT", "$todayPresent", Icons.Default.CheckCircle, NeonGreen, Modifier.weight(1f), onClick = { onNavigateTab(2) })
                StatBox("TODAY ABSENT", "$todayAbsent", Icons.Default.Cancel, NeonRed, Modifier.weight(1f), onClick = { onNavigateTab(2) })
            }
        }

        item {
            Text("GROUP MEMBERS LIST", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (groupStudents.isEmpty()) {
            item { EmptyStateCard("No students enrolled in your assigned group.") }
        } else {
            items(groupStudents) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(
                                photoUrl = student.photoUrl,
                                size = 38.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Ph: ${student.mobile}", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        BatchBadge(student.studentId, BentoNavy)
                    }
                }
            }
        }

        item {
            DeveloperCard(developerInfo = developerInfo)
        }
    }
}

@Composable
fun LeaderGroupMembersContent(groupStudents: List<Student>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("ASSIGNED GROUP MEMBERS (${groupStudents.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (groupStudents.isEmpty()) {
            item { EmptyStateCard("No members in assigned group.") }
        } else {
            items(groupStudents) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
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
                                ProfileAvatar(
                                    photoUrl = student.photoUrl,
                                    size = 40.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Father: ${student.fatherName}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            BatchBadge(student.studentId, BentoPurpleOn)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Father: ${student.fatherName}", fontSize = 11.sp, color = TextSecondary)
                            Text("Age: ${student.calculatedAge()} yrs", fontSize = 11.sp, color = BentoMintOn)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mobile: ${student.mobile}", fontSize = 11.sp, color = TextSecondary)
                            Text("City: ${student.city}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderAttendanceContent(
    user: User,
    assignedGroup: Group?,
    assignedBatch: Batch?,
    groupStudents: List<Student>,
    attendanceList: List<AttendanceRecord>,
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf(DateUtils.getTodayString()) }

    // Map of studentId -> AttendanceStatus (PRESENT/ABSENT)
    val attendanceMap = remember { mutableStateMapOf<String, AttendanceStatus>() }

    // Initialize or load existing attendance for selected date
    LaunchedEffect(selectedDate, groupStudents, attendanceList) {
        attendanceMap.clear()
        groupStudents.forEach { student ->
            val record = attendanceList.find { it.studentId == student.studentId && it.date == selectedDate }
            if (record != null) {
                attendanceMap[student.studentId] = record.status
            }
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedStr = DateUtils.formatDate(year, month, dayOfMonth)
            if (DateUtils.isAfter(pickedStr, DateUtils.getTodayString())) {
                Toast.makeText(context, "Attendance cannot be marked or viewed for future dates.", Toast.LENGTH_SHORT).show()
            } else {
                selectedDate = pickedStr
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("MARK GROUP ATTENDANCE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Spacer(modifier = Modifier.height(10.dp))

            // Date Picker Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ATTENDANCE DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(selectedDate, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                    }
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BentoNavy)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Mark All Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        groupStudents.forEach { attendanceMap[it.studentId] = AttendanceStatus.PRESENT }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoMintCard, contentColor = BentoMintOn),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("MARK ALL PRESENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        groupStudents.forEach { attendanceMap[it.studentId] = AttendanceStatus.ABSENT }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BentoCoralCard, contentColor = BentoCoralOn),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("MARK ALL ABSENT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (groupStudents.isEmpty()) {
            item { EmptyStateCard("No students in assigned group to mark attendance.") }
        } else {
            items(groupStudents) { student ->
                val status = attendanceMap[student.studentId]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            when (status) {
                                AttendanceStatus.PRESENT -> BentoMintOn.copy(alpha = 0.5f)
                                AttendanceStatus.ABSENT -> BentoCoralOn.copy(alpha = 0.5f)
                                else -> CyberBorder
                            },
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
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
                        Column {
                            Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ID: ${student.studentId}", fontSize = 11.sp, color = TextMuted)
                                if (status == null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• Not Marked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BentoCoralOn)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = status == AttendanceStatus.PRESENT,
                                onClick = { attendanceMap[student.studentId] = AttendanceStatus.PRESENT },
                                label = { Text("P", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoMintCard,
                                    selectedLabelColor = BentoMintOn,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )

                            FilterChip(
                                selected = status == AttendanceStatus.ABSENT,
                                onClick = { attendanceMap[student.studentId] = AttendanceStatus.ABSENT },
                                label = { Text("A", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BentoCoralCard,
                                    selectedLabelColor = BentoCoralOn,
                                    containerColor = CyberSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                CyberButton(
                    text = "SAVE & SYNCHRONIZE ATTENDANCE",
                    onClick = {
                        if (attendanceMap.isEmpty()) {
                            Toast.makeText(context, "Please mark attendance (P or A) for at least one student before saving.", Toast.LENGTH_SHORT).show()
                            return@CyberButton
                        }
                        if (assignedGroup != null && assignedBatch != null) {
                            repository.saveAttendanceBatch(
                                date = selectedDate,
                                batchId = assignedBatch.batchId,
                                groupId = assignedGroup.groupId,
                                recordsMap = attendanceMap.toMap(),
                                markedByUid = user.uid,
                                markedByRole = "GROUP_LEADER",
                                onComplete = { success, msg ->
                                    Toast.makeText(context, "Attendance synced to Firebase successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Save,
                    containerColor = BentoMintOn,
                    contentColor = Color.White
                )
            }
        }
    }
}

@Composable
fun LeaderNoticesContent(
    user: User,
    assignedGroup: Group?,
    assignedBatch: Batch?,
    noticesList: List<Notice>,
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var noticeTitle by remember { mutableStateOf("") }
    var noticeMessage by remember { mutableStateOf("") }

    val groupNotices = noticesList.filter {
        it.targetType == NoticeTargetType.GROUP && it.groupId == assignedGroup?.groupId
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("PUBLISH GROUP NOTICE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

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
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Notice Title", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleOn,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noticeMessage,
                        onValueChange = { noticeMessage = it },
                        label = { Text("Notice Message", color = TextSecondary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleOn,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CyberButton(
                        text = "PUBLISH TO MY GROUP",
                        onClick = {
                            if (noticeTitle.isNotBlank() && noticeMessage.isNotBlank() && assignedGroup != null) {
                                val notice = Notice(
                                    title = noticeTitle,
                                    message = noticeMessage,
                                    targetType = NoticeTargetType.GROUP,
                                    batchId = assignedGroup.batchId,
                                    groupId = assignedGroup.groupId,
                                    createdBy = user.name
                                )
                                repository.createNotice(notice) { _, _ ->
                                    noticeTitle = ""
                                    noticeMessage = ""
                                    Toast.makeText(context, "Group Notice Published!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        containerColor = BentoPurpleOn,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Send
                    )
                }
            }
        }

        item {
            Text("GROUP NOTICES HISTORY", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (groupNotices.isEmpty()) {
            item { EmptyStateCard("No group notices published yet.") }
        } else {
            items(groupNotices) { notice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(notice.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoPurpleOn)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notice.message, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderProfileContent(
    user: User,
    assignedGroup: Group?,
    authViewModel: AuthViewModel
) {
    var photoUrl by remember(user) { mutableStateOf(user.photoUrl) }
    var name by remember(user) { mutableStateOf(user.name) }
    var isEditing by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("GROUP LEADER PROFILE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Button(
                onClick = {
                    if (isEditing) {
                        name = user.name
                        photoUrl = user.photoUrl
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing) CyberSurfaceVariant else BentoPurpleOn,
                    contentColor = if (isEditing) TextSecondary else Color.White
                ),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEditing) "CANCEL" else "EDIT PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showSuccessMessage) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoMintOn, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BentoMintCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BentoMintOn, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profile picture & details updated successfully!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BentoMintOn)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(
                        photoUrl = photoUrl.ifBlank { "preset_leader" },
                        size = 64.dp,
                        iconSize = 32.dp,
                        showEditOverlay = isEditing,
                        onEditClick = if (isEditing) { { showAvatarPicker = true } } else null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(name.ifBlank { user.name }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(user.email, fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(role = UserRole.GROUP_LEADER)
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Leader Full Name", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BentoPurpleOn) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPurpleOn,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CyberButton(
                        text = "SAVE PROFILE & PICTURE",
                        onClick = {
                            authViewModel.updateUserProfile(user, photoUrl = photoUrl, name = name) {
                                isEditing = false
                                showSuccessMessage = true
                            }
                        },
                        containerColor = BentoPurpleOn,
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Save
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CyberBorder)
                Spacer(modifier = Modifier.height(12.dp))

                ProfileDetailRow("Assigned Group", assignedGroup?.name ?: "None")
                ProfileDetailRow("Leader UID", user.uid)
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            currentPhotoUrl = photoUrl,
            onAvatarSelected = { photoUrl = it },
            onDismiss = { showAvatarPicker = false }
        )
    }
}

