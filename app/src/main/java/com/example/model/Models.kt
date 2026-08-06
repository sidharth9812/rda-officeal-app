package com.example.model

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

enum class UserRole {
    ADMIN,
    GROUP_LEADER,
    STUDENT
}

data class User(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val name: String = "",
    val photoUrl: String = "",
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Student(
    val studentId: String = "",
    val uid: String = "",
    val name: String = "",
    val fatherName: String = "",
    val dob: String = "", // YYYY-MM-DD
    val gender: String = "Male",
    val mobile: String = "",
    val city: String = "",
    val photoUrl: String = "",
    val batchId: String = "",
    val groupId: String = "",
    val status: String = "ACTIVE", // ACTIVE, BLOCKED, INACTIVE
    val isLeader: Boolean = false,
    val profileCompleted: Boolean = false,
    val targetExam: String = "MP Police Constable",
    val targetRunTime: String = "02:40 min (800m)",
    val targetLongJump: String = "15.0 ft",
    val targetShotPut: String = "25.0 ft",
    val fitnessNotes: String = "Building stamina & physical agility",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isBlocked: Boolean get() = status.equals("BLOCKED", ignoreCase = true)

    fun calculatedAge(): Int {
        if (dob.isBlank()) return 0
        return try {
            val formatter = if (dob.contains("/")) DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            else DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = LocalDate.parse(dob, formatter)
            val currentDate = LocalDate.now()
            Period.between(birthDate, currentDate).years
        } catch (e: Exception) {
            0
        }
    }
}

data class Batch(
    val batchId: String = "",
    val name: String = "",
    val type: String = "Physical Training", // e.g. MP Police, Army, Defence
    val startDate: String = "",
    val startTime: String = "05:30 AM",
    val schedule: String = "Mon - Sat (5:30 AM - 8:00 AM)",
    val description: String = "",
    val status: String = "ACTIVE", // ACTIVE, CLOSED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null
)

data class Group(
    val groupId: String = "",
    val batchId: String = "",
    val name: String = "",
    val leaderId: String = "",
    val leaderName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT
}

data class AttendanceRecord(
    val attendanceId: String = "",
    val studentId: String = "",
    val studentUid: String = "",
    val batchId: String = "",
    val groupId: String = "",
    val date: String = "", // YYYY-MM-DD
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val markedBy: String = "",
    val markedByRole: String = "GROUP_LEADER",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
)

enum class NoticeTargetType {
    ALL,
    BATCH,
    GROUP
}

data class Notice(
    val noticeId: String = "",
    val title: String = "",
    val message: String = "",
    val targetType: NoticeTargetType = NoticeTargetType.ALL,
    val batchId: String = "",
    val groupId: String = "",
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

data class Certificate(
    val certificateId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val title: String = "",
    val imageUrl: String = "", // Cloudinary HTTPS URL
    val issueDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class GalleryItem(
    val itemId: String = "",
    val title: String = "",
    val category: String = "Training", // Training, Event, Parade, Exam
    val imageUrl: String = "", // Cloudinary HTTPS URL
    val uploadedBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

data class AchievementItem(
    val achievementId: String = "",
    val title: String = "",
    val studentName: String = "",
    val description: String = "",
    val imageUrl: String = "", // Cloudinary HTTPS URL
    val date: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class DeveloperInfo(
    val name: String = "Sidharth Malviya",
    val photoUrl: String = "", // Cloudinary HTTPS URL
    val roleTitle: String = "App Developer & Technical Lead",
    val updatedAt: Long = System.currentTimeMillis()
)

data class AppUpdateConfig(
    val configId: String = "latest",
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val title: String = "New App Update Available",
    val releaseNotes: String = "New features, performance enhancements & bug fixes.",
    val downloadUrl: String = "https://github.com/sidharth9812/rda-officeal-app/releases/latest",
    val isMandatory: Boolean = false,
    val active: Boolean = true,
    val pushedByAdmin: String = "Academy Admin",
    val updatedAt: Long = System.currentTimeMillis()
)

