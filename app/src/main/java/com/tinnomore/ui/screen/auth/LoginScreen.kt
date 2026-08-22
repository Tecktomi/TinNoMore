package com.tinnomore.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinnomore.data.db.entity.User
import com.tinnomore.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (User) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val loginError  by authViewModel.loginError.collectAsState()
    val isLoading   by authViewModel.isLoading.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        currentUser?.let { onLoginSuccess(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / título
        Text(
            text       = "TinNoMore",
            fontSize   = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.primary
        )
        Text(
            text      = "Terapia adaptativa para el tinnitus",
            fontSize  = 15.sp,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value         = email,
            onValueChange = { email = it; authViewModel.clearError() },
            label         = { Text("Correo electrónico") },
            modifier      = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine    = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value               = password,
            onValueChange       = { password = it; authViewModel.clearError() },
            label               = { Text("Contraseña") },
            modifier            = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine          = true
        )

        loginError?.let { err ->
            Text(
                text     = err,
                color    = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick  = { authViewModel.login(email, password) },
            enabled  = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ayuda con cuentas demo
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Cuentas de demostración", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                DemoRow("Paciente",     "paciente@demo.com",     "1234")
                DemoRow("Especialista", "especialista@demo.com", "1234")
                DemoRow("Admin",        "admin@demo.com",        "admin")
            }
        }
    }
}

@Composable
private fun DemoRow(role: String, email: String, pass: String) {
    Text("• $role: $email / $pass", fontSize = 12.sp, color = Color.Gray)
}
