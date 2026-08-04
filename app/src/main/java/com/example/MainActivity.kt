package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import com.example.model.Student
import com.example.model.UserRole
import com.example.notifications.NotificationHelper
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.RDAAcademyTheme
import com.example.update.GitHubUpdateManager
import com.example.update.ReleaseInfo

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
    val updateManager = remember { GitHubUpdateManager(context) }
    var availableRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(Unit) {
        val result = updateManager.checkForUpdate()
        result.onSuccess { release ->
            if (release != null) {
                availableRelease = release
                NotificationHelper.showUpdateNotification(
                    context,
                    "New Update Available: v${release.versionName}",
                    "Tap to download and update RDA Physical Academy app."
                )
            }
        }
    }

    var isSplashFinished by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    availableRelease?.let { release ->
        UpdateDialog(
            releaseInfo = release,
            updateManager = updateManager,
            onDismiss = { availableRelease = null }
        )
    }

    if (!isSplashFinished) {
        SplashScreen(onSplashFinished = { isSplashFinished = true })
    } else {
        when (val state = authState) {
            is AuthState.Loading -> {
                SplashScreen(onSplashFinished = {})
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
}
