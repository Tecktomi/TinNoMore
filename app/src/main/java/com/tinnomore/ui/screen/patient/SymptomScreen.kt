package com.tinnomore.ui.screen.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.data.db.entity.SymptomEntry
import com.tinnomore.viewmodel.SymptomUiState
import com.tinnomore.viewmodel.SymptomViewModel
import java.text.SimpleDateFormat
import java.util.*

private val SymptomPrimary     = Color(0xFF1565C0)   // TinBlue
private val SymptomPrimaryDark = Color(0xFF003C8F)   // TinBlueDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomScreen(
    patientId: Long,
    onBack: () -> Unit,
    showBackButton: Boolean = false,
    topTabs: (@Composable () -> Unit)? = null,
    vm: SymptomViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val toast   by vm.toast.collectAsState()

    var showDialog       by remember { mutableStateOf(false) }
    var editingSymptom   by remember { mutableStateOf<SymptomEntry?>(null) }

    LaunchedEffect(patientId) { vm.loadSymptoms(patientId) }

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
                    title = { Text(if (topTabs != null) "Mis Registros" else "Registro de Síntomas") },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = SymptomPrimary,
                        titleContentColor = Color.White
                    )
                )
                topTabs?.invoke()
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick          = { editingSymptom = null; showDialog = true },
                containerColor   = SymptomPrimary
            ) {
                Icon(Icons.Default.Add, "Registrar síntoma", tint = Color.White)
            }
        },
        snackbarHost = {
            toast?.let { (isSuccess, msg) ->
                Snackbar(
                    modifier          = Modifier.padding(8.dp),
                    containerColor    = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    contentColor      = Color.White
                ) { Text(msg) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = uiState) {

                is SymptomUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is SymptomUiState.Success -> {
                    if (s.symptoms.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No tienes registros.", fontSize = 18.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { editingSymptom = null; showDialog = true }) {
                                Text("Crear mi primer registro")
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            items(s.symptoms, key = { it.id }) { symptom ->
                                SymptomCard(symptom = symptom) {
                                    editingSymptom = symptom
                                    showDialog = true
                                }
                            }
                        }
                    }
                }

                is SymptomUiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    if (showDialog) {
        SymptomDialog(
            existing  = editingSymptom,
            onDismiss = { showDialog = false },
            onSave    = { intensity, duration, sleep, concentration ->
                val e = editingSymptom
                if (e != null) {
                    vm.updateSymptom(
                        id                = e.id,
                        patientId         = patientId,
                        originalTimestamp = e.timestamp,
                        intensity         = intensity,
                        durationMinutes   = duration,
                        sleepImpact       = sleep,
                        concentrationImpact = concentration
                    )
                } else {
                    vm.saveSymptom(patientId, intensity, duration, sleep, concentration)
                }
                showDialog = false
            }
        )
    }
}

// ─── Tarjeta de síntoma ──────────────────────────────────────────────────────

@Composable
private fun SymptomCard(symptom: SymptomEntry, onEdit: () -> Unit) {
    val fmt     = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
    val canEdit = System.currentTimeMillis() - symptom.timestamp < 24 * 3_600_000L

    Card(
        modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fmt.format(Date(symptom.timestamp)),
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Intensidad: ", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    IntensityChip(symptom.intensity)
                }
                symptom.durationMinutes?.let {
                    Text("Duración: $it min", fontSize = 13.sp, color = Color.DarkGray)
                }
                symptom.sleepImpact?.let {
                    Text("Impacto sueño: $it/10", fontSize = 13.sp, color = Color.DarkGray)
                }
                symptom.concentrationImpact?.let {
                    Text("Concentración: $it/10", fontSize = 13.sp, color = Color.DarkGray)
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun IntensityChip(intensity: Int) {
    val color = when {
        intensity <= 3 -> Color(0xFF2E7D32)
        intensity <= 6 -> Color(0xFFE65100)
        else           -> Color(0xFFD32F2F)
    }
    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            "$intensity / 10",
            color      = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── Diálogo de registro / edición ──────────────────────────────────────────

@Composable
private fun SymptomDialog(
    existing: SymptomEntry?,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?, Int?, Int?) -> Unit
) {
    var intensity             by remember { mutableFloatStateOf(existing?.intensity?.toFloat() ?: 5f) }
    var intensityTouched      by remember { mutableStateOf(existing != null) }
    var durationText          by remember { mutableStateOf(existing?.durationMinutes?.toString() ?: "") }
    var sleepImpact           by remember { mutableFloatStateOf(existing?.sleepImpact?.toFloat() ?: 5f) }
    var concentrationImpact   by remember { mutableFloatStateOf(existing?.concentrationImpact?.toFloat() ?: 5f) }
    var showIntensityError    by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Editar registro" else "Registrar síntomas") },
        text  = {
            Column {
                Text("Intensidad del tinnitus *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Slider(
                    value          = intensity,
                    onValueChange  = {
                        intensity = it
                        intensityTouched = true
                        showIntensityError = false
                    },
                    valueRange     = 1f..10f,
                    steps          = 8,
                    modifier       = Modifier.fillMaxWidth()
                )
                Text(
                    if (intensityTouched) "${intensity.toInt()} / 10" else "No seleccionada",
                    color      = if (intensityTouched) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                if (showIntensityError) {
                    Text("La intensidad es obligatoria", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = durationText,
                    onValueChange = { durationText = it },
                    label         = { Text("Duración (minutos)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                Spacer(Modifier.height(12.dp))

                Text("Impacto en sueño", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Slider(
                    value = sleepImpact,
                    onValueChange = { sleepImpact = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text("${sleepImpact.toInt()} / 10", fontSize = 13.sp)

                Spacer(Modifier.height(8.dp))

                Text("Impacto en concentración", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Slider(
                    value = concentrationImpact,
                    onValueChange = { concentrationImpact = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text("${concentrationImpact.toInt()} / 10", fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!intensityTouched) { showIntensityError = true; return@Button }
                onSave(
                    intensity.toInt(),
                    durationText.toIntOrNull(),
                    sleepImpact.toInt(),
                    concentrationImpact.toInt()
                )
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
