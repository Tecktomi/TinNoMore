package com.tinnomore.ui.screen.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinnomore.data.db.entity.User

private val SettingsPurple     = Color(0xFF4A148C)  // TinPurple
private val SettingsPurpleLight = Color(0xFFEDE7F6)

/**
 * Pestaña de Configuración y Sobre nosotros.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    showBackButton: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = SettingsPurple,
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
                .padding(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Perfil ────────────────────────────────────────────────────────
            Text("Mi perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = SettingsPurpleLight)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color    = SettingsPurple,
                        shape    = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                user?.name?.first()?.uppercaseChar()?.toString() ?: "?",
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 22.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(user?.name ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(user?.email ?: "—", fontSize = 13.sp, color = Color.Gray)
                        Text("RUT: ${user?.rut ?: "—"}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Configuración ─────────────────────────────────────────────────
            Text("Configuración", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

            SettingsItem(icon = Icons.Default.Notifications,  label = "Recordatorios de terapia", value = "Activados") {}
            SettingsItem(icon = Icons.Default.DarkMode,       label = "Modo oscuro",              value = "Sistema") {}
            SettingsItem(icon = Icons.Default.Language,       label = "Idioma",                   value = "Español") {}

            Spacer(Modifier.height(24.dp))

            // ── Sobre la app ──────────────────────────────────────────────────
            Text("Sobre TinNoMore", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutRow("Versión", "0.2.0")
                    AboutRow("Institución", "UTFSM — Depto. de Informática")
                    AboutRow("Contacto clínico", "simonsnm@ug.uchile.cl")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "TinNoMore es un prototipo académico desarrollado como proyecto de curso. " +
                        "No reemplaza el diagnóstico ni tratamiento médico profesional.",
                        fontSize = 12.sp,
                        color    = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Qué es el tinnitus ────────────────────────────────────────────
            Text("Sobre el tinnitus", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("¿Qué es el tinnitus?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "El tinnitus o acúfeno es la percepción de un sonido (zumbido, pitido o silbido) " +
                        "sin que exista una fuente sonora externa.",
                        fontSize = 13.sp, color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Factores de riesgo:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    listOf(
                        "Exposición prolongada a ruidos fuertes",
                        "Pérdida auditiva relacionada con la edad",
                        "Estrés y ansiedad elevados",
                        "Otitis y otros problemas del oído"
                    ).forEach { item ->
                        Text("• $item", fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Cerrar sesión ─────────────────────────────────────────────────
            OutlinedButton(
                onClick  = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = SettingsPurple, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Text(value, fontSize = 13.sp, color = Color.Gray)
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
