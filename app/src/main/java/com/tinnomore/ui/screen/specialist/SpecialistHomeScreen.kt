package com.tinnomore.ui.screen.specialist

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
import com.tinnomore.viewmodel.SpecialistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistHomeScreen(
    specialist: User?,
    onLogout: () -> Unit,
    vm: SpecialistViewModel = viewModel()
) {
    val filtered     by vm.filtered.collectAsState()
    val searchQuery  by vm.searchQuery.collectAsState()
    val selected     by vm.selected.collectAsState()

    // HU-05-2: si hay paciente seleccionado, mostrar detalle
    if (selected != null) {
        PatientDetailScreen(
            data    = selected!!,
            onBack  = { vm.clearSelected() },
            vm      = vm
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Especialista") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            specialist?.let {
                Text(
                    "Dr/a. ${it.name}",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 14.dp)
                )
            }

            // HU-05-5: búsqueda por nombre o RUT
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                label         = { Text("Buscar por nombre o RUT") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                modifier  = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // HU-05-4: ordenar alfabéticamente
            OutlinedButton(
                onClick  = { vm.sortAlphabetically() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SortByAlpha, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ordenar alfabéticamente")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "${filtered.size} paciente(s) asignado(s)",
                fontSize = 13.sp,
                color    = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            when {
                filtered.isEmpty() && searchQuery.isNotBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontró ningún paciente con ese criterio.", color = Color.Gray)
                    }
                }
                filtered.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay pacientes registrados.", color = Color.Gray)
                    }
                }
                else -> {
                    // HU-05-1: listado de pacientes asignados
                    LazyColumn {
                        items(filtered, key = { it.id }) { patient ->
                            PatientListCard(patient = patient, onClick = { vm.selectPatient(patient) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientListCard(patient: User, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con inicial
            Surface(
                color  = MaterialTheme.colorScheme.primary,
                shape  = MaterialTheme.shapes.medium,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        patient.name.first().uppercaseChar().toString(),
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 20.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("RUT: ${patient.rut}", fontSize = 13.sp, color = Color.Gray)
                Text(patient.email, fontSize = 12.sp, color = Color.Gray)
            }

            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
