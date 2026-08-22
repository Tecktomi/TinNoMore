package com.tinnomore.ui.screen.patient

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinnomore.util.FrequencyPredictor
import com.tinnomore.util.NotchProcessor.NoiseType
import com.tinnomore.viewmodel.AudiometryViewModel
import com.tinnomore.viewmodel.GlobalNotchViewModel
import com.tinnomore.viewmodel.NotchGenState
import com.tinnomore.viewmodel.NotchViewModel
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ── Paleta ────────────────────────────────────────────────────────────────────
private val Teal700  = Color(0xFF00695C)
private val Teal50   = Color(0xFFE0F2F1)
private val Teal100  = Color(0xFFB2DFDB)
private val Amber500 = Color(0xFFFFC107)
private val Amber600 = Color(0xFFFFB300)
private val AmberBg  = Color(0xFFFFF8E1)
private val Red50    = Color(0xFFFFEBEE)
private val Red400   = Color(0xFFEF5350)

private val noiseAccent = mapOf(
    NoiseType.PINK  to Color(0xFFAD1457),
    NoiseType.WHITE to Color(0xFF37474F),
    NoiseType.BROWN to Color(0xFF4E342E)
)
private val noiseBg = mapOf(
    NoiseType.PINK  to Color(0xFFFCE4EC),
    NoiseType.WHITE to Color(0xFFECEFF1),
    NoiseType.BROWN to Color(0xFFEFEBE9)
)

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotchTherapyScreen(
    patientId: Long,
    onBack: () -> Unit = {},
    showBackButton: Boolean = false,
    initialNoiseType: NoiseType? = null,
    audiometryVm: AudiometryViewModel = viewModel(),
    notchVm: NotchViewModel = viewModel(),
    globalNotchVm: GlobalNotchViewModel = viewModel()
) {
    val predictedFc  by audiometryVm.predictedFc.collectAsState()
    val selectedFreq by notchVm.selectedFreq.collectAsState()
    val noiseType    by notchVm.noiseType.collectAsState()
    val volumeDb     by notchVm.volumeDb.collectAsState()
    val isPlaying    by notchVm.isPlaying.collectAsState()
    val genState     by notchVm.genState.collectAsState()

    val globalEnabled by globalNotchVm.enabled.collectAsState()

    LaunchedEffect(patientId) { audiometryVm.loadLatestProfile(patientId) }

    // Si el notch global está activo, se mantiene sincronizado con la
    // frecuencia elegida acá (misma fc para el ruido local y el filtro global).
    LaunchedEffect(selectedFreq, globalEnabled) {
        if (globalEnabled) globalNotchVm.setFrequency(selectedFreq)
    }

    LaunchedEffect(predictedFc) {
        predictedFc?.let { fc ->
            if (notchVm.availableFrequencies.contains(fc)) notchVm.setFrequency(fc)
        }
    }

    LaunchedEffect(initialNoiseType) {
        initialNoiseType?.let {
            notchVm.setNoiseType(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notch Therapy") },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Teal700,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. Tipo de ruido + acordeón ───────────────────────────────────────
            NoiseSelector(
                selected  = noiseType,
                isPlaying = isPlaying,
                onSelect  = { notchVm.setNoiseType(it) }
            )

            Spacer(Modifier.height(14.dp))

            // ── 2. Selector de frecuencia ─────────────────────────────────────────
            FrequencySelector(
                frequencies = notchVm.availableFrequencies,
                selected    = selectedFreq,
                predictedFc = predictedFc,
                onSelect    = { notchVm.setFrequency(it) }
            )

            Spacer(Modifier.height(14.dp))

            // ── 3. Rango del notch ────────────────────────────────────────────────
            NotchRangeCard(fcHz = selectedFreq)

            Spacer(Modifier.height(14.dp))

            // ── 3b. Notch global (todo el audio del dispositivo) ───────────────────
            GlobalNotchCard(
                enabled       = globalEnabled,
                fcHz          = selectedFreq,
                isSupported   = globalNotchVm.isSupported,
                unsupportedMsg = globalNotchVm.minSdkMessage,
                onToggle      = { globalNotchVm.setEnabled(it) }
            )

            Spacer(Modifier.height(14.dp))

            // ── 4. Volumen ────────────────────────────────────────────────────────
            VolumeCard(volumeDb = volumeDb, onVolumeChange = { notchVm.setVolume(it) })

            Spacer(Modifier.height(20.dp))

            // ── Estado generación ─────────────────────────────────────────────────
            AnimatedVisibility(genState is NotchGenState.Generating) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Teal50)
                            .padding(14.dp)
                    ) {
                        CircularProgressIndicator(color = Teal700, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Procesando audio con notch…", fontSize = 13.sp, color = Teal700)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            AnimatedVisibility(genState is NotchGenState.Error) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Red50)
                            .padding(14.dp)
                    ) {
                        Text("⚠ ${(genState as? NotchGenState.Error)?.msg}", fontSize = 13.sp, color = Red400)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── 5. Play / Pause ───────────────────────────────────────────────────
            PlayButton(
                isPlaying = isPlaying,
                isLoading = genState is NotchGenState.Generating,
                noiseType = noiseType,
                onToggle  = { if (isPlaying) notchVm.stop() else notchVm.play() }
            )

            AnimatedVisibility(
                visible = isPlaying,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(14.dp))
                    PlayingBadge(fcHz = selectedFreq, noiseType = noiseType, volumeDb = volumeDb)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NoiseSelector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoiseSelector(
    selected: NoiseType,
    isPlaying: Boolean,
    onSelect: (NoiseType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tipo de ruido base", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Teal700)
                if (isPlaying) {
                    Surface(color = Teal100, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "Detener para cambiar",
                            fontSize = 10.sp, color = Teal700,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NoiseType.entries.forEach { type ->
                    NoiseChip(
                        type       = type,
                        isSelected = type == selected,
                        enabled    = !isPlaying,
                        modifier   = Modifier.weight(1f),
                        onSelect   = { onSelect(type) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "¿En qué se diferencian?",
                    fontSize = 13.sp,
                    color    = Teal700,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Teal700,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    NoiseInfoRow("","Blanco", "Ruido generado por un rango continuo de frequencias distribuidas de manera uniforme a lo largo del espectro auditivo. Es el más agudo a pesar de que sus frecuencias sean uniformes, esto se debe a como nuestro oído percibe los sonidos.")
                    Spacer(Modifier.height(6.dp))
                    NoiseInfoRow("", "Rosa", "Variación del ruido blanco donde las frecuencias altas se atenuan y las bajas se amplifican. Es mas suave que el ruido blanco, el balance entre agudo y grave.")
                    Spacer(Modifier.height(6.dp))
                    NoiseInfoRow("", "Marrón", "Variación del ruido blanco con un enfasis aun mayor en las frecuencias graves, produciendo un sonido profundo y suave.")
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    NoiseInfoRow("", "¿Qué es la Notch Therapy?", "La Notch Therapy, o Terapia de muesca, es un tipo de terapia para tratar los acufenos. Conociendo la frecuencia central de la tinnitus, la terapia notch suprime esa frecuencia de tinnitus, haciendo que el cerebro lentamente interprete el ruido de tinnitus como ruido de fondo y su percepción disminuya.")
                    Spacer(Modifier.height(6.dp))
                    NoiseInfoRow("", "Recomendaciones", "Te aconsejamos realizar tu terapia en un ambiente silencioso y tranquilo, donde puedas descansar sin interrupciones por la duración de esta. Recomendamos sesiones de 30 minutos todos los días.")
                }
            }
        }
    }
}

@Composable
private fun NoiseChip(
    type: NoiseType,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit
) {
    val accent = noiseAccent[type] ?: Teal700
    val bg     = if (isSelected) (noiseBg[type] ?: Teal50) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (isSelected) accent else Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onSelect() },
        color = bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                type.label,
                fontSize   = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (isSelected) accent else Color.Gray
            )
        }
    }
}

@Composable
private fun NoiseInfoRow(emoji: String, bold: String?, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 15.sp, modifier = Modifier.width(26.dp))
        if (bold != null) {
            Text(bold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Text(" — $text", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        } else {
            Text(text, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FrequencySelector
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FrequencySelector(
    frequencies: List<Int>,
    selected: Int,
    predictedFc: Int?,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Frecuencia a filtrar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Teal700)
                if (predictedFc != null) {
                    Surface(color = AmberBg, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "ML: ${FrequencyPredictor.freqLabel(predictedFc)}",
                            fontSize = 11.sp, color = Amber600, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            val isMLSelected = selected == predictedFc
            Text(
                FrequencyPredictor.freqLabel(selected),
                fontSize   = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (isMLSelected) Amber600 else Teal700,
                modifier   = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )
            if (isMLSelected && predictedFc != null) {
                Text(
                    "Frecuencia recomendada por tu audiometría",
                    fontSize   = 11.sp,
                    color      = Amber500,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                frequencies.forEach { freq ->
                    val isSelected  = freq == selected
                    val isPredicted = freq == predictedFc

                    val chipBg = when {
                        isSelected && isPredicted -> Amber600
                        isSelected               -> Teal700
                        isPredicted              -> AmberBg
                        else                     -> Teal50
                    }
                    val chipText = when {
                        isSelected && isPredicted -> Color.White
                        isSelected               -> Color.White
                        isPredicted              -> Amber600
                        else                     -> Teal700
                    }
                    val chipBorder = if (isPredicted && !isSelected) Amber600 else Color.Transparent

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(if (isPredicted && !isSelected) 1.5.dp else 0.dp, chipBorder, RoundedCornerShape(20.dp))
                            .clickable { onSelect(freq) },
                        color = chipBg,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                FrequencyPredictor.freqLabel(freq),
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected || isPredicted) FontWeight.Bold else FontWeight.Normal,
                                color      = chipText
                            )
                            if (isPredicted) {
                                Text("Frecuencia predicha", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  NotchRangeCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotchRangeCard(fcHz: Int) {
    val lower = (fcHz / sqrt(2.0)).roundToInt()
    val upper = (fcHz * sqrt(2.0)).roundToInt()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Teal50),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Rango del filtro notch", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Teal700)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FreqPill("−1 octava", lower, "límite inferior")
                Text("↔", fontSize = 20.sp, color = Teal700)
                FreqPill("fc", fcHz, "centro", bold = true)
                Text("↔", fontSize = 20.sp, color = Teal700)
                FreqPill("+1 octava", upper, "límite superior")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Ancho de banda: 1 octava (fc/√2 … fc×√2)",
                fontSize = 11.sp, color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FreqPill(topLabel: String, hz: Int, bottomLabel: String, bold: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(topLabel, fontSize = 10.sp, color = Color.Gray)
        Text(
            FrequencyPredictor.freqLabel(hz),
            fontSize   = if (bold) 18.sp else 14.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color      = Teal700
        )
        Text(bottomLabel, fontSize = 10.sp, color = Color.Gray)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VolumeCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VolumeCard(volumeDb: Float, onVolumeChange: (Float) -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Intensidad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Teal700)
                Surface(color = Teal100, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "${volumeDb.roundToInt()} dB",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal700,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Slider(
                value         = volumeDb,
                onValueChange = onVolumeChange,
                valueRange    = -40f..0f,
                steps         = 39,
                colors        = SliderDefaults.colors(thumbColor = Teal700, activeTrackColor = Teal700),
                modifier      = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Silencio (−40 dB)", fontSize = 11.sp, color = Color.Gray)
                Text("Máximo (0 dB)", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PlayButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    noiseType: NoiseType,
    onToggle: () -> Unit
) {
    val containerColor = when {
        isLoading -> Color(0xFF607D8B)
        isPlaying -> Color(0xFF795548)
        else      -> Teal700
    }
    Button(
        onClick  = onToggle,
        enabled  = !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Procesando ruido ${noiseType.label.lowercase()}…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isPlaying) "Pausar" else "Reproducir en loop",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PlayingBadge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayingBadge(fcHz: Int, noiseType: NoiseType, volumeDb: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE8F5E9))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7D32).copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Ruido ${noiseType.label} • Notch en ${FrequencyPredictor.freqLabel(fcHz)} • ${volumeDb.roundToInt()} dB",
            fontSize   = 13.sp,
            color      = Color(0xFF2E7D32),
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Notch global (todo el audio del dispositivo) ──────────────────────────

@Composable
private fun GlobalNotchCard(
    enabled: Boolean,
    fcHz: Int,
    isSupported: Boolean,
    unsupportedMsg: String?,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFEDE7F6) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Notch en todo el audio del dispositivo",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isSupported)
                            "Aplica el filtro a cualquier app que reproduzca sonido (música, podcasts, videos), no solo a TinNoMore."
                        else
                            unsupportedMsg ?: "No disponible en este dispositivo.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = isSupported
                )
            }

            if (enabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Filtrando ${FrequencyPredictor.freqLabel(fcHz)} · Se mantiene activo mientras la app corra en segundo plano (verás una notificación).",
                    fontSize = 11.sp,
                    color = Color(0xFF4A148C)
                )
            }
        }
    }
}
