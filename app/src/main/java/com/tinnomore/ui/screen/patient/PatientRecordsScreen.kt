package com.tinnomore.ui.screen.patient

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SymptomPrimaryTab    = Color(0xFF1565C0)   // TinBlue
private val MedicationPrimaryTab = Color(0xFF00695C)   // TinTeal

private enum class RecordsTab { SYMPTOMS, MEDICATIONS }

/**
 * Contenedor de la pestaña "Síntomas" de la barra inferior.
 *
 * Mantiene intacta la barra de navegación del paciente (2 pestañas · Crisis · 2 pestañas)
 * y agrupa aquí los dos registros diarios que lleva el paciente:
 * síntomas y medicamentos.
 */
@Composable
fun PatientRecordsScreen(patientId: Long) {
    var selected by remember { mutableStateOf(RecordsTab.SYMPTOMS) }

    val tabs: @Composable () -> Unit = {
        TabRow(
            selectedTabIndex = selected.ordinal,
            containerColor   = if (selected == RecordsTab.SYMPTOMS) SymptomPrimaryTab else MedicationPrimaryTab,
            contentColor     = Color.White,
            // Indicador explícito: el TabRow se recrea al alternar de pantalla, así que
            // se posiciona a partir de la pestaña activa y se pinta en blanco para que
            // combine con la cabecera (el color primario por defecto desentona en teal).
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selected.ordinal]),
                    color    = Color.White
                )
            }
        ) {
            Tab(
                selected = selected == RecordsTab.SYMPTOMS,
                onClick  = { selected = RecordsTab.SYMPTOMS },
                text     = {
                    Text(
                        "Síntomas",
                        fontSize   = 13.sp,
                        fontWeight = if (selected == RecordsTab.SYMPTOMS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.padding(bottom = 0.dp))
                }
            )
            Tab(
                selected = selected == RecordsTab.MEDICATIONS,
                onClick  = { selected = RecordsTab.MEDICATIONS },
                text     = {
                    Text(
                        "Medicamentos",
                        fontSize   = 13.sp,
                        fontWeight = if (selected == RecordsTab.MEDICATIONS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(Icons.Default.Medication, contentDescription = null)
                }
            )
        }
    }

    when (selected) {
        RecordsTab.SYMPTOMS    -> SymptomScreen(patientId = patientId, onBack = {}, topTabs = tabs)
        RecordsTab.MEDICATIONS -> MedicationScreen(patientId = patientId, topTabs = tabs)
    }
}
