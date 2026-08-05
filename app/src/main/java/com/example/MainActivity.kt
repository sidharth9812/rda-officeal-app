package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import com.example.model.Student
import com.example.model.UserRole
import com.example.notifications.NotificationHelper
import com.example.ui.screens.*
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.RDAAcademyTheme
import com.example.update.GitHubUpdateManager
import com.example.update.UpdateDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            RDAAcademyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBackground
                ) {
                    RDAAcademyApp()
                }
            }
        }
    }
}

@Composable
fun RDAAcademyApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSplashFinished by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    // GitHub Auto Update Manager
    val updateManager = remember { GitHubUpdateManager(context) }
    val updateState by updateManager.updateState.collectAsState()

    LaunchedEffect(Unit) {
        // Automatically check for GitHub release updates on app start
        scope.launch {
            updateManager.checkForUpdates(silent = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isSplashFinished) {
            SplashScreen(onSplashFinished = { isSplashFinished = true })
        } else {
            when (val state = authState) {
                is AuthState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CyberBackground),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = com.example.ui.theme.BentoNavy
                        )
                    }
                }
                is AuthState.Unauthenticated, is AuthState.Error -> {
                    LoginScreen(authViewModel = authViewModel)
                }
                is AuthState.ProfileIncomplete -> {
                    ProfileSetupScreen(
                        authViewModel = authViewModel,
                        initialStudent = state.partialStudent
                    )
                }
                is AuthState.Authenticated -> {
                    val isLeaderUser = state.user.role == UserRole.GROUP_LEADER || (state.studentProfile != null && state.studentProfile.isLeader)
                    when {
                        state.user.role == UserRole.ADMIN -> {
                            AdminMainScreen(
                                user = state.user,
                                authViewModel = authViewModel,
                                repository = authViewModel.repository
                            )
                        }
                        isLeaderUser -> {
                            LeaderMainScreen(
                                user = state.user,
                                authViewModel = authViewModel,
                                repository = authViewModel.repository
                            )
                        }
                        else -> {
                            if (state.studentProfile != null && state.studentProfile.profileCompleted) {
                                StudentMainScreen(
                                    user = state.user,
                                    student = state.studentProfile,
                                    authViewModel = authViewModel,
                                    repository = authViewModel.repository
                                )
                            } else {
                                val initial = state.studentProfile ?: Student(
                                    uid = state.user.uid,
                                    name = state.user.name
                                )
                                ProfileSetupScreen(
                                    authViewModel = authViewModel,
                                    initialStudent = initial
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Update Dialog overlay
        UpdateDialog(
            updateState = updateState,
            updateManager = updateManager,
            onDismiss = { updateManager.dismissUpdate() }
        )
    }
}

