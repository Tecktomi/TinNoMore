package com.tinnomore.ui.screen.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.data.db.entity.MedicationEntry
import com.tinnomore.viewmodel.MedicationUiState
import com.tinnomore.viewmodel.MedicationViewModel
import java.text.SimpleDateFormat
import java.util.*

private val MedicationPrimary = Color(0xFF00695C)   // TinTeal, coherente con "Audiometría"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    patientId: Long,
    topTabs: (@Composable () -> Unit)? = null,
    vm: MedicationViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val toast   by vm.toast.collectAsState()

    var showDialog        by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<MedicationEntry?>(null) }

    LaunchedEffect(patientId) { vm.loadMedications(patientId) }

    toast?.let { (_, msg) ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title  = { Text("Mis Registros") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MedicationPrimary,
                        titleContentColor = Color.White
                    )
                )
                topTabs?.invoke()
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { editingMedication = null; showDialog = true },
                containerColor = MedicationPrimary
            ) {
                Icon(Icons.Default.Add, "Registrar medicamento", tint = Color.White)
            }
        },
        snackbarHost = {
            toast?.let { (isSuccess, msg) ->
                Snackbar(
                    modifier       = Modifier.padding(8.dp),
                    containerColor = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    contentColor   = Color.White
                ) { Text(msg) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = uiState) {

                is MedicationUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is MedicationUiState.Success -> {
                    if (s.medications.isEmpty()) {
                        Column(
                            modifier            = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No tienes medicamentos registrados.", fontSize = 18.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { editingMedication = null; showDialog = true },
                                colors  = ButtonDefaults.buttonColors(containerColor = MedicationPrimary)
                            ) {
                                Text("Registrar mi primer medicamento")
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            items(s.medications, key = { it.id }) { medication ->
                                MedicationCard(
                                    medication = medication,
                                    onEdit     = { editingMedication = medication; showDialog = true },
                                    onDelete   = { vm.deleteMedication(medication) }
                                )
                            }
                        }
                    }
                }

                is MedicationUiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showDialog) {
        MedicationDialog(
            existing  = editingMedication,
            onDismiss = { showDialog = false },
            onSave    = { name, dose, notes ->
                val e = editingMedication
                if (e != null) {
                    vm.updateMedication(
                        id                = e.id,
                        patientId         = patientId,
                        originalTimestamp = e.timestamp,
                        name              = name,
                        dose              = dose,
                        notes             = notes
                    )
                } else {
                    vm.saveMedication(patientId, name, dose, notes)
                }
                showDialog = false
            }
        )
    }
}

// ─── Tarjeta de medicamento ──────────────────────────────────────────────────

@Composable
private fun MedicationCard(
    medication: MedicationEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt     = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
    val canEdit = System.currentTimeMillis() - medication.timestamp < 24 * 3_600_000L

    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Medication,
                contentDescription = null,
                tint     = MedicationPrimary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fmt.format(Date(medication.timestamp)),
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Text(medication.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                medication.dose?.let {
                    Text("Dosis: $it", fontSize = 13.sp, color = Color.DarkGray)
                }
                medication.notes?.let {
                    Text(it, fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─── Diálogo de registro / edición ──────────────────────────────────────────

@Composable
private fun MedicationDialog(
    existing: MedicationEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?) -> Unit
) {
    var name      by remember { mutableStateOf(existing?.name ?: "") }
    var dose      by remember { mutableStateOf(existing?.dose ?: "") }
    var notes     by remember { mutableStateOf(existing?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Editar medicamento" else "Registrar medicamento") },
        text  = {
            Column {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it; showError = false },
                    label         = { Text("Medicamento *") },
                    placeholder   = { Text("Ej. Betahistina") },
                    isError       = showError,
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
                if (showError) {
                    Text(
                        "El nombre del medicamento es obligatorio",
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = dose,
                    onValueChange = { dose = it },
                    label         = { Text("Dosis") },
                    placeholder   = { Text("Ej. 16 mg") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Observaciones") },
                    placeholder   = { Text("Motivo, efectos, etc.") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) { showError = true; return@Button }
                    onSave(name, dose, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedicationPrimary)
            ) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
