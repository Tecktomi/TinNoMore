package com.tinnomore.ui.screen.patient

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

// ── Paleta ────────────────────────────────────────────────────────────────────
private val Teal700  = Color(0xFF00695C)
private val Teal50   = Color(0xFFE0F2F1)
private val Teal100  = Color(0xFFB2DFDB)
private val Amber500 = Color(0xFFFFC107)
private val Amber600 = Color(0xFFFFB300)
private val NotchGreen      = Color(0xFF2E9E5B)
private val NotchGreenDark  = Color(0xFF1B6B3C)
private val NotchGreenSoft  = Color(0xFFCFEFDA)
private val GraphInk        = Color(0xFF163A2E)
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
    val widthOctaves by notchVm.widthOctaves.collectAsState()
    val isPlaying    by notchVm.isPlaying.collectAsState()
    val genState     by notchVm.genState.collectAsState()

    val globalEnabled by globalNotchVm.enabled.collectAsState()

    // ── Toggles de edición manual ────────────────────────────────────────────
    // Por defecto la pantalla es solo informativa: se muestran los valores
    // configurados (frecuencia central y ancho de banda) sin poder tocarlos.
    // Estos toggles habilitan sus respectivos controles cuando el usuario
    // realmente necesita ajustarlos a mano.
    var manualFreqEnabled  by remember { mutableStateOf(false) }
    var manualWidthEnabled by remember { mutableStateOf(false) }

    // El ancho de banda es uno solo: se define arriba (ruido local) y se
    // replica automáticamente al notch en segundo plano para que ambos
    // filtren siempre exactamente el mismo rango.
    LaunchedEffect(widthOctaves) { globalNotchVm.setWidthOctaves(widthOctaves) }

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

            // ── 2. Toggles de ajuste manual ───────────────────────────────────────
            ManualAdjustmentTogglesCard(
                manualFreqEnabled  = manualFreqEnabled,
                onManualFreqChange = { manualFreqEnabled = it },
                manualWidthEnabled = manualWidthEnabled,
                onManualWidthChange = { manualWidthEnabled = it }
            )

            Spacer(Modifier.height(14.dp))

            // ── 3. Frecuencia central + ancho de banda (un solo contenedor) ────────
            // Sin los toggles activados, solo se informan los valores configurados
            // (ML o predeterminados), sin posibilidad de modificarlos.
            FrequencyAndWidthCard(
                frequencies     = notchVm.availableFrequencies,
                selectedFreq    = selectedFreq,
                predictedFc     = predictedFc,
                freqEditable    = manualFreqEnabled,
                onSelectFreq    = { notchVm.setFrequency(it) },
                widthOctaves    = widthOctaves,
                widthEditable   = manualWidthEnabled,
                onWidthChange   = { notchVm.setWidthOctaves(it) },
                volumeDb        = volumeDb,
                onVolumeChange  = { notchVm.setVolume(it) }
            )

            Spacer(Modifier.height(14.dp))

            // ── 5. Notch global (todo el audio del dispositivo) ─────────────────────
            GlobalNotchCard(
                enabled        = globalEnabled,
                isSupported    = globalNotchVm.isSupported,
                unsupportedMsg = globalNotchVm.minSdkMessage,
                onToggle       = { globalNotchVm.setEnabled(it) }
            )

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

            // ── 6. Play / Pause ───────────────────────────────────────────────────
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
//  ManualAdjustmentTogglesCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ManualAdjustmentTogglesCard(
    manualFreqEnabled: Boolean,
    onManualFreqChange: (Boolean) -> Unit,
    manualWidthEnabled: Boolean,
    onManualWidthChange: (Boolean) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Ajustes manuales", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Teal700)

            Spacer(Modifier.height(10.dp))

            ManualToggleRow(
                title    = "Cambiar frecuencia manualmente",
                checked  = manualFreqEnabled,
                onCheck  = onManualFreqChange
            )
            Spacer(Modifier.height(4.dp))
            ManualToggleRow(
                title    = "Cambiar ancho de banda manualmente",
                checked  = manualWidthEnabled,
                onCheck  = onManualWidthChange
            )
        }
    }
}

@Composable
private fun ManualToggleRow(title: String, checked: Boolean, onCheck: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 13.sp, color = GraphInk, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Teal700
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FrequencyAndWidthCard — frecuencia central, curva del filtro, ancho e
//  intensidad, todo en un solo contenedor cohesivo.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FrequencyAndWidthCard(
    frequencies: List<Int>,
    selectedFreq: Int,
    predictedFc: Int?,
    freqEditable: Boolean = true,
    onSelectFreq: (Int) -> Unit,
    widthOctaves: Float,
    widthEditable: Boolean = true,
    onWidthChange: (Float) -> Unit,
    volumeDb: Float,
    onVolumeChange: (Float) -> Unit
) {
    val lower = notchEdgeHz(selectedFreq, widthOctaves, lower = true)
    val upper = notchEdgeHz(selectedFreq, widthOctaves, lower = false)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // ── Frecuencia central ──────────────────────────────────────────
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

            val isMLSelected = selectedFreq == predictedFc
            Text(
                FrequencyPredictor.freqLabel(selectedFreq),
                fontSize   = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (isMLSelected) Amber600 else Teal700,
                modifier   = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )

            if (freqEditable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    frequencies.forEach { freq ->
                        val isSelected  = freq == selectedFreq
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
                                .clickable { onSelectFreq(freq) },
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

            // ── Curva verde del notch: solo visible con ajuste manual de ancho ──
            if (widthEditable) {
                Spacer(Modifier.height(14.dp))

                NotchCurveGraph(
                    fcHz = selectedFreq,
                    widthOctaves = widthOctaves,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(FrequencyPredictor.freqLabel(lower), fontSize = 11.sp, color = Color.Gray)
                    Text("centro ${FrequencyPredictor.freqLabel(selectedFreq)}", fontSize = 11.sp, color = NotchGreenDark, fontWeight = FontWeight.Medium)
                    Text(FrequencyPredictor.freqLabel(upper), fontSize = 11.sp, color = Color.Gray)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF0F0F0))

            // ── Ancho de banda ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Ancho de banda", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GraphInk)
                Surface(color = NotchGreenSoft, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "${String.format("%.2f", widthOctaves)} oct",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NotchGreenDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (widthEditable) {
                Slider(
                    value         = widthOctaves,
                    onValueChange = onWidthChange,
                    valueRange    = 0.1f..3f,
                    colors        = SliderDefaults.colors(thumbColor = NotchGreen, activeTrackColor = NotchGreen),
                    modifier      = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Intensidad ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Intensidad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GraphInk)
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

/** Límite inferior/superior del notch en Hz, dado el ancho total en octavas. */
private fun notchEdgeHz(fcHz: Int, widthOctaves: Float, lower: Boolean): Int {
    val half = widthOctaves / 2.0
    val factor = 2.0.pow(if (lower) -half else half)
    return (fcHz * factor).roundToInt()
}

/**
 * Curva del filtro notch en escala logarítmica de frecuencia (100 Hz–16 kHz),
 * calculada con la misma matemática que GlobalNotchEffect.gainForBand para
 * que lo que se ve acá sea exactamente lo que se está aplicando.
 */
@Composable
private fun NotchCurveGraph(fcHz: Int, widthOctaves: Float, modifier: Modifier = Modifier) {
    val fMin = 100f
    val fMax = 16000f
    val logMin = log2(fMin)
    val logMax = log2(fMax)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 8f
        val bottomPad = 8f
        val plotH = h - topPad - bottomPad

        fun xForFreq(f: Float): Float {
            val t = ((log2(f) - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
            return t * w
        }

        // gain 0f = sin atenuar (arriba), 1f = atenuación máxima del notch (abajo)
        fun attenuationAt(f: Float): Float {
            val halfWidth = (widthOctaves / 2f).coerceIn(0.05f, 1.5f)
            val octaves = abs(log2(f / fcHz))
            return when {
                octaves <= halfWidth * 0.6f -> 1f
                octaves >= halfWidth * 1.6f -> 0f
                else -> {
                    val t = (octaves - halfWidth * 0.6f) / (halfWidth * 1.6f - halfWidth * 0.6f)
                    1f - t.coerceIn(0f, 1f)
                }
            }
        }

        val steps = 96
        val points = (0..steps).map { i ->
            val f = 2f.pow(logMin + (logMax - logMin) * i / steps)
            val atten = attenuationAt(f)
            val y = topPad + atten * plotH
            Offset(xForFreq(f), y)
        }

        // Área rellena bajo la curva (gradiente verde)
        val fillPath = Path().apply {
            moveTo(points.first().x, topPad + plotH)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, topPad + plotH)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(NotchGreen.copy(alpha = 0.35f), NotchGreen.copy(alpha = 0.03f)),
                startY = topPad,
                endY = topPad + plotH
            )
        )

        // Línea de la curva
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            color = NotchGreenDark,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Línea vertical punteada marcando fc
        val fcX = xForFreq(fcHz.toFloat())
        drawLine(
            color = NotchGreenDark.copy(alpha = 0.5f),
            start = Offset(fcX, topPad),
            end   = Offset(fcX, topPad + plotH),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Punto central en el fondo de la curva (mínimo, i.e. máxima atenuación)
        drawCircle(
            color = NotchGreenDark,
            radius = 8f,
            center = Offset(fcX, topPad + plotH)
        )
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
    isSupported: Boolean,
    unsupportedMsg: String?,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) NotchGreenDark else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Notch global",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isSupported)
                            "Filtra todo sonido del celular, como música, podcasts, o videos."
                        else
                            unsupportedMsg ?: "No disponible en este dispositivo.",
                        fontSize = 12.sp,
                        color = if (enabled) Color.White.copy(alpha = 0.75f) else Color.Gray
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    enabled = isSupported,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NotchGreen
                    )
                )
            }

            if (enabled) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Se mantiene activo con la app en segundo plano.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
