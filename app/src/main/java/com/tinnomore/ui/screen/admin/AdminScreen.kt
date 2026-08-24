package com.tinnomore.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.data.db.entity.User
import com.tinnomore.data.db.entity.UserRole
import com.tinnomore.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    user: User?,
    onLogout: () -> Unit,
    vm: AdminViewModel = viewModel()
) {
    val allUsers by vm.allUsers.collectAsState(initial = emptyList())
    val specialists by vm.specialists.collectAsState(initial = emptyList())
    val patientSpecialistIds by vm.patientSpecialistIds.collectAsState(initial = emptyMap())
    var toDelete by remember { mutableStateOf<User?>(null) }
    var toAssign by remember { mutableStateOf<User?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Administrador") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF4A148C),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            Text(
                "Gestión de Usuarios",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "${allUsers.size} usuarios en el sistema",
                fontSize = 13.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Resumen por rol
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val patients    = allUsers.count { it.role == UserRole.PATIENT }
                val specialists = allUsers.count { it.role == UserRole.SPECIALIST }
                val admins      = allUsers.count { it.role == UserRole.ADMIN }
                RoleSummary(Modifier.weight(1f), "Pacientes",     patients,    Color(0xFF1565C0))
                RoleSummary(Modifier.weight(1f), "Especialistas", specialists, Color(0xFF00695C))
                RoleSummary(Modifier.weight(1f), "Admins",        admins,      Color(0xFF4A148C))
            }

            Spacer(Modifier.height(16.dp))

            // Lista de usuarios
            LazyColumn {
                items(allUsers, key = { it.id }) { u ->
                    UserCard(
                        u = u,
                        onDelete = { toDelete = u },
                        onAssign = { toAssign = u }.takeIf { u.role == UserRole.PATIENT }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación
    toDelete?.let { u ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminar usuario") },
            text  = { Text("¿Deseas eliminar la cuenta de ${u.name}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteUser(u); toDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { toDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de asignación de especialista(s) a un paciente
    toAssign?.let { patient ->
        val assignedIds = patientSpecialistIds[patient.id].orEmpty()
        AlertDialog(
            onDismissRequest = { toAssign = null },
            title = { Text("Asignar especialista a ${patient.name}") },
            text = {
                if (specialists.isEmpty()) {
                    Text("No hay especialistas registrados en el sistema.", color = Color.Gray)
                } else {
                    Column {
                        specialists.forEach { s ->
                            val isAssigned = assignedIds.contains(s.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAssigned,
                                    onCheckedChange = { checked ->
                                        if (checked) vm.assignSpecialist(patient.id, s.id)
                                        else vm.unassignSpecialist(patient.id, s.id)
                                    }
                                )
                                Column {
                                    Text(s.name, fontWeight = FontWeight.SemiBold)
                                    Text(s.email, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { toAssign = null }) { Text("Listo") }
            }
        )
    }
}

@Composable
private fun RoleSummary(modifier: Modifier, label: String, count: Int, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun UserCard(u: User, onDelete: () -> Unit, onAssign: (() -> Unit)? = null) {
    val roleColor = when (u.role) {
        UserRole.PATIENT    -> Color(0xFF1565C0)
        UserRole.SPECIALIST -> Color(0xFF00695C)
        UserRole.ADMIN      -> Color(0xFF4A148C)
    }
    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(u.name, fontWeight = FontWeight.SemiBold)
                Text("RUT: ${u.rut}", fontSize = 12.sp, color = Color.Gray)
                Text(u.email, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Surface(color = roleColor, shape = MaterialTheme.shapes.extraSmall) {
                    Text(
                        u.role.name,
                        color    = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (onAssign != null) {
                IconButton(onClick = onAssign) {
                    Icon(Icons.Default.PersonAdd, "Asignar especialista", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
