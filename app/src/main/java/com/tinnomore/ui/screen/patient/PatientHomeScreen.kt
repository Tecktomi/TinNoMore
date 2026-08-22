package com.tinnomore.ui.screen.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.data.db.entity.SymptomEntry
import com.tinnomore.data.db.entity.User
import com.tinnomore.viewmodel.SymptomUiState
import com.tinnomore.viewmodel.SymptomViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Colores ──────────────────────────────────────────────────────────────────
private val HomePrimary = Color(0xFF1565C0)
private val HomeTeal    = Color(0xFF00695C)

// ─── Pantalla principal del paciente ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    user: User?,
    onCrisisClick: () -> Unit,
    onSymptomsClick: () -> Unit,
    onAudiometryClick: () -> Unit,
    onLogout: () -> Unit,
    vm: SymptomViewModel = viewModel()
) {
    val patientId = user?.id ?: 0L
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(patientId) { vm.loadSymptoms(patientId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TinNoMore") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = HomePrimary,
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
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {

            // ── Saludo ─────────────────────────────────────────────────────
            val firstName = user?.name?.split(" ")?.firstOrNull() ?: "Paciente"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "¡Buenos días"
                hour < 20 -> "¡Buenas tardes"
                else      -> "¡Buenas noches"
            }
            Text(
                text       = "$greeting, $firstName!",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text     = "¿Cómo te encuentras hoy?",
                fontSize = 15.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ── Resumen últimos 7 días ────────────────────────────────────
            SevenDaySummaryCard(uiState = uiState)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Accesos rápidos ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.EditNote,
                    title     = "Síntomas",
                    subtitle  = "Registra tu estado diario",
                    color     = HomePrimary,
                    onClick   = onSymptomsClick
                )
                FeatureCard(
                    modifier  = Modifier.weight(1f),
                    icon      = Icons.Default.HearingDisabled,
                    title     = "Audiometría",
                    subtitle  = "Configura tu terapia",
                    color     = HomeTeal,
                    onClick   = onAudiometryClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Tarjeta resumen 7 días ───────────────────────────────────────────────────

@Composable
private fun SevenDaySummaryCard(uiState: SymptomUiState) {
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 3_600_000L

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint     = HomePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Últimos 7 días",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            when (uiState) {
                is SymptomUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                is SymptomUiState.Success -> {
                    val recent = uiState.symptoms.filter { it.timestamp >= sevenDaysAgo }

                    if (recent.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sin registros en los últimos 7 días",
                                color    = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        val avgIntensity = recent.map { it.intensity }.average()
                        val maxIntensity = recent.maxOf { it.intensity }
                        val avgSleep     = recent.mapNotNull { it.sleepImpact }.let {
                            if (it.isEmpty()) null else it.average()
                        }
                        val avgConc      = recent.mapNotNull { it.concentrationImpact }.let {
                            if (it.isEmpty()) null else it.average()
                        }

                        // Stats row
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatPill(
                                label = "Registros",
                                value = "${recent.size}",
                                icon  = Icons.Default.EditNote,
                                color = HomePrimary
                            )
                            StatPill(
                                label = "Intensidad media",
                                value = "%.1f".format(avgIntensity),
                                icon  = Icons.Default.ShowChart,
                                color = intensityColor(avgIntensity.toFloat())
                            )
                            StatPill(
                                label = "Pico",
                                value = "$maxIntensity",
                                icon  = Icons.Default.Warning,
                                color = intensityColor(maxIntensity.toFloat())
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Mini bar chart (last 7 calendar days)
                        WeekBarChart(symptoms = recent)

                        // Optional sleep / concentration
                        if (avgSleep != null || avgConc != null) {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                avgSleep?.let {
                                    ImpactChip(
                                        modifier = Modifier.weight(1f),
                                        label    = "Sueño",
                                        value    = "%.1f".format(it),
                                        icon     = Icons.Default.Bedtime
                                    )
                                }
                                avgConc?.let {
                                    ImpactChip(
                                        modifier = Modifier.weight(1f),
                                        label    = "Concentración",
                                        value    = "%.1f".format(it),
                                        icon     = Icons.Default.Psychology
                                    )
                                }
                            }
                        }
                    }
                }

                is SymptomUiState.Error -> {
                    Text(
                        "No se pudieron cargar los síntomas",
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─── Mini gráfico de barras por día ──────────────────────────────────────────

@Composable
private fun WeekBarChart(symptoms: List<SymptomEntry>) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }

    // Build last 7 days: (dayLabel, avgIntensity or null)
    val days = (6 downTo 0).map { daysBack ->
        val cal = today.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        val start = cal.timeInMillis
        val end   = start + 24 * 3_600_000L - 1

        val dayFmt = SimpleDateFormat("EE", Locale("es")).format(cal.time)
            .replaceFirstChar { it.uppercase() }.take(2)

        val entries = symptoms.filter { it.timestamp in start..end }
        val avg     = if (entries.isEmpty()) null else entries.map { it.intensity }.average().toFloat()
        Pair(dayFmt, avg)
    }

    val maxBar = 10f
    val barMaxHeight = 52.dp

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom
    ) {
        days.forEach { (label, avg) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (avg != null) {
                    val fraction = (avg / maxBar).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(barMaxHeight * fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(intensityColor(avg))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.LightGray)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

// ─── Componentes pequeños ─────────────────────────────────────────────────────

@Composable
private fun StatPill(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun ImpactChip(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        color    = MaterialTheme.colorScheme.secondaryContainer,
        shape    = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("$label: $value/10", fontSize = 11.sp)
        }
    }
}

private fun intensityColor(intensity: Float): Color = when {
    intensity <= 3f -> Color(0xFF2E7D32)
    intensity <= 6f -> Color(0xFFE65100)
    else            -> Color(0xFFD32F2F)
}

// ─── FeatureCard ─────────────────────────────────────────────────────────────

@Composable
fun FeatureCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(120.dp),
        colors    = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title,    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.8f),            fontSize = 11.sp)
        }
    }
}
