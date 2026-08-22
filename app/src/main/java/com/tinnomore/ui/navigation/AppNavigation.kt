package com.tinnomore.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tinnomore.data.db.entity.UserRole
import com.tinnomore.ui.screen.admin.AdminScreen
import com.tinnomore.ui.screen.auth.LoginScreen
import com.tinnomore.ui.screen.patient.CrisisScreen
import com.tinnomore.ui.screen.patient.PatientMainScreen
import com.tinnomore.ui.screen.specialist.SpecialistHomeScreen
import com.tinnomore.util.NotchProcessor
import com.tinnomore.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object PatientMain    : Screen("patient_main?noiseType={noiseType}")   // v0.2: reemplaza PatientHome
    object Crisis         : Screen("crisis")
    object SpecialistHome : Screen("specialist_home")
    object Admin          : Screen("admin")
}

@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val currentUser      by authViewModel.currentUser.collectAsState()
    val isSessionLoading by authViewModel.isSessionLoading.collectAsState()
    // ── Mientras se verifica la sesión guardada, mostrar spinner ──────────
    if (isSessionLoading && currentUser == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ── Reaccionar a cambios de sesión (login, auto-login, logout) ────────
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val dest = when (currentUser!!.role) {
                UserRole.PATIENT    -> "patient_main"
                UserRole.SPECIALIST -> Screen.SpecialistHome.route
                UserRole.ADMIN      -> Screen.Admin.route
            }
            val current = navController.currentDestination?.route
            if (current == Screen.Login.route || current == null) {
                navController.navigate(dest) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        } else {
            // Logout: volver a login limpiando todo el backstack
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        // ── Login ─────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel  = authViewModel,
                onLoginSuccess = { user ->
                    val dest = when (user.role) {
                        UserRole.PATIENT    -> "patient_main"
                        UserRole.SPECIALIST -> Screen.SpecialistHome.route
                        UserRole.ADMIN      -> Screen.Admin.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Paciente: pantalla principal con barra inferior (v0.2) ────────
        composable(
            route = Screen.PatientMain.route,
            arguments = listOf(
                navArgument("noiseType") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val noiseTypeStr = backStackEntry.arguments?.getString("noiseType")
            val initialNoiseType = try {
                noiseTypeStr?.let { NotchProcessor.NoiseType.valueOf(it) }
            } catch (e: Exception) {
                null
            }

            PatientMainScreen(
                user             = currentUser,
                onCrisisClick    = { navController.navigate(Screen.Crisis.route) },
                onLogout         = { authViewModel.logout() },
                initialNoiseType = initialNoiseType
            )
        }

        // ── Crisis: pantalla modal completa ───────────────────────────────
        composable(Screen.Crisis.route) {
            CrisisScreen(
                patientId = currentUser?.id ?: 0L,
                onBack    = { navController.popBackStack() },
                onNavigateToTherapy = { noiseType: NotchProcessor.NoiseType? ->
                    val route = if (noiseType != null) {
                        "patient_main?noiseType=${noiseType.name}"
                    } else {
                        "patient_main"
                    }
                    navController.navigate(route) {
                        popUpTo("patient_main") { inclusive = true }
                    }
                }
            )
        }

        // ── Especialista ──────────────────────────────────────────────────
        composable(Screen.SpecialistHome.route) {
            SpecialistHomeScreen(
                specialist = currentUser,
                onLogout   = { authViewModel.logout() }
            )
        }

        // ── Administrador ─────────────────────────────────────────────────
        composable(Screen.Admin.route) {
            AdminScreen(
                user     = currentUser,
                onLogout = { authViewModel.logout() }
            )
        }
    }
}
