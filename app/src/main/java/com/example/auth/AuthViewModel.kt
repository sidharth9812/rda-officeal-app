package com.example.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.AcademyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User, val studentProfile: Student? = null) : AuthState()
    data class ProfileIncomplete(val user: User, val partialStudent: Student) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository: AcademyRepository = AcademyRepository(application)
    private val prefs = application.getSharedPreferences("rda_session_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
        observeRepositoryUpdates()
    }

    private fun observeRepositoryUpdates() {
        viewModelScope.launch {
            repository.students.collect { studentList ->
                val currentState = _authState.value
                if (currentState is AuthState.Authenticated && currentState.studentProfile != null && (currentState.user.role == UserRole.STUDENT || currentState.user.role == UserRole.GROUP_LEADER)) {
                    val currentStudent = currentState.studentProfile
                    val updatedStudent = studentList.find {
                        (it.uid.isNotBlank() && it.uid == currentStudent.uid) ||
                        (it.studentId.isNotBlank() && it.studentId == currentStudent.studentId)
                    }
                    if (updatedStudent != null) {
                        if (updatedStudent.isBlocked) {
                            clearSession()
                            _authState.value = AuthState.Error("Your student account has been BLOCKED by the Administrator.")
                        } else if (updatedStudent != currentStudent) {
                            val newRole = if (updatedStudent.isLeader) UserRole.GROUP_LEADER else currentState.user.role
                            val updatedUser = currentState.user.copy(
                                photoUrl = updatedStudent.photoUrl.ifBlank { currentState.user.photoUrl },
                                name = updatedStudent.name.ifBlank { currentState.user.name },
                                role = newRole
                            )
                            saveSession(updatedUser, updatedStudent)
                            _authState.value = AuthState.Authenticated(updatedUser, updatedStudent)
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.users.collect { userList ->
                val currentState = _authState.value
                if (currentState is AuthState.Authenticated && (currentState.user.role == UserRole.STUDENT || currentState.user.role == UserRole.GROUP_LEADER)) {
                    val currentUser = currentState.user
                    val updatedUser = userList.find { it.uid == currentUser.uid }
                    if (updatedUser != null) {
                        if (updatedUser.isBlocked) {
                            clearSession()
                            _authState.value = AuthState.Error("Your account has been BLOCKED by the Administrator.")
                        } else if (updatedUser != currentUser) {
                            val isLeaderNow = (updatedUser.role == UserRole.GROUP_LEADER)
                            val updatedStudent = currentState.studentProfile?.copy(
                                photoUrl = updatedUser.photoUrl.ifBlank { currentState.studentProfile.photoUrl },
                                name = updatedUser.name.ifBlank { currentState.studentProfile.name },
                                isLeader = isLeaderNow
                            )
                            saveSession(updatedUser, updatedStudent)
                            _authState.value = AuthState.Authenticated(updatedUser, updatedStudent)
                        }
                    }
                }
            }
        }
    }

    private fun saveSession(user: User, student: Student? = null) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("uid", user.uid)
            .putString("email", user.email)
            .putString("name", user.name)
            .putString("role", user.role.name)
            .putString("photo_url", user.photoUrl)
            .apply()

        if (student != null) {
            prefs.edit()
                .putString("student_id", student.studentId)
                .putString("father_name", student.fatherName)
                .putString("dob", student.dob)
                .putString("gender", student.gender)
                .putString("mobile", student.mobile)
                .putString("city", student.city)
                .putString("photo_url", student.photoUrl)
                .putString("batch_id", student.batchId)
                .putString("group_id", student.groupId)
                .putString("target_exam", student.targetExam)
                .putString("target_run_time", student.targetRunTime)
                .putString("target_long_jump", student.targetLongJump)
                .putString("target_shot_put", student.targetShotPut)
                .putString("fitness_notes", student.fitnessNotes)
                .putBoolean("profile_completed", student.profileCompleted)
                .apply()
        }
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun getSavedSession(): Pair<User, Student?>? {
        if (!prefs.getBoolean("is_logged_in", false)) return null
        val uid = prefs.getString("uid", "") ?: ""
        if (uid.isBlank()) return null

        val email = prefs.getString("email", "") ?: ""
        val name = prefs.getString("name", "User") ?: "User"
        val roleStr = prefs.getString("role", "STUDENT") ?: "STUDENT"
        val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.STUDENT }
        val photoUrl = prefs.getString("photo_url", "") ?: ""

        val user = User(uid = uid, email = email, role = role, name = name, photoUrl = photoUrl)

        val student = if (role == UserRole.STUDENT || role == UserRole.GROUP_LEADER) {
            val rawStudentId = prefs.getString("student_id", "") ?: ""
            val fatherName = prefs.getString("father_name", "") ?: ""
            val dob = prefs.getString("dob", "2001-01-01") ?: "2001-01-01"
            val gender = prefs.getString("gender", "Male") ?: "Male"
            val mobile = prefs.getString("mobile", "") ?: ""
            val city = prefs.getString("city", "") ?: ""
            val batchId = prefs.getString("batch_id", "") ?: ""
            val groupId = prefs.getString("group_id", "") ?: ""
            val targetExam = prefs.getString("target_exam", "MP Police Constable") ?: "MP Police Constable"
            val targetRunTime = prefs.getString("target_run_time", "02:40 min (800m)") ?: "02:40 min (800m)"
            val targetLongJump = prefs.getString("target_long_jump", "15.0 ft") ?: "15.0 ft"
            val targetShotPut = prefs.getString("target_shot_put", "25.0 ft") ?: "25.0 ft"
            val fitnessNotes = prefs.getString("fitness_notes", "Building stamina & physical agility") ?: "Building stamina & physical agility"
            val isCompleted = prefs.getBoolean("profile_completed", false)

            val studentId = rawStudentId.ifBlank { "STU_${uid.takeLast(6).uppercase()}" }

            Student(
                studentId = studentId,
                uid = uid,
                name = name,
                fatherName = fatherName,
                dob = dob,
                gender = gender,
                mobile = mobile,
                city = city,
                photoUrl = photoUrl,
                batchId = batchId.ifBlank { repository.batches.value.firstOrNull { it.status == "ACTIVE" }?.batchId ?: "batch_mp_police_2026" },
                groupId = groupId.ifBlank { repository.groups.value.firstOrNull()?.groupId ?: "group_mp_a" },
                status = "ACTIVE",
                isLeader = (role == UserRole.GROUP_LEADER),
                profileCompleted = isCompleted,
                targetExam = targetExam,
                targetRunTime = targetRunTime,
                targetLongJump = targetLongJump,
                targetShotPut = targetShotPut,
                fitnessNotes = fitnessNotes
            )
        } else null

        return Pair(user, student)
    }

    fun checkCurrentUser() {
        val savedSession = getSavedSession()
        if (savedSession != null) {
            val (savedUser, savedStudent) = savedSession
            if (savedStudent != null && !savedStudent.profileCompleted && savedUser.role == UserRole.STUDENT) {
                _authState.value = AuthState.ProfileIncomplete(savedUser, savedStudent)
            } else {
                _authState.value = AuthState.Authenticated(savedUser, savedStudent)
            }
        }

        val fbUser = auth?.currentUser
        if (fbUser != null) {
            repository.fetchUserByUid(fbUser.uid) { userFromFs ->
                val user = userFromFs ?: savedSession?.first ?: User(
                    uid = fbUser.uid,
                    email = fbUser.email ?: "",
                    role = UserRole.STUDENT,
                    name = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "Academy User"
                )
                resolveUserRoleAndProfile(user)
            }
        } else if (savedSession == null) {
            clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun resolveUserRoleAndProfile(user: User) {
        if (user.isBlocked) {
            clearSession()
            _authState.value = AuthState.Error("Your account has been BLOCKED by the Academy Administrator. Please contact support.")
            return
        }

        viewModelScope.launch {
            if (user.role == UserRole.STUDENT || user.role == UserRole.GROUP_LEADER) {
                repository.fetchStudentByUid(user.uid) { existingStudent ->
                    if (existingStudent != null && existingStudent.isBlocked) {
                        clearSession()
                        _authState.value = AuthState.Error("Your student account has been BLOCKED by the Academy Administrator.")
                        return@fetchStudentByUid
                    }

                    val savedSession = getSavedSession()
                    val savedStudentProfile = savedSession?.second

                    if (existingStudent == null) {
                        if (savedStudentProfile != null) {
                            repository.completeOrUpdateStudentProfile(savedStudentProfile) {}
                            saveSession(user, savedStudentProfile)
                            _authState.value = if (savedStudentProfile.profileCompleted) AuthState.Authenticated(user, savedStudentProfile) else AuthState.ProfileIncomplete(user, savedStudentProfile)
                        } else {
                            val newStudent = Student(
                                studentId = "STU_${user.uid.takeLast(6).uppercase()}",
                                uid = user.uid,
                                name = user.name,
                                batchId = repository.batches.value.firstOrNull { it.status == "ACTIVE" }?.batchId ?: "batch_mp_police_2026",
                                groupId = repository.groups.value.firstOrNull()?.groupId ?: "group_mp_a",
                                status = "ACTIVE",
                                isLeader = (user.role == UserRole.GROUP_LEADER),
                                profileCompleted = false
                            )
                            repository.completeOrUpdateStudentProfile(newStudent) {}
                            _authState.value = AuthState.ProfileIncomplete(user, newStudent)
                        }
                    } else if (!existingStudent.profileCompleted) {
                        val mergedStudent = if (savedStudentProfile != null && savedStudentProfile.profileCompleted) savedStudentProfile else existingStudent
                        _authState.value = if (mergedStudent.profileCompleted) AuthState.Authenticated(user, mergedStudent) else AuthState.ProfileIncomplete(user, mergedStudent)
                    } else {
                        val updatedStudent = if (user.role == UserRole.GROUP_LEADER && !existingStudent.isLeader) existingStudent.copy(isLeader = true) else existingStudent
                        saveSession(user, updatedStudent)
                        _authState.value = AuthState.Authenticated(user, updatedStudent)
                    }
                }
            } else {
                saveSession(user)
                _authState.value = AuthState.Authenticated(user)
            }
        }
    }

    fun registerWithEmail(
        name: String,
        email: String,
        pass: String,
        requestedRole: UserRole,
        securityCode: String = "",
        photoUrl: String = ""
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val role = if (requestedRole == UserRole.GROUP_LEADER) UserRole.STUDENT else requestedRole

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || pass.length < 6) {
            _authState.value = AuthState.Error("Please enter name, valid email, and password (min 6 characters).")
            return
        }

        if (role == UserRole.ADMIN) {
            val adminCodes = listOf("RDA2026", "ADMIN123", "RDAADMIN", "ADMIN2026")
            if (securityCode.trim().uppercase() !in adminCodes) {
                _authState.value = AuthState.Error("Invalid Security Passcode for ADMIN registration.")
                return
            }
        }

        _authState.value = AuthState.Loading

        val authInstance = auth
        if (authInstance != null) {
            authInstance.createUserWithEmailAndPassword(trimmedEmail, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: "user_${System.currentTimeMillis()}"
                    val newUser = User(
                        uid = uid,
                        email = trimmedEmail,
                        role = role,
                        name = trimmedName,
                        photoUrl = photoUrl
                    )
                    repository.registerUserInFirestore(newUser) { _, _ ->
                        resolveUserRoleAndProfile(newUser)
                    }
                }
                .addOnFailureListener { e ->
                    val rawMsg = e.message ?: ""
                    val errorMsg = when {
                        rawMsg.contains("already in use", ignoreCase = true) ||
                        rawMsg.contains("EMAIL_EXISTS", ignoreCase = true) -> "An account with this email already exists. Please login instead."
                        rawMsg.contains("weak", ignoreCase = true) -> "Password is too weak. Please use at least 6 characters."
                        rawMsg.contains("badly formatted", ignoreCase = true) -> "Invalid email address format."
                        else -> e.localizedMessage ?: "Registration failed. Please check your credentials."
                    }
                    _authState.value = AuthState.Error(errorMsg)
                }
        } else {
            _authState.value = AuthState.Error("Firebase Authentication service is currently unavailable.")
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please enter valid email and password.")
            return
        }

        _authState.value = AuthState.Loading

        val authInstance = auth
        if (authInstance != null) {
            authInstance.signInWithEmailAndPassword(trimmedEmail, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    repository.fetchUserByUid(uid) { userFromFs ->
                        val matchedUser = userFromFs ?: User(
                            uid = uid,
                            email = trimmedEmail,
                            role = UserRole.STUDENT,
                            name = trimmedEmail.substringBefore("@")
                        )
                        resolveUserRoleAndProfile(matchedUser)
                    }
                }
                .addOnFailureListener { e ->
                    val rawMsg = e.message ?: ""
                    val errorMsg = when {
                        rawMsg.contains("password", ignoreCase = true) ||
                        rawMsg.contains("INVALID_PASSWORD", ignoreCase = true) ||
                        rawMsg.contains("invalid-credential", ignoreCase = true) ||
                        rawMsg.contains("credential", ignoreCase = true) -> "Incorrect password. Please try again."

                        rawMsg.contains("user", ignoreCase = true) ||
                        rawMsg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                        rawMsg.contains("no user", ignoreCase = true) -> "No account found with this email. Please register first."

                        rawMsg.contains("badly formatted", ignoreCase = true) ||
                        rawMsg.contains("INVALID_EMAIL", ignoreCase = true) -> "Invalid email address format."

                        else -> e.localizedMessage ?: "Incorrect password. Please try again."
                    }
                    _authState.value = AuthState.Error(errorMsg)
                }
        } else {
            _authState.value = AuthState.Error("Firebase Authentication service is currently unavailable.")
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }

        val authInstance = auth
        if (authInstance != null) {
            authInstance.sendPasswordResetEmail(trimmedEmail)
                .addOnSuccessListener {
                    onResult(true, "Password reset email sent to $trimmedEmail. Please check your inbox.")
                }
                .addOnFailureListener { e ->
                    val rawMsg = e.message ?: ""
                    val errorMsg = when {
                        rawMsg.contains("user-not-found", ignoreCase = true) ||
                        rawMsg.contains("user not found", ignoreCase = true) ||
                        rawMsg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                        rawMsg.contains("no user", ignoreCase = true) -> "No account found registered with $trimmedEmail."

                        rawMsg.contains("invalid-email", ignoreCase = true) ||
                        rawMsg.contains("badly formatted", ignoreCase = true) -> "Invalid email address format."

                        else -> e.localizedMessage ?: "Failed to send password reset email. Please try again."
                    }
                    onResult(false, errorMsg)
                }
        } else {
            onResult(false, "Firebase Authentication service is currently unavailable.")
        }
    }

    fun completeStudentProfile(
        fullName: String,
        fatherName: String,
        dob: String,
        gender: String,
        mobile: String,
        city: String,
        photoUrl: String
    ) {
        val currentState = _authState.value
        val (user, partial) = when (currentState) {
            is AuthState.ProfileIncomplete -> Pair(currentState.user, currentState.partialStudent)
            is AuthState.Authenticated -> Pair(currentState.user, currentState.studentProfile ?: Student(uid = currentState.user.uid))
            else -> return
        }

        val completedStudent = partial.copy(
            name = fullName.ifBlank { user.name },
            fatherName = fatherName,
            dob = dob,
            gender = gender,
            mobile = mobile,
            city = city,
            photoUrl = photoUrl,
            profileCompleted = true
        )

        repository.completeOrUpdateStudentProfile(completedStudent) { success ->
            saveSession(user, completedStudent)
            _authState.value = AuthState.Authenticated(user, completedStudent)
        }
    }

    fun updateStudentProfile(updatedStudent: Student, onComplete: (Boolean) -> Unit = {}) {
        val currentState = _authState.value
        val user = when (currentState) {
            is AuthState.Authenticated -> currentState.user.copy(photoUrl = updatedStudent.photoUrl)
            is AuthState.ProfileIncomplete -> currentState.user.copy(photoUrl = updatedStudent.photoUrl)
            else -> User(uid = updatedStudent.uid, name = updatedStudent.name, role = UserRole.STUDENT, photoUrl = updatedStudent.photoUrl)
        }

        val finalStudent = updatedStudent.copy(
            profileCompleted = true,
            updatedAt = System.currentTimeMillis()
        )

        repository.completeOrUpdateStudentProfile(finalStudent) { success ->
            repository.updateUserInFirestore(user)
            saveSession(user, finalStudent)
            _authState.value = AuthState.Authenticated(user, finalStudent)
            onComplete(success)
        }
    }

    fun updateUserProfile(updatedUser: User, photoUrl: String = updatedUser.photoUrl, name: String = updatedUser.name, onComplete: (Boolean) -> Unit = {}) {
        val finalUser = updatedUser.copy(
            name = name.ifBlank { updatedUser.name },
            photoUrl = photoUrl,
            updatedAt = System.currentTimeMillis()
        )

        val currentState = _authState.value
        val currentStudent = if (currentState is AuthState.Authenticated) currentState.studentProfile else null

        val updatedStudent = currentStudent?.copy(
            name = finalUser.name,
            photoUrl = finalUser.photoUrl,
            updatedAt = System.currentTimeMillis()
        )

        repository.updateUserInFirestore(finalUser) { success ->
            if (updatedStudent != null) {
                repository.completeOrUpdateStudentProfile(updatedStudent) {
                    saveSession(finalUser, updatedStudent)
                    _authState.value = AuthState.Authenticated(finalUser, updatedStudent)
                    onComplete(true)
                }
            } else {
                saveSession(finalUser, null)
                _authState.value = AuthState.Authenticated(finalUser, null)
                onComplete(true)
            }
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            // ignore
        }
        clearSession()
        _authState.value = AuthState.Unauthenticated
    }
}
