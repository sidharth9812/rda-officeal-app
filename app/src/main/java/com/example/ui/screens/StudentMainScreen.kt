package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@Composable
fun StudentMainScreen(
    user: User,
    student: Student,
    authViewModel: AuthViewModel,
    repository: AcademyRepository
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: HOME, 1: ATTENDANCE, 2: NOTICES, 3: PROFILE

    val batches by repository.batches.collectAsState()
    val groups by repository.groups.collectAsState()
    val studentsList by repository.students.collectAsState()
    val usersList by repository.users.collectAsState()
    val attendanceList by repository.attendance.collectAsState()
    val noticesList by repository.notices.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val developerInfo by repository.developerInfo.collectAsState()

    val currentStudent = remember(studentsList, student) {
        studentsList.find {
            (it.uid.isNotBlank() && it.uid == student.uid) ||
            (it.studentId.isNotBlank() && it.studentId == student.studentId)
        } ?: student
    }
    val currentUser = remember(usersList, user) {
        usersList.find { it.uid == user.uid } ?: user
    }

    val currentBatch = batches.find { it.batchId == currentStudent.batchId }
    val currentGroup = groups.find { it.groupId == currentStudent.groupId }

    // Student's personal attendance records
    val studentAttendance = remember(attendanceList, currentStudent) {
        attendanceList.filter { it.studentId == currentStudent.studentId || it.studentUid == currentStudent.uid }
            .sortedByDescending { it.date }
    }

    val presentCount = studentAttendance.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = studentAttendance.count { it.status == AttendanceStatus.ABSENT }
    val totalTrainingDays = studentAttendance.size
    val attendancePercentage = if (totalTrainingDays > 0) (presentCount.toFloat() / totalTrainingDays * 100).toInt() else 100

    val todayStr = LocalDate.now().toString()
    val todayRecord = studentAttendance.find { it.date == todayStr }

    // Relevant notices (ALL, Student Batch, Student Group)
    val eligibleNotices = remember(noticesList, currentStudent) {
        noticesList.filter { notice ->
            notice.targetType == NoticeTargetType.ALL ||
            (notice.targetType == NoticeTargetType.BATCH && notice.batchId == currentStudent.batchId) ||
            (notice.targetType == NoticeTargetType.GROUP && notice.groupId == currentStudent.groupId)
        }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                CyberHeaderCard(
                    title = currentStudent.name.ifBlank { currentUser.name },
                    subtitle = "${currentBatch?.name ?: "Batch Unassigned"} • ${currentGroup?.name ?: "Group Unassigned"}",
                    role = UserRole.STUDENT,
                    photoUrl = currentStudent.photoUrl.ifBlank { currentUser.photoUrl },
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
                    label = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoNavy,
                        selectedTextColor = BentoNavy,
                        indicatorColor = BentoBlueCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.EventAvailable, contentDescription = "Attendance") },
                    label = { Text("ATTENDANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoMintOn,
                        selectedTextColor = BentoMintOn,
                        indicatorColor = BentoMintCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = "Notices") },
                    label = { Text("NOTICES", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoPurpleOn,
                        selectedTextColor = BentoPurpleOn,
                        indicatorColor = BentoPurpleCard
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BentoCoralOn,
                        selectedTextColor = BentoCoralOn,
                        indicatorColor = BentoCoralCard
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
                0 -> StudentDashboardContent(
                    student = currentStudent,
                    batchName = currentBatch?.name ?: "Unassigned Batch",
                    groupName = currentGroup?.name ?: "Unassigned Group",
                    todayRecord = todayRecord,
                    attendancePercentage = attendancePercentage,
                    presentCount = presentCount,
                    absentCount = absentCount,
                    totalTrainingDays = totalTrainingDays,
                    notices = eligibleNotices,
                    developerInfo = developerInfo,
                    onNavigateTab = { selectedTab = it }
                )
                1 -> StudentAttendanceContent(studentAttendance = studentAttendance)
                2 -> StudentNoticesContent(notices = eligibleNotices)
                3 -> StudentProfileContent(
                    student = currentStudent,
                    currentBatch = currentBatch,
                    currentGroup = currentGroup,
                    authViewModel = authViewModel,
                    repository = repository
                )
            }
        }
    }
}

@Composable
fun StudentDashboardContent(
    student: Student,
    batchName: String,
    groupName: String,
    todayRecord: AttendanceRecord?,
    attendancePercentage: Int,
    presentCount: Int,
    absentCount: Int,
    totalTrainingDays: Int,
    notices: List<Notice>,
    developerInfo: DeveloperInfo = DeveloperInfo(),
    onNavigateTab: (Int) -> Unit = {}
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Today's Status Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateTab(1) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TODAY'S TRAINING ATTENDANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (todayRecord != null) "Marked as ${todayRecord.status}" else "Pending Attendance Marking",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    if (todayRecord != null) {
                        AttendanceBadge(status = todayRecord.status)
                    } else {
                        BatchBadge("PENDING", NeonYellow)
                    }
                }
            }
        }

        // Stats Grid (Bento Style)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    title = "ATTENDANCE %",
                    value = "$attendancePercentage%",
                    icon = Icons.Default.PieChart,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
                StatBox(
                    title = "PRESENT",
                    value = "$presentCount Days",
                    icon = Icons.Default.CheckCircle,
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
                StatBox(
                    title = "ABSENT",
                    value = "$absentCount Days",
                    icon = Icons.Default.Cancel,
                    accentColor = NeonRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
            }
        }

        // Training Schedule Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateTab(3) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoBlueCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("ASSIGNED TRAINING BATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoNavy.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(batchName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = BentoNavy, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(groupName, fontSize = 13.sp, color = BentoNavy.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Recent Notices Header
        item {
            Text("LATEST ACADEMY NOTICES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (notices.isEmpty()) {
            item {
                EmptyStateCard("No academy notices published yet.")
            }
        } else {
            items(notices.take(3)) { notice ->
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
                            Text(notice.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                            BatchBadge(notice.targetType.name, BentoPurpleOn)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(notice.message, fontSize = 12.sp, color = TextSecondary)
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
fun StudentAttendanceContent(studentAttendance: List<AttendanceRecord>) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM

    val filteredAttendance = remember(studentAttendance, selectedFilter) {
        val now = LocalDate.now()
        when (selectedFilter) {
            "THIS_WEEK" -> {
                val startOfWeek = now.minusDays(7)
                studentAttendance.filter {
                    try { LocalDate.parse(it.date) >= startOfWeek } catch (e: Exception) { true }
                }
            }
            "THIS_MONTH" -> {
                studentAttendance.filter {
                    try { LocalDate.parse(it.date).month == now.month && LocalDate.parse(it.date).year == now.year } catch (e: Exception) { true }
                }
            }
            "THIS_YEAR" -> {
                studentAttendance.filter {
                    try { LocalDate.parse(it.date).year == now.year } catch (e: Exception) { true }
                }
            }
            else -> studentAttendance
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("MY PERSONAL ATTENDANCE LOG", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mapOf(
                    "ALL" to "All",
                    "THIS_WEEK" to "This Week",
                    "THIS_MONTH" to "This Month",
                    "THIS_YEAR" to "This Year"
                ).forEach { (key, label) ->
                    val isSel = selectedFilter == key
                    Button(
                        onClick = { selectedFilter = key },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) BentoMintCard else CyberSurfaceVariant,
                            contentColor = if (isSel) BentoMintOn else TextSecondary
                        ),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filteredAttendance.isEmpty()) {
            item {
                EmptyStateCard("No attendance records for the selected filter.")
            }
        } else {
            items(filteredAttendance) { record ->
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(record.date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Marked by ${record.markedByRole}", fontSize = 11.sp, color = TextMuted)
                        }
                        AttendanceBadge(status = record.status)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentNoticesContent(notices: List<Notice>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("AUTHORIZED ACADEMY NOTICES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        if (notices.isEmpty()) {
            item {
                EmptyStateCard("No notices available for your batch/group.")
            }
        } else {
            items(notices) { notice ->
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
                            Text(notice.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BentoNavy)
                            BatchBadge(notice.targetType.name, BentoPurpleOn)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(notice.message, fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Published by ${notice.createdBy}", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileContent(
    student: Student,
    currentBatch: Batch?,
    currentGroup: Group?,
    authViewModel: AuthViewModel,
    repository: AcademyRepository
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Form states
    var name by remember(student) { mutableStateOf(student.name) }
    var fatherName by remember(student) { mutableStateOf(student.fatherName) }
    var dob by remember(student) { mutableStateOf(student.dob.ifBlank { "2001-01-01" }) }
    var gender by remember(student) { mutableStateOf(student.gender.ifBlank { "Male" }) }
    var mobile by remember(student) { mutableStateOf(student.mobile) }
    var city by remember(student) { mutableStateOf(student.city) }
    var photoUrl by remember(student) { mutableStateOf(student.photoUrl) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Goal states
    var targetExam by remember(student) { mutableStateOf(student.targetExam.ifBlank { "MP Police Constable" }) }
    var targetRunTime by remember(student) { mutableStateOf(student.targetRunTime.ifBlank { "02:40 min (800m)" }) }
    var targetLongJump by remember(student) { mutableStateOf(student.targetLongJump.ifBlank { "15.0 ft" }) }
    var targetShotPut by remember(student) { mutableStateOf(student.targetShotPut.ifBlank { "25.0 ft" }) }
    var fitnessNotes by remember(student) { mutableStateOf(student.fitnessNotes.ifBlank { "Building stamina & physical agility" }) }

    val calculatedAge = remember(dob) {
        Student(dob = dob).calculatedAge()
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedMonth = (month + 1).toString().padStart(2, '0')
            val formattedDay = dayOfMonth.toString().padStart(2, '0')
            dob = "$year-$formattedMonth-$formattedDay"
        },
        calendar.get(Calendar.YEAR) - 22,
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "EDIT PROFILE & GOALS" else "MY PROFILE & TRAINING GOALS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Button(
                    onClick = {
                        if (isEditing) {
                            // Reset changes
                            name = student.name
                            fatherName = student.fatherName
                            dob = student.dob
                            gender = student.gender
                            mobile = student.mobile
                            city = student.city
                            targetExam = student.targetExam
                            targetRunTime = student.targetRunTime
                            targetLongJump = student.targetLongJump
                            targetShotPut = student.targetShotPut
                            fitnessNotes = student.fitnessNotes
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditing) CyberSurfaceVariant else BentoNavy,
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
        }

        // Success Alert Banner
        if (showSuccessMessage) {
            item {
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BentoMintOn,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Profile & Training Goals successfully updated and synced with Firestore! ⚡",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoMintOn
                        )
                    }
                }
            }
        }

        // Header Card (Avatar & Status)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(
                        photoUrl = photoUrl.ifBlank { "preset_runner" },
                        size = 60.dp,
                        iconSize = 30.dp,
                        showEditOverlay = isEditing,
                        onEditClick = if (isEditing) { { showAvatarPicker = true } } else null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = name.ifBlank { student.name },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Student ID: ${student.studentId}",
                            fontSize = 12.sp,
                            color = BentoNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BatchBadge(
                            text = student.status,
                            color = if (student.status == "ACTIVE") BentoMintOn else BentoCoralOn
                        )
                    }
                }
            }
        }

        if (!isEditing) {
            // DISPLAY MODE: Personal Information Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "PERSONAL INFORMATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Icon(Icons.Default.Badge, contentDescription = null, tint = BentoNavy, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CyberBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileDetailRow("Father's Name", student.fatherName)
                        ProfileDetailRow("Date of Birth", "${student.dob} (${student.calculatedAge()} yrs)")
                        ProfileDetailRow("Gender", student.gender)
                        ProfileDetailRow("Mobile Number", student.mobile)
                        ProfileDetailRow("City", student.city)
                        ProfileDetailRow("Current Batch", currentBatch?.name ?: "Unassigned")
                        ProfileDetailRow("Current Group", currentGroup?.name ?: "Unassigned")
                    }
                }
            }

            // DISPLAY MODE: Training Goals Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "PHYSICAL & TRAINING GOALS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoCoralOn
                            )
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BentoCoralOn, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CyberBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        ProfileDetailRow("Target Exam / Course", student.targetExam)
                        ProfileDetailRow("Target 800m Run Time", student.targetRunTime)
                        ProfileDetailRow("Target Long Jump", student.targetLongJump)
                        ProfileDetailRow("Target Shot Put", student.targetShotPut)
                        ProfileDetailRow("Fitness Milestones", student.fitnessNotes)
                    }
                }
            }
        } else {
            // EDIT MODE FORM
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "UPDATE PERSONAL INFORMATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BentoNavy) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoNavy,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            label = { Text("Father's Name", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = BentoNavy) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoNavy,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // DOB Picker Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                                .background(CyberSurfaceVariant)
                                .clickable { datePickerDialog.show() }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BentoMintOn)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Date of Birth", fontSize = 10.sp, color = TextSecondary)
                                        Text("$dob ($calculatedAge yrs)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Gender Select
                        Text("Gender", fontSize = 11.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                val isSelected = gender.equals(g, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { gender = g },
                                    label = { Text(g, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BentoNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BentoMintOn) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoMintOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City / Hometown", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = BentoPurpleOn) },
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
                    }
                }
            }

            // EDIT MODE: TRAINING GOALS SECTION
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "UPDATE TRAINING & PHYSICAL GOALS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoCoralOn
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = targetExam,
                            onValueChange = { targetExam = it },
                            label = { Text("Target Exam / Course", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BentoCoralOn) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = targetRunTime,
                            onValueChange = { targetRunTime = it },
                            label = { Text("Target 800m Run Time", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = BentoCoralOn) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = targetLongJump,
                            onValueChange = { targetLongJump = it },
                            label = { Text("Target Long Jump (ft)", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = BentoCoralOn) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = targetShotPut,
                            onValueChange = { targetShotPut = it },
                            label = { Text("Target Shot Put (ft)", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.SportsBasketball, contentDescription = null, tint = BentoCoralOn) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = fitnessNotes,
                            onValueChange = { fitnessNotes = it },
                            label = { Text("Fitness Milestones / Focus Notes", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = BentoCoralOn) },
                            maxLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCoralOn,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SAVE & SYNC BUTTON
                        CyberButton(
                            text = "SAVE & SYNC WITH FIRESTORE",
                            onClick = {
                                val updatedStudent = student.copy(
                                    name = name.ifBlank { student.name },
                                    fatherName = fatherName,
                                    dob = dob,
                                    gender = gender,
                                    mobile = mobile,
                                    city = city,
                                    photoUrl = photoUrl,
                                    targetExam = targetExam,
                                    targetRunTime = targetRunTime,
                                    targetLongJump = targetLongJump,
                                    targetShotPut = targetShotPut,
                                    fitnessNotes = fitnessNotes,
                                    profileCompleted = true,
                                    updatedAt = System.currentTimeMillis()
                                )

                                authViewModel.updateStudentProfile(updatedStudent) { success ->
                                    isEditing = false
                                    showSuccessMessage = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.CloudUpload,
                            containerColor = BentoNavy,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }

        // Admin Notice Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Batch assignment and official attendance records are managed by Academy Administration.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
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

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value.ifBlank { "Not specified" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

