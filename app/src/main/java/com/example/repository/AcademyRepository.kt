package com.example.repository

import android.content.Context
import android.util.Log
import com.example.model.*
import com.example.util.LocalCacheManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.util.DateUtils

class AcademyRepository(private val context: Context? = null) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            val fs = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                fs.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w("AcademyRepo", "Firestore settings already applied or error: ${e.message}")
            }
            fs
        } catch (e: Exception) {
            Log.w("AcademyRepo", "Firestore not initialized, running in cached mode: ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // StateFlows for Real-time Reactive UI
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches: StateFlow<List<Batch>> = _batches.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _attendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendance: StateFlow<List<AttendanceRecord>> = _attendance.asStateFlow()

    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices: StateFlow<List<Notice>> = _notices.asStateFlow()

    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates.asStateFlow()

    private val _gallery = MutableStateFlow<List<GalleryItem>>(emptyList())
    val gallery: StateFlow<List<GalleryItem>> = _gallery.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementItem>>(emptyList())
    val achievements: StateFlow<List<AchievementItem>> = _achievements.asStateFlow()

    private val _developerInfo = MutableStateFlow<DeveloperInfo>(DeveloperInfo())
    val developerInfo: StateFlow<DeveloperInfo> = _developerInfo.asStateFlow()

    private val _appUpdateConfig = MutableStateFlow<AppUpdateConfig?>(null)
    val appUpdateConfig: StateFlow<AppUpdateConfig?> = _appUpdateConfig.asStateFlow()

    private val _isOffline = MutableStateFlow<Boolean>(false)

    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    init {
        loadDiskCacheOrSeed()
        setupFirestoreListeners()
    }

    private fun saveBatchesCache() = LocalCacheManager.saveList(context, "batches", _batches.value, Batch::class.java)
    private fun saveGroupsCache() = LocalCacheManager.saveList(context, "groups", _groups.value, Group::class.java)
    private fun saveStudentsCache() = LocalCacheManager.saveList(context, "students", _students.value, Student::class.java)
    private fun saveAttendanceCache() = LocalCacheManager.saveList(context, "attendance", _attendance.value, AttendanceRecord::class.java)
    private fun saveNoticesCache() = LocalCacheManager.saveList(context, "notices", _notices.value, Notice::class.java)
    private fun saveCertificatesCache() = LocalCacheManager.saveList(context, "certificates", _certificates.value, Certificate::class.java)
    private fun saveGalleryCache() = LocalCacheManager.saveList(context, "gallery", _gallery.value, GalleryItem::class.java)
    private fun saveAchievementsCache() = LocalCacheManager.saveList(context, "achievements", _achievements.value, AchievementItem::class.java)
    private fun saveUsersCache() = LocalCacheManager.saveList(context, "users", _users.value, User::class.java)
    private fun saveDevInfoCache() = LocalCacheManager.saveObject(context, "developer_info", _developerInfo.value, DeveloperInfo::class.java)
    private fun saveAppUpdateCache() {
        val config = _appUpdateConfig.value
        if (config != null) {
            LocalCacheManager.saveObject(context, "app_update_config", config, AppUpdateConfig::class.java)
        }
    }

    private fun loadDiskCacheOrSeed() {
        val cachedBatches = LocalCacheManager.loadList(context, "batches", Batch::class.java)
        val cachedGroups = LocalCacheManager.loadList(context, "groups", Group::class.java)
        val cachedStudents = LocalCacheManager.loadList(context, "students", Student::class.java)
        val cachedAttendance = LocalCacheManager.loadList(context, "attendance", AttendanceRecord::class.java)
        val cachedNotices = LocalCacheManager.loadList(context, "notices", Notice::class.java)
        val cachedCertificates = LocalCacheManager.loadList(context, "certificates", Certificate::class.java)
        val cachedGallery = LocalCacheManager.loadList(context, "gallery", GalleryItem::class.java)
        val cachedAchievements = LocalCacheManager.loadList(context, "achievements", AchievementItem::class.java)
        val cachedUsers = LocalCacheManager.loadList(context, "users", User::class.java)
        val cachedDevInfo = LocalCacheManager.loadObject(context, "developer_info", DeveloperInfo::class.java)
        val cachedAppUpdate = LocalCacheManager.loadObject(context, "app_update_config", AppUpdateConfig::class.java)

        if (cachedBatches != null && cachedBatches.isNotEmpty()) _batches.value = cachedBatches
        if (cachedGroups != null && cachedGroups.isNotEmpty()) _groups.value = cachedGroups
        if (cachedStudents != null) _students.value = cachedStudents
        if (cachedAttendance != null) _attendance.value = cachedAttendance
        if (cachedNotices != null && cachedNotices.isNotEmpty()) _notices.value = cachedNotices
        if (cachedCertificates != null) _certificates.value = cachedCertificates
        if (cachedGallery != null) _gallery.value = cachedGallery
        if (cachedAchievements != null) _achievements.value = cachedAchievements
        if (cachedUsers != null) _users.value = cachedUsers
        if (cachedDevInfo != null) _developerInfo.value = cachedDevInfo
        if (cachedAppUpdate != null) _appUpdateConfig.value = cachedAppUpdate

        if (_batches.value.isEmpty() || _groups.value.isEmpty()) {
            seedInitialData()
        }
    }

    private fun seedInitialData() {
        val initialBatches = listOf(
            Batch(
                batchId = "batch_mp_police_2026",
                name = "MP Police Physical Batch 2026",
                type = "MP Police",
                startDate = "2026-01-15",
                startTime = "05:30 AM",
                schedule = "Mon - Sat (5:30 AM - 8:30 AM)",
                description = "800m running, Long Jump & Shot Put rigorous training program.",
                status = "ACTIVE"
            ),
            Batch(
                batchId = "batch_army_2026",
                name = "Army Physical Batch 2026",
                type = "Army",
                startDate = "2026-02-01",
                startTime = "05:00 AM",
                schedule = "Mon - Sat (5:00 AM - 8:00 AM)",
                description = "1600m high-endurance military drill prep.",
                status = "ACTIVE"
            ),
            Batch(
                batchId = "batch_defence_2026",
                name = "Defence Physical Batch 2026",
                type = "Defence",
                startDate = "2026-03-01",
                startTime = "06:00 AM",
                schedule = "Mon - Sat (6:00 AM - 8:30 AM)",
                description = "General physical fitness and physical examination readiness.",
                status = "CLOSED",
                closedAt = System.currentTimeMillis() - 864000000
            )
        )

        val initialGroups = listOf(
            Group(
                groupId = "group_mp_a",
                batchId = "batch_mp_police_2026",
                name = "Group A (Alpha)",
                leaderId = "leader_uid_201",
                leaderName = "Instructor Vikram Singh"
            ),
            Group(
                groupId = "group_mp_b",
                batchId = "batch_mp_police_2026",
                name = "Group B (Bravo)",
                leaderId = "leader_uid_202",
                leaderName = "Coach Rajesh Kumar"
            ),
            Group(
                groupId = "group_army_a",
                batchId = "batch_army_2026",
                name = "Army Squad 1",
                leaderId = "leader_uid_203",
                leaderName = "Havildar Manoj Verma"
            )
        )

        val initialUsers = emptyList<User>()
        val initialStudents = emptyList<Student>()
        val initialAttendance = emptyList<AttendanceRecord>()

        val initialNotices = listOf(
            Notice(
                noticeId = "notice_1",
                title = "Upcoming Physical Mock Examination",
                message = "All batches have mandatory timing measurement tomorrow at 6:00 AM sharp at the main sports stadium. Ensure proper academy uniform.",
                targetType = NoticeTargetType.ALL,
                createdBy = "Academy Director"
            ),
            Notice(
                noticeId = "notice_2",
                title = "MP Police Batch Special Sprint Session",
                message = "Group A and Group B will have 100m sprint training at 06:15 AM on Thursday.",
                targetType = NoticeTargetType.BATCH,
                batchId = "batch_mp_police_2026",
                createdBy = "Director Office"
            ),
            Notice(
                noticeId = "notice_3",
                title = "Group A Leader Announcement",
                message = "Please carry your personal hydration flasks and jump shoes for long-jump technique analysis.",
                targetType = NoticeTargetType.GROUP,
                batchId = "batch_mp_police_2026",
                groupId = "group_mp_a",
                createdBy = "Instructor Vikram Singh"
            )
        )

        _batches.value = initialBatches
        _groups.value = initialGroups
        _users.value = initialUsers
        _students.value = initialStudents
        _attendance.value = initialAttendance
        _notices.value = initialNotices

        // Seed initial data to Firestore if connected
        firestore?.let { fs ->
            initialBatches.forEach { fs.collection("batches").document(it.batchId).set(it) }
            initialGroups.forEach { fs.collection("groups").document(it.groupId).set(it) }
            initialNotices.forEach { fs.collection("notices").document(it.noticeId).set(it) }

            val devInfo = _developerInfo.value
            val devMap = mapOf(
                "name" to devInfo.name.ifBlank { "Sidharth Malviya" },
                "photoUrl" to devInfo.photoUrl,
                "roleTitle" to devInfo.roleTitle.ifBlank { "App Developer & Technical Lead" },
                "phone" to devInfo.phone.ifBlank { "7441197419" },
                "updatedAt" to System.currentTimeMillis()
            )
            fs.collection("settings").document("developer_info").set(devMap, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    private fun setupFirestoreListeners() {
        val fs = firestore ?: return

        try {
            fs.collection("batches").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isOffline.value = true
                    return@addSnapshotListener
                }
                _isOffline.value = false
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Batch::class.java) }
                    _batches.value = list
                    saveBatchesCache()
                }
            }

            fs.collection("groups").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Group::class.java) }
                    _groups.value = list
                    saveGroupsCache()
                }
            }

            fs.collection("students").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Student::class.java) }
                    _students.value = list
                    saveStudentsCache()
                }
            }

            fs.collection("attendance").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AttendanceRecord::class.java) }
                    _attendance.value = list
                    saveAttendanceCache()
                }
            }

            fs.collection("notices").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Notice::class.java) }
                    _notices.value = list
                    saveNoticesCache()
                }
            }

            fs.collection("certificates").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Certificate::class.java) }
                    _certificates.value = list
                    saveCertificatesCache()
                }
            }

            fs.collection("gallery").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(GalleryItem::class.java) }
                    _gallery.value = list
                    saveGalleryCache()
                }
            }

            fs.collection("achievements").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(AchievementItem::class.java) }
                    _achievements.value = list
                    saveAchievementsCache()
                }
            }

            fs.collection("settings").document("developer_info").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val current = _developerInfo.value
                    val name = snapshot.getString("name")?.ifBlank { null } ?: current.name.ifBlank { "Sidharth Malviya" }
                    val photoUrlStr = snapshot.getString("photoUrl")
                    val photoUrl = if (!photoUrlStr.isNullOrBlank()) photoUrlStr else current.photoUrl
                    val roleTitle = snapshot.getString("roleTitle")?.ifBlank { null } ?: current.roleTitle.ifBlank { "App Developer & Technical Lead" }
                    val phone = snapshot.getString("phone")?.ifBlank { null } ?: current.phone.ifBlank { "7441197419" }
                    val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    _developerInfo.value = DeveloperInfo(name, photoUrl, roleTitle, phone, updatedAt)
                    saveDevInfoCache()
                }
            }

            fs.collection("users").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val uid = doc.getString("uid") ?: doc.id
                            val email = doc.getString("email") ?: ""
                            val name = doc.getString("name") ?: ""
                            val roleStr = doc.getString("role") ?: "STUDENT"
                            val photoUrl = doc.getString("photoUrl") ?: ""
                            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STUDENT }
                            User(uid, email, role, name, photoUrl)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _users.value = list
                    saveUsersCache()
                }
            }

            fs.collection("app_updates").document("latest").addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(AppUpdateConfig::class.java)
                    if (config != null) {
                        _appUpdateConfig.value = config
                        saveAppUpdateCache()
                    }
                }
            }
        } catch (e: Exception) {
            _isOffline.value = true
            Log.e("AcademyRepo", "Firestore snapshot registration error: ${e.message}")
        }
    }

    fun pushAppUpdate(config: AppUpdateConfig, onComplete: (Boolean, String?) -> Unit) {
        _appUpdateConfig.value = config
        saveAppUpdateCache()
        val fs = firestore
        if (fs != null) {
            fs.collection("app_updates").document("latest").set(config)
                .addOnSuccessListener {
                    saveAppUpdateCache()
                    onComplete(true, null)
                }
                .addOnFailureListener { e ->
                    Log.w("AcademyRepo", "Firestore update push notice: ${e.message}")
                    saveAppUpdateCache()
                    onComplete(true, null)
                }
        } else {
            saveAppUpdateCache()
            onComplete(true, null)
        }
    }

    // --- User Firestore Operations ---
    fun registerUserInFirestore(user: User, onComplete: (Boolean, String?) -> Unit) {
        val currentUsers = _users.value.filter { it.uid != user.uid } + user
        _users.value = currentUsers

        val userMap = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "name" to user.name,
            "role" to user.role.name,
            "photoUrl" to user.photoUrl,
            "createdAt" to System.currentTimeMillis()
        )

        firestore?.collection("users")?.document(user.uid)?.set(userMap)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(true, e.message) }
            ?: onComplete(true, null)
    }

    fun updateUserInFirestore(user: User, onComplete: (Boolean) -> Unit = {}) {
        val currentUsers = _users.value.filter { it.uid != user.uid } + user
        _users.value = currentUsers

        val userMap = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "name" to user.name,
            "role" to user.role.name,
            "photoUrl" to user.photoUrl,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore?.collection("users")?.document(user.uid)?.set(userMap, com.google.firebase.firestore.SetOptions.merge())
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun fetchUserByUid(uid: String, onComplete: (User?) -> Unit) {
        val localUser = _users.value.find { it.uid == uid }
        val fs = firestore
        if (fs != null) {
            fs.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val email = doc.getString("email") ?: localUser?.email ?: ""
                        val name = doc.getString("name") ?: localUser?.name ?: ""
                        val roleStr = doc.getString("role") ?: localUser?.role?.name ?: "STUDENT"
                        val photoUrl = doc.getString("photoUrl") ?: localUser?.photoUrl ?: ""
                        val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STUDENT }
                        val user = User(uid, email, role, name, photoUrl)
                        _users.value = _users.value.filter { it.uid != uid } + user
                        onComplete(user)
                    } else {
                        onComplete(localUser)
                    }
                }
                .addOnFailureListener {
                    onComplete(localUser)
                }
        } else {
            onComplete(localUser)
        }
    }

    // --- Student Profile Operations ---
    fun getStudentByUid(uid: String): Student? {
        return _students.value.find { it.uid == uid }
    }

    fun fetchStudentByUid(uid: String, onComplete: (Student?) -> Unit) {
        val localStudent = _students.value.find { it.uid == uid }
        val fs = firestore
        if (fs != null) {
            fs.collection("students").whereEqualTo("uid", uid).get()
                .addOnSuccessListener { snapshot ->
                    val found = snapshot?.documents?.firstOrNull()?.toObject(Student::class.java) ?: localStudent
                    if (found != null) {
                        _students.value = _students.value.filter { it.uid != uid && it.studentId != found.studentId } + found
                    }
                    onComplete(found)
                }
                .addOnFailureListener {
                    onComplete(localStudent)
                }
        } else {
            onComplete(localStudent)
        }
    }

    fun completeOrUpdateStudentProfile(student: Student, onComplete: (Boolean) -> Unit) {
        val updatedList = _students.value.toMutableList()
        val index = updatedList.indexOfFirst {
            (it.uid.isNotBlank() && it.uid == student.uid) ||
            (it.studentId.isNotBlank() && it.studentId == student.studentId)
        }
        val updatedStudent = student.copy(profileCompleted = true, updatedAt = System.currentTimeMillis())

        if (index >= 0) {
            updatedList[index] = updatedStudent
        } else {
            updatedList.add(updatedStudent)
        }
        _students.value = updatedList

        val docId = updatedStudent.studentId.ifBlank { updatedStudent.uid }
        firestore?.collection("students")?.document(docId)
            ?.set(updatedStudent, com.google.firebase.firestore.SetOptions.merge())
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    // --- Admin Operations ---
    fun createBatch(batch: Batch, onComplete: (Boolean, String?) -> Unit) {
        val batchId = batch.batchId.ifBlank { "batch_${System.currentTimeMillis()}" }
        val newBatch = batch.copy(batchId = batchId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())

        _batches.value = _batches.value.filter { it.batchId != batchId } + newBatch

        firestore?.collection("batches")?.document(batchId)?.set(newBatch)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun updateBatch(batch: Batch, onComplete: (Boolean, String?) -> Unit) {
        val updatedBatch = batch.copy(updatedAt = System.currentTimeMillis())
        _batches.value = _batches.value.map { if (it.batchId == batch.batchId) updatedBatch else it }

        firestore?.collection("batches")?.document(batch.batchId)
            ?.set(updatedBatch, com.google.firebase.firestore.SetOptions.merge())
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteBatch(batchId: String, onComplete: (Boolean, String?) -> Unit) {
        // 1. Remove batch from local state flow
        _batches.value = _batches.value.filterNot { it.batchId == batchId }

        // 2. Unassign students belonging to this batch
        _students.value = _students.value.map { student ->
            if (student.batchId == batchId) {
                student.copy(batchId = "", groupId = "", updatedAt = System.currentTimeMillis())
            } else student
        }

        // 3. Remove groups belonging to this batch
        _groups.value = _groups.value.filterNot { it.batchId == batchId }

        // 4. Firestore operations
        if (firestore != null) {
            firestore?.collection("batches")?.document(batchId)?.delete()

            firestore?.collection("students")?.whereEqualTo("batchId", batchId)?.get()
                ?.addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.update(mapOf("batchId" to "", "groupId" to "", "updatedAt" to System.currentTimeMillis()))
                    }
                }

            firestore?.collection("groups")?.whereEqualTo("batchId", batchId)?.get()
                ?.addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                }
        }

        onComplete(true, null)
    }

    fun closeBatch(batchId: String, onComplete: (Boolean) -> Unit) {
        val updatedBatches = _batches.value.map {
            if (it.batchId == batchId) it.copy(status = "CLOSED", closedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            else it
        }
        _batches.value = updatedBatches

        firestore?.collection("batches")?.document(batchId)
            ?.update(mapOf("status" to "CLOSED", "closedAt" to System.currentTimeMillis(), "updatedAt" to System.currentTimeMillis()))
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun createGroup(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val groupId = group.groupId.ifBlank { "group_${System.currentTimeMillis()}" }
        val newGroup = group.copy(groupId = groupId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())

        _groups.value = _groups.value.filter { it.groupId != groupId } + newGroup

        firestore?.collection("groups")?.document(groupId)?.set(newGroup)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun updateGroup(group: Group, onComplete: (Boolean, String?) -> Unit) {
        val updatedGroup = group.copy(updatedAt = System.currentTimeMillis())
        _groups.value = _groups.value.map { if (it.groupId == group.groupId) updatedGroup else it }

        firestore?.collection("groups")?.document(group.groupId)
            ?.set(updatedGroup, com.google.firebase.firestore.SetOptions.merge())
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteGroup(groupId: String, onComplete: (Boolean, String?) -> Unit) {
        _groups.value = _groups.value.filterNot { it.groupId == groupId }

        // Unassign students from this group
        _students.value = _students.value.map { student ->
            if (student.groupId == groupId) {
                student.copy(groupId = "", updatedAt = System.currentTimeMillis())
            } else student
        }

        if (firestore != null) {
            firestore?.collection("groups")?.document(groupId)?.delete()

            firestore?.collection("students")?.whereEqualTo("groupId", groupId)?.get()
                ?.addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.update(mapOf("groupId" to "", "updatedAt" to System.currentTimeMillis()))
                    }
                }
        }

        onComplete(true, null)
    }

    fun assignGroupLeader(groupId: String, leaderId: String, leaderName: String, onComplete: (Boolean) -> Unit) {
        val now = System.currentTimeMillis()

        val nextLeaderId = leaderId.trim()
        val nextLeaderName = leaderName.trim()

        // Identify previous leader for this target group or cleared groups
        val targetGroup = _groups.value.find { it.groupId == groupId }
        val prevLeaderIds = mutableSetOf<String>()
        if (targetGroup != null && targetGroup.leaderId.isNotBlank()) {
            prevLeaderIds.add(targetGroup.leaderId)
        }

        val previousGroupsToClear = if (nextLeaderId.isNotBlank() || nextLeaderName.isNotBlank()) {
            _groups.value.filter { g ->
                g.groupId != groupId && (
                    (nextLeaderId.isNotBlank() && (g.leaderId == nextLeaderId || g.leaderId.equals(nextLeaderId, ignoreCase = true))) ||
                    (nextLeaderName.isNotBlank() && g.leaderName.equals(nextLeaderName, ignoreCase = true))
                )
            }
        } else emptyList()

        previousGroupsToClear.forEach { g ->
            if (g.leaderId.isNotBlank()) prevLeaderIds.add(g.leaderId)
        }

        // 1. Revert previous leaders to STUDENT if they won't lead any group anymore
        prevLeaderIds.forEach { oldLeaderId ->
            if (oldLeaderId != nextLeaderId) {
                val leadingOtherGroup = _groups.value.any { g ->
                    g.groupId != groupId && !previousGroupsToClear.any { pg -> pg.groupId == g.groupId } && g.leaderId == oldLeaderId
                }
                if (!leadingOtherGroup) {
                    val targetPrevStudent = _students.value.find { s ->
                        (s.uid.isNotBlank() && s.uid == oldLeaderId) ||
                        (s.studentId.isNotBlank() && s.studentId == oldLeaderId) ||
                        (targetGroup != null && targetGroup.leaderName.isNotBlank() && s.name.equals(targetGroup.leaderName, ignoreCase = true))
                    }
                    val prevUid = targetPrevStudent?.uid ?: oldLeaderId
                    val prevStudentId = targetPrevStudent?.studentId ?: ""

                    _students.value = _students.value.map { s ->
                        if ((prevUid.isNotBlank() && s.uid == prevUid) || (prevStudentId.isNotBlank() && s.studentId == prevStudentId)) {
                            s.copy(isLeader = false, updatedAt = now)
                        } else s
                    }

                    _users.value = _users.value.map { u ->
                        if (prevUid.isNotBlank() && u.uid == prevUid) u.copy(role = UserRole.STUDENT) else u
                    }

                    if (firestore != null) {
                        if (prevStudentId.isNotBlank()) {
                            firestore?.collection("students")?.document(prevStudentId)
                                ?.update(mapOf("isLeader" to false, "updatedAt" to now))
                        }
                        if (prevUid.isNotBlank()) {
                            firestore?.collection("users")?.document(prevUid)
                                ?.update(mapOf("role" to "STUDENT", "updatedAt" to now))
                        }
                    }
                }
            }
        }

        // 2. Update groups state
        val updatedGroups = _groups.value.map { g ->
            if (g.groupId == groupId) {
                g.copy(leaderId = nextLeaderId, leaderName = nextLeaderName, updatedAt = now)
            } else if (previousGroupsToClear.any { it.groupId == g.groupId }) {
                g.copy(leaderId = "", leaderName = "", updatedAt = now)
            } else {
                g
            }
        }
        _groups.value = updatedGroups

        if (firestore != null) {
            firestore?.collection("groups")?.document(groupId)
                ?.update(mapOf("leaderId" to nextLeaderId, "leaderName" to nextLeaderName, "updatedAt" to now))

            previousGroupsToClear.forEach { prevG ->
                firestore?.collection("groups")?.document(prevG.groupId)
                    ?.update(mapOf("leaderId" to "", "leaderName" to "", "updatedAt" to now))
            }
        }

        // 3. Promote new student to LEADER if nextLeaderId or nextLeaderName is provided
        if (nextLeaderId.isNotBlank() || nextLeaderName.isNotBlank()) {
            val assignedStudent = _students.value.find { s ->
                (s.uid.isNotBlank() && s.uid == nextLeaderId) ||
                (s.studentId.isNotBlank() && s.studentId == nextLeaderId) ||
                (nextLeaderName.isNotBlank() && s.name.equals(nextLeaderName, ignoreCase = true))
            }

            if (assignedStudent != null) {
                val leaderUid = assignedStudent.uid
                val leaderStudentId = assignedStudent.studentId
                val targetGroupId = if (groupId.isNotBlank()) groupId else assignedStudent.groupId

                _students.value = _students.value.map { s ->
                    if (s.studentId == leaderStudentId || (leaderUid.isNotBlank() && s.uid == leaderUid)) {
                        s.copy(isLeader = true, groupId = targetGroupId, updatedAt = now)
                    } else s
                }

                if (leaderUid.isNotBlank()) {
                    _users.value = _users.value.map { u ->
                        if (u.uid == leaderUid) u.copy(role = UserRole.GROUP_LEADER) else u
                    }
                }

                if (firestore != null) {
                    if (leaderStudentId.isNotBlank()) {
                        firestore?.collection("students")?.document(leaderStudentId)
                            ?.update(mapOf("isLeader" to true, "groupId" to targetGroupId, "updatedAt" to now))
                    }
                    if (leaderUid.isNotBlank()) {
                        firestore?.collection("users")?.document(leaderUid)
                            ?.update(mapOf("role" to "GROUP_LEADER", "updatedAt" to now))
                    }
                }
            }
        }

        onComplete(true)
    }

    fun removeLeaderRoleFromStudent(student: Student, onComplete: (Boolean) -> Unit) {
        val groupsAssigned = _groups.value.filter { g ->
            (student.uid.isNotBlank() && g.leaderId == student.uid) ||
            (student.studentId.isNotBlank() && g.leaderId == student.studentId) ||
            (g.leaderName.equals(student.name, ignoreCase = true) && g.leaderName.isNotBlank())
        }

        groupsAssigned.forEach { g ->
            assignGroupLeader(g.groupId, "", "", {})
        }

        val now = System.currentTimeMillis()
        _students.value = _students.value.map { s ->
            if (s.studentId == student.studentId || (student.uid.isNotBlank() && s.uid == student.uid)) {
                s.copy(isLeader = false, updatedAt = now)
            } else s
        }
        _users.value = _users.value.map { u ->
            if (student.uid.isNotBlank() && u.uid == student.uid) u.copy(role = UserRole.STUDENT) else u
        }

        if (firestore != null) {
            if (student.studentId.isNotBlank()) {
                firestore?.collection("students")?.document(student.studentId)
                    ?.update(mapOf("isLeader" to false, "updatedAt" to now))
            }
            if (student.uid.isNotBlank()) {
                firestore?.collection("users")?.document(student.uid)
                    ?.update(mapOf("role" to "STUDENT", "updatedAt" to now))
            }
        }

        onComplete(true)
    }

    fun removeStudentFromBatch(studentId: String, onComplete: (Boolean) -> Unit) {
        val currentList = _students.value.map {
            if (it.studentId == studentId) it.copy(batchId = "", groupId = "", updatedAt = System.currentTimeMillis())
            else it
        }
        _students.value = currentList

        firestore?.collection("students")?.document(studentId)
            ?.update(mapOf("batchId" to "", "groupId" to "", "updatedAt" to System.currentTimeMillis()))
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun addOrUpdateStudent(student: Student, onComplete: (Boolean, String?) -> Unit) {
        val studentId = student.studentId.ifBlank { "STU_${(1000..9999).random()}" }
        val updatedStudent = student.copy(studentId = studentId, updatedAt = System.currentTimeMillis())

        val currentList = _students.value.toMutableList()
        val index = currentList.indexOfFirst { it.studentId == studentId || (it.uid.isNotBlank() && it.uid == student.uid) }
        if (index >= 0) {
            currentList[index] = updatedStudent
        } else {
            currentList.add(updatedStudent)
        }
        _students.value = currentList

        firestore?.collection("students")?.document(studentId)?.set(updatedStudent)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun moveStudentGroup(studentId: String, newBatchId: String, newGroupId: String, onComplete: (Boolean) -> Unit) {
        val currentList = _students.value.map {
            if (it.studentId == studentId) it.copy(batchId = newBatchId, groupId = newGroupId, updatedAt = System.currentTimeMillis())
            else it
        }
        _students.value = currentList

        firestore?.collection("students")?.document(studentId)
            ?.update(mapOf("batchId" to newBatchId, "groupId" to newGroupId, "updatedAt" to System.currentTimeMillis()))
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun blockStudent(studentId: String, studentUid: String, block: Boolean, onComplete: (Boolean, String?) -> Unit) {
        val newStatus = if (block) "BLOCKED" else "ACTIVE"

        // Update local state flows in real-time
        _students.value = _students.value.map {
            if (it.studentId == studentId || (studentUid.isNotBlank() && it.uid == studentUid)) {
                it.copy(status = newStatus, updatedAt = System.currentTimeMillis())
            } else it
        }

        if (studentUid.isNotBlank()) {
            _users.value = _users.value.map {
                if (it.uid == studentUid) {
                    it.copy(isBlocked = block, updatedAt = System.currentTimeMillis())
                } else it
            }
        }

        // Realtime Firestore sync
        val docId = studentId.ifBlank { studentUid }
        if (docId.isNotBlank()) {
            firestore?.collection("students")?.document(docId)
                ?.update(mapOf("status" to newStatus, "updatedAt" to System.currentTimeMillis()))
        }
        if (studentUid.isNotBlank()) {
            firestore?.collection("users")?.document(studentUid)
                ?.update(mapOf("isBlocked" to block, "updatedAt" to System.currentTimeMillis()))
        }

        onComplete(true, null)
    }

    fun deleteStudent(studentId: String, studentUid: String, onComplete: (Boolean, String?) -> Unit) {
        // 1. Remove from local reactive StateFlows in real-time
        _students.value = _students.value.filterNot {
            it.studentId == studentId || (studentUid.isNotBlank() && it.uid == studentUid)
        }

        if (studentUid.isNotBlank()) {
            _users.value = _users.value.filterNot { it.uid == studentUid }
        }

        _attendance.value = _attendance.value.filterNot {
            it.studentId == studentId || (studentUid.isNotBlank() && it.studentUid == studentUid)
        }

        // 2. Realtime Firebase Firestore Cleanup
        if (firestore != null) {
            // Delete student record
            val studentDocId = studentId.ifBlank { studentUid }
            if (studentDocId.isNotBlank()) {
                firestore?.collection("students")?.document(studentDocId)?.delete()
            }
            if (studentUid.isNotBlank() && studentUid != studentDocId) {
                firestore?.collection("students")?.document(studentUid)?.delete()
            }

            // Delete user user auth document
            if (studentUid.isNotBlank()) {
                firestore?.collection("users")?.document(studentUid)?.delete()
            }

            // Query & clear associated attendance records for this student
            val attQuery = if (studentId.isNotBlank()) {
                firestore?.collection("attendance")?.whereEqualTo("studentId", studentId)
            } else {
                firestore?.collection("attendance")?.whereEqualTo("studentUid", studentUid)
            }

            attQuery?.get()?.addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
                onComplete(true, null)
            }?.addOnFailureListener {
                onComplete(true, null) // fallback
            } ?: onComplete(true, null)
        } else {
            onComplete(true, null)
        }
    }

    // --- Attendance Operations ---
    fun saveAttendanceBatch(
        date: String,
        batchId: String,
        groupId: String,
        recordsMap: Map<String, AttendanceStatus>, // studentId -> PRESENT/ABSENT
        markedByUid: String,
        markedByRole: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val currentAttendance = _attendance.value.toMutableList()

        recordsMap.forEach { (studentId, status) ->
            val studentObj = _students.value.find { it.studentId == studentId }
            val existingIndex = currentAttendance.indexOfFirst {
                it.studentId == studentId && it.date == date
            }

            val docId = if (existingIndex >= 0) currentAttendance[existingIndex].attendanceId else "att_${studentId}_${date.replace("-", "")}"
            val record = AttendanceRecord(
                attendanceId = docId,
                studentId = studentId,
                studentUid = studentObj?.uid ?: "",
                batchId = if (batchId.isNotBlank()) batchId else (studentObj?.batchId ?: ""),
                groupId = if (groupId.isNotBlank()) groupId else (studentObj?.groupId ?: ""),
                date = date,
                status = status,
                markedBy = markedByUid,
                markedByRole = markedByRole,
                createdAt = if (existingIndex >= 0) currentAttendance[existingIndex].createdAt else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                updatedBy = markedByUid
            )

            if (existingIndex >= 0) {
                currentAttendance[existingIndex] = record
            } else {
                currentAttendance.add(record)
            }

            // Sync to Firestore
            firestore?.collection("attendance")?.document(docId)?.set(record)
        }

        _attendance.value = currentAttendance
        onComplete(true, null)
    }

    // --- Notice Operations ---
    fun createNotice(notice: Notice, onComplete: (Boolean, String?) -> Unit) {
        val noticeId = notice.noticeId.ifBlank { "notice_${System.currentTimeMillis()}" }
        val newNotice = notice.copy(noticeId = noticeId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())

        _notices.value = listOf(newNotice) + _notices.value

        firestore?.collection("notices")?.document(noticeId)?.set(newNotice)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteNotice(noticeId: String, onComplete: (Boolean) -> Unit) {
        _notices.value = _notices.value.filter { it.noticeId != noticeId }

        firestore?.collection("notices")?.document(noticeId)?.delete()
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    // --- Cloudinary Media Operations ---
    fun addCertificate(certificate: Certificate, onComplete: (Boolean, String?) -> Unit) {
        val certId = certificate.certificateId.ifBlank { "cert_${System.currentTimeMillis()}" }
        val newCert = certificate.copy(certificateId = certId, createdAt = System.currentTimeMillis())

        _certificates.value = listOf(newCert) + _certificates.value.filter { it.certificateId != certId }

        firestore?.collection("certificates")?.document(certId)?.set(newCert)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteCertificate(certificateId: String, onComplete: (Boolean) -> Unit) {
        _certificates.value = _certificates.value.filter { it.certificateId != certificateId }
        firestore?.collection("certificates")?.document(certificateId)?.delete()
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun addGalleryItem(item: GalleryItem, onComplete: (Boolean, String?) -> Unit) {
        val itemId = item.itemId.ifBlank { "gallery_${System.currentTimeMillis()}" }
        val newItem = item.copy(itemId = itemId, createdAt = System.currentTimeMillis())

        _gallery.value = listOf(newItem) + _gallery.value.filter { it.itemId != itemId }

        firestore?.collection("gallery")?.document(itemId)?.set(newItem)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteGalleryItem(itemId: String, onComplete: (Boolean) -> Unit) {
        _gallery.value = _gallery.value.filter { it.itemId != itemId }
        firestore?.collection("gallery")?.document(itemId)?.delete()
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun addAchievement(achievement: AchievementItem, onComplete: (Boolean, String?) -> Unit) {
        val achId = achievement.achievementId.ifBlank { "ach_${System.currentTimeMillis()}" }
        val newAch = achievement.copy(achievementId = achId, createdAt = System.currentTimeMillis())

        _achievements.value = listOf(newAch) + _achievements.value.filter { it.achievementId != achId }

        firestore?.collection("achievements")?.document(achId)?.set(newAch)
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }

    fun deleteAchievement(achievementId: String, onComplete: (Boolean) -> Unit) {
        _achievements.value = _achievements.value.filter { it.achievementId != achievementId }
        firestore?.collection("achievements")?.document(achievementId)?.delete()
            ?.addOnSuccessListener { onComplete(true) }
            ?.addOnFailureListener { onComplete(true) }
            ?: onComplete(true)
    }

    fun updateStudentPhotoUrl(studentId: String, photoUrl: String, onComplete: (Boolean) -> Unit) {
        val student = _students.value.find { it.studentId == studentId }
        if (student != null) {
            val updated = student.copy(photoUrl = photoUrl, updatedAt = System.currentTimeMillis())
            _students.value = _students.value.map { if (it.studentId == studentId) updated else it }
            firestore?.collection("students")?.document(studentId)?.set(updated)

            if (student.uid.isNotBlank()) {
                firestore?.collection("users")?.document(student.uid)?.update("photoUrl", photoUrl)
            }
        }
        onComplete(true)
    }

    fun updateDeveloperInfo(info: DeveloperInfo, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        _developerInfo.value = info
        saveDevInfoCache()
        val map = mapOf(
            "name" to info.name,
            "photoUrl" to info.photoUrl,
            "roleTitle" to info.roleTitle,
            "phone" to info.phone,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore?.collection("settings")?.document("developer_info")
            ?.set(map, com.google.firebase.firestore.SetOptions.merge())
            ?.addOnSuccessListener { onComplete(true, null) }
            ?.addOnFailureListener { e -> onComplete(false, e.message) }
            ?: onComplete(true, null)
    }
}

