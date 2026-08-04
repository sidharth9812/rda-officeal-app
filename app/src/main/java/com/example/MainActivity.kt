package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthState
import com.example.auth.AuthViewModel
import com.example.model.Student
import com.example.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.RDAAcademyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    var isSplashFinished by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

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
