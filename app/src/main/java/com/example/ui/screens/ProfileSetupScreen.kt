package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.auth.AuthViewModel
import com.example.model.Student
import com.example.ui.components.AvatarPickerDialog
import com.example.ui.components.CyberButton
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel,
    initialStudent: Student
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf(initialStudent.name) }
    var fatherName by remember { mutableStateOf(initialStudent.fatherName) }
    var dob by remember { mutableStateOf(initialStudent.dob.ifBlank { "2001-06-09" }) }
    var gender by remember { mutableStateOf(initialStudent.gender.ifBlank { "Male" }) }
    var mobile by remember { mutableStateOf(initialStudent.mobile.ifBlank { "9876543210" }) }
    var city by remember { mutableStateOf(initialStudent.city.ifBlank { "Indore" }) }
    var photoUrl by remember { mutableStateOf(initialStudent.photoUrl) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var targetExam by remember { mutableStateOf(initialStudent.targetExam.ifBlank { "MP Police Constable" }) }
    var targetRunTime by remember { mutableStateOf(initialStudent.targetRunTime.ifBlank { "02:40 min (800m)" }) }
    var targetLongJump by remember { mutableStateOf(initialStudent.targetLongJump.ifBlank { "15.0 ft" }) }
    var targetShotPut by remember { mutableStateOf(initialStudent.targetShotPut.ifBlank { "25.0 ft" }) }

    // Dynamic Age Calculation from DOB
    val calculatedAge = remember(dob) {
        val tempStudent = Student(dob = dob)
        tempStudent.calculatedAge()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(
                    photoUrl = photoUrl.ifBlank { "preset_runner" },
                    size = 72.dp,
                    iconSize = 36.dp,
                    showEditOverlay = true,
                    onEditClick = { showAvatarPicker = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "COMPLETE YOUR PROFILE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "First-time Student Setup • RDA Physical Academy",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
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
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = BentoPurpleOn) },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Date of Birth & Auto Age Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .clickable { datePickerDialog.show() }
                    ) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Date of Birth", color = TextSecondary) },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BentoNavy) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = CyberBorder,
                                disabledTextColor = TextPrimary,
                                disabledLabelColor = TextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Auto Calculated Age Bento Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoMintCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CALCULATED AGE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoMintOn.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (calculatedAge > 0) "$calculatedAge YRS" else "-- YRS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoMintOn
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gender Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Male", "Female", "Other").forEach { item ->
                        val isSelected = gender == item
                        Button(
                            onClick = { gender = item },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) BentoBlueCard else CyberSurfaceVariant,
                                contentColor = if (isSelected) BentoNavy else TextSecondary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BentoNavy) },
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

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CyberBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text("PHYSICAL & TRAINING GOALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BentoCoralOn)
                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(20.dp))

                CyberButton(
                    text = "SAVE & GO TO DASHBOARD",
                    onClick = {
                        val updated = initialStudent.copy(
                            name = fullName.ifBlank { initialStudent.name },
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
                            profileCompleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        authViewModel.updateStudentProfile(updated)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.CheckCircle,
                    containerColor = BentoNavy,
                    contentColor = Color.White
                )
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
}

