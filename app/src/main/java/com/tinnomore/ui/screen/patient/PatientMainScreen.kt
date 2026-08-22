package com.tinnomore.ui.screen.patient

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinnomore.data.db.entity.User
import com.tinnomore.util.NotchProcessor

// ─── Pestañas del paciente ────────────────────────────────────────────────────

enum class PatientTab {
    HOME, AUDIOMETRY, NOTCH, SYMPTOMS
}

private data class TabInfo(
    val tab: PatientTab,
    val icon: ImageVector,
    val label: String
)

private val LEFT_TABS = listOf(
    TabInfo(PatientTab.HOME,      Icons.Default.Home,           "Inicio"),
    TabInfo(PatientTab.AUDIOMETRY, Icons.Default.HearingDisabled, "Audición")
)

private val RIGHT_TABS = listOf(
    TabInfo(PatientTab.NOTCH,    Icons.Default.MusicNote, "Notch"),
    TabInfo(PatientTab.SYMPTOMS, Icons.Default.EditNote,  "Síntomas")
)

// ─── Pantalla principal del paciente ─────────────────────────────────────────

/**
 * v0.2: reemplaza PatientHomeScreen.
 * Contiene una barra de navegación inferior con 5 elementos:
 *   Síntomas · Audición · (CRISIS) · Notch · Ajustes
 *
 * El botón de Crisis (centro) es circular, rojo y más grande.
 * Al pulsarlo navega hacia CrisisScreen vía (onCrisisClick).
 */
@Composable
fun PatientMainScreen(
    user: User?,
    onCrisisClick: () -> Unit,
    onLogout: () -> Unit,
    initialNoiseType: NotchProcessor.NoiseType? = null
) {
    val patientId = user?.id ?: 0L
    var selectedTab by remember { mutableStateOf(PatientTab.HOME) }

    // Si venimos de CrisisScreen con una recomendación, cambiar a Notch y pasar el tipo
    LaunchedEffect(initialNoiseType) {
        if (initialNoiseType != null) {
            selectedTab = PatientTab.NOTCH
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Reserva el espacio de la barra de navegación del sistema para
            // que nuestra barra inferior personalizada nunca quede tapada
            // por ella (el resto de la pantalla sigue en edge-to-edge, así
            // las topbars pueden extender su color bajo la barra de estado).
            .navigationBarsPadding()
    ) {

        // ── Contenido de la pestaña activa ────────────────────────────────
        // Padding inferior para que el contenido no quede tapado por la barra
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = BOTTOM_BAR_HEIGHT)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    PatientTab.HOME       -> PatientHomeScreen(
                        user             = user,
                        onCrisisClick    = onCrisisClick,
                        onSymptomsClick  = { selectedTab = PatientTab.SYMPTOMS },
                        onAudiometryClick = { selectedTab = PatientTab.AUDIOMETRY },
                        onLogout         = onLogout
                    )
                    PatientTab.SYMPTOMS   -> SymptomScreen(patientId = patientId, onBack = {})
                    PatientTab.AUDIOMETRY -> AudiometryScreen(patientId = patientId, onBack = {})
                    PatientTab.NOTCH      -> NotchTherapyScreen(
                        patientId = patientId,
                        initialNoiseType = initialNoiseType
                    )
                }
            }
        }

        // ── Barra de navegación inferior personalizada ────────────────────
        PatientBottomBar(
            modifier     = Modifier.align(Alignment.BottomCenter),
            selectedTab  = selectedTab,
            onTabSelect  = { selectedTab = it },
            onCrisisClick = onCrisisClick
        )
    }
}

// ─── Barra inferior personalizada ────────────────────────────────────────────

/** Altura total de la zona inferior (bar + FAB que sobresale) */
private val BOTTOM_BAR_HEIGHT = 72.dp

/** Tamaño del botón de crisis (mayor que los iconos normales) */
private val CRISIS_BUTTON_SIZE = 62.dp

/** Cuánto sobresale el botón de crisis por encima de la barra */
private val CRISIS_OFFSET_UP = 18.dp

@Composable
private fun PatientBottomBar(
    modifier: Modifier = Modifier,
    selectedTab: PatientTab,
    onTabSelect: (PatientTab) -> Unit,
    onCrisisClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BOTTOM_BAR_HEIGHT + CRISIS_OFFSET_UP)   // espacio para el botón elevado
    ) {
        // ── Fondo de la barra (Surface) ────────────────────────────────────
        Surface(
            modifier         = Modifier
                .fillMaxWidth()
                .height(BOTTOM_BAR_HEIGHT)
                .align(Alignment.BottomCenter)
                .shadow(elevation = 12.dp),
            color            = MaterialTheme.colorScheme.surface,
            tonalElevation   = 3.dp
        ) {
            Row(
                modifier              = Modifier.fillMaxSize(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // ── Pestañas izquierda ──────────────────────────────────
                LEFT_TABS.forEach { info ->
                    BottomNavItem(
                        modifier  = Modifier.weight(1f),
                        icon      = info.icon,
                        label     = info.label,
                        selected  = selectedTab == info.tab,
                        onClick   = { onTabSelect(info.tab) }
                    )
                }

                // ── Espacio central para el botón de crisis ─────────────
                Spacer(modifier = Modifier.weight(1.1f))

                // ── Pestañas derecha ────────────────────────────────────
                RIGHT_TABS.forEach { info ->
                    BottomNavItem(
                        modifier  = Modifier.weight(1f),
                        icon      = info.icon,
                        label     = info.label,
                        selected  = selectedTab == info.tab,
                        onClick   = { onTabSelect(info.tab) }
                    )
                }
            }
        }

        // ── Botón de crisis: circular, rojo, elevado sobre la barra ───────
        Column(
            modifier            = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(CRISIS_BUTTON_SIZE)
                    .shadow(
                        elevation = 10.dp,
                        shape     = CircleShape,
                        clip      = false
                    )
                    .background(color = Color(0xFFD32F2F), shape = CircleShape)
                    .clickable(onClick = onCrisisClick)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Crisis",
                    tint     = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Etiqueta debajo del botón (dentro de la barra)
            Text(
                "Crisis",
                color      = Color(0xFFD32F2F),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(top = 3.dp)
            )
        }
    }
}

// ─── Ítem individual de la barra ─────────────────────────────────────────────

@Composable
private fun BottomNavItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray

    Column(
        modifier            = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint     = color,
            modifier = Modifier.size(if (selected) 26.dp else 24.dp)
        )
        Text(
            label,
            fontSize   = 10.sp,
            color      = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier   = Modifier.padding(top = 2.dp)
        )
        // Indicador activo
        if (selected) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(width = 18.dp, height = 2.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
        }
    }
}
