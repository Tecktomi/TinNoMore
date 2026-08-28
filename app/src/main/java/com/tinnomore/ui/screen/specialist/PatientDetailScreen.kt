package com.tinnomore.ui.screen.specialist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinnomore.data.db.entity.SymptomEntry
import com.tinnomore.viewmodel.PatientWithSymptoms
import com.tinnomore.viewmodel.SpecialistViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    data: PatientWithSymptoms,
    onBack: () -> Unit,
    vm: SpecialistViewModel
) {
    var showDateRangePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val fmtDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (showDateRangePicker) {
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            // end + 86400000 para incluir el día final completo
                            vm.filterByDateRange(start, end + 86_400_000L)
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Selecciona el rango de fechas",
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = false,
                modifier = Modifier.fillMaxWidth().height(500.dp).padding(16.dp)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data.patient.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    // HU-05-3: abrir filtro de fechas
                    IconButton(onClick = { showDateRangePicker = true }) {
                        Icon(Icons.Default.DateRange, "Filtrar fechas", tint = Color.White)
                    }
                    IconButton(onClick = { vm.clearFilter() }) {
                        Icon(Icons.Default.FilterListOff, "Quitar filtro", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Datos del paciente ───────────────────────────────────────
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Información del paciente", fontWeight = FontWeight.Bold)
                    Text("RUT: ${data.patient.rut}")
                    Text("Email: ${data.patient.email}")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Indicador de filtro activo
            if (data.symptoms.isNotEmpty()) {
                val startStr = fmtDate.format(Date(data.symptoms.minOf { it.timestamp }))
                val endStr = fmtDate.format(Date(data.symptoms.maxOf { it.timestamp }))
                
                AssistChip(
                    onClick = { vm.clearFilter() },
                    label = { Text("Rango: $startStr - $endStr") },
                    leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Default.Clear, "Quitar filtro", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── HU-05-2: Gráficos de evolución ───────────────────────────
            Text("Evolución de síntomas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            if (data.symptoms.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin registros de síntomas en este período", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // Gráfico de línea de intensidad
                SymptomLineChart(symptoms = data.symptoms)

                Spacer(Modifier.height(14.dp))

                // Estadísticas
                val avg = data.symptoms.map { it.intensity }.average()
                val max = data.symptoms.maxOf { it.intensity }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge(Modifier.weight(1f), "Registros", "${data.symptoms.size}", MaterialTheme.colorScheme.primary)
                    StatBadge(Modifier.weight(1f), "Promedio",  String.format(Locale.getDefault(), "%.1f", avg), Color(0xFFE65100))
                    StatBadge(Modifier.weight(1f), "Máximo",    "$max / 10", Color(0xFFD32F2F))
                }

                Spacer(Modifier.height(16.dp))

                // Tabla de registros recientes
                Text(
                    "Registros recientes (${data.symptoms.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    modifier   = Modifier.padding(bottom = 6.dp)
                )

                val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                data.symptoms.take(15).forEach { s ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(fmt.format(Date(s.timestamp)), fontSize = 13.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Intensidad: ", fontSize = 13.sp)
                                val col = when {
                                    s.intensity <= 3 -> Color(0xFF2E7D32)
                                    s.intensity <= 6 -> Color(0xFFE65100)
                                    else             -> Color(0xFFD32F2F)
                                }
                                Surface(color = col, shape = MaterialTheme.shapes.extraSmall) {
                                    Text(
                                        "${s.intensity}/10",
                                        color    = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        // HU-05-2: impacto en sueño y concentración
                        if (s.sleepImpact != null || s.concentrationImpact != null) {
                            Row(modifier = Modifier.padding(top = 3.dp)) {
                                s.sleepImpact?.let {
                                    Text("Sueño: $it/10", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(Modifier.width(12.dp))
                                }
                                s.concentrationImpact?.let {
                                    Text("Concentración: $it/10", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Exposición a ruido (a partir de registros de crisis) ─────
            Text("Exposición a ruido", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            if (data.crisisRecords.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin registros de crisis/exposición en este período", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                val maxDb = data.crisisRecords.maxOf { it.maxDecibels }
                val avgDb = data.crisisRecords.map { it.maxDecibels }.average()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBadge(Modifier.weight(1f), "Episodios", "${data.crisisRecords.size}", MaterialTheme.colorScheme.primary)
                    StatBadge(Modifier.weight(1f), "dB promedio", String.format(Locale.getDefault(), "%.0f", avgDb), Color(0xFFE65100))
                    StatBadge(Modifier.weight(1f), "dB máximo", String.format(Locale.getDefault(), "%.0f", maxDb), Color(0xFFD32F2F))
                }

                Spacer(Modifier.height(12.dp))

                val fmtCrisis = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                data.crisisRecords.take(15).forEach { c ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(fmtCrisis.format(Date(c.timestamp)), fontSize = 13.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (c.therapyModified) {
                                Surface(color = Color(0xFFE65100), shape = MaterialTheme.shapes.extraSmall) {
                                    Text(
                                        "Terapia ajustada",
                                        color    = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            val col = when {
                                c.maxDecibels < 70f -> Color(0xFF2E7D32)
                                c.maxDecibels < 85f -> Color(0xFFE65100)
                                else                -> Color(0xFFD32F2F)
                            }
                            Surface(color = col, shape = MaterialTheme.shapes.extraSmall) {
                                Text(
                                    "${c.maxDecibels.toInt()} dB",
                                    color    = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Gráfico de línea ────────────────────────────────────────────────────────

@Composable
private fun SymptomLineChart(symptoms: List<SymptomEntry>) {
    val points = symptoms.reversed().takeLast(20)   // últimas 20 entradas en orden cronológico

    Card(
        modifier  = Modifier.fillMaxWidth().height(200.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        if (points.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Se necesitan al menos 2 registros para el gráfico", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            return@Card
        }
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val w     = size.width
            val h     = size.height
            val xStep = w / (points.size - 1)
            val lineColor = Color(0xFF1565C0)

            fun yOf(i: Int) = h - (h * points[i].intensity / 10f)

            // Grid horizontal (intensidades 2, 4, 6, 8, 10)
            for (v in 2..10 step 2) {
                val y = h - h * v / 10f
                drawLine(Color.LightGray, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Área bajo la curva
            val area = Path().also { p ->
                p.moveTo(0f, h)
                points.indices.forEach { i -> p.lineTo(i * xStep, yOf(i)) }
                p.lineTo((points.size - 1) * xStep, h)
                p.close()
            }
            drawPath(area, Color(0x331565C0))

            // Línea
            val line = Path().also { p ->
                points.indices.forEach { i ->
                    if (i == 0) p.moveTo(0f, yOf(0)) else p.lineTo(i * xStep, yOf(i))
                }
            }
            drawPath(line, lineColor, style = Stroke(width = 3f))

            // Puntos
            points.indices.forEach { i ->
                drawCircle(lineColor, 6f, Offset(i * xStep, yOf(i)))
                drawCircle(Color.White, 3f, Offset(i * xStep, yOf(i)))
            }
        }
    }
}

// ─── Badge de estadística ────────────────────────────────────────────────────

@Composable
private fun StatBadge(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
        Column(
            modifier            = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}
