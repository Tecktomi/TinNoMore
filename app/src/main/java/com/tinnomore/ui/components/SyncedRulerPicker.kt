package com.tinnomore.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nativeknights.rulerkit.RulerConfig
import com.nativeknights.rulerkit.RulerPicker
import com.nativeknights.rulerkit.rememberRulerPickerState

/**
 * Wraps RulerKit's [RulerPicker] so it behaves like a normal state-hoisted
 * Compose control: `value` in, `onValueChange` out — exactly like [androidx.compose.material3.Slider].
 *
 * Por qué hace falta este wrapper y no basta con pasar `value` directo:
 * [rememberRulerPickerState] crea un estado interno (RulerPickerState) que NO se
 * actualiza solo cuando `value` cambia desde fuera (por ejemplo, un StateFlow del
 * ViewModel que se resetea o se sincroniza con otra pantalla). Este wrapper:
 *
 *  1. Crea el estado del ruler una sola vez, con `value` como valor inicial.
 *  2. Escucha cambios externos de `value` y los aplica al ruler con `state.setValue(...)`.
 *  3. Evita el eco: si el cambio de `value` vino del propio arrastre del usuario
 *     (vía `onValueChanged` -> `onValueChange` -> ViewModel -> `value`), no lo
 *     vuelve a aplicar al ruler, así el drag queda perfectamente fluido.
 *
 * Nota sobre el punto 3: cuando `onValueChange` redondea el valor crudo (por
 * ejemplo, a `Int` para Hz o dB HL), el valor que vuelve por `value` casi nunca
 * coincide EXACTO con el crudo que emitió la regla en ese frame — así que
 * comparar con `!=` dispara `setValue` en cada frame y la regla se "traba"
 * peleando contra el propio dedo. Por eso la comparación usa una tolerancia de
 * medio paso (`step / 2`): cualquier diferencia menor a eso se considera "el
 * mismo valor, solo redondeado", y no se re-sincroniza.
 *
 * @param value          Valor actual (fuente de verdad, típicamente un StateFlow del ViewModel).
 * @param onValueChange  Se invoca en cada frame mientras el usuario arrastra (igual que Slider.onValueChange).
 * @param config         Configuración del ruler (rango, paso, unidad, colores, etc.).
 * @param onScrollEnd    Opcional: se invoca solo cuando el usuario suelta el dedo, con el valor ya encajado al step.
 */
@Composable
fun SyncedRulerPicker(
    value: Float,
    onValueChange: (Float) -> Unit,
    config: RulerConfig,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp),
    onScrollEnd: ((Float) -> Unit)? = null,
) {
    val state = rememberRulerPickerState(config.copy(initialValue = value))

    // Último valor que nosotros mismos emitimos hacia afuera. Sirve para
    // distinguir "el usuario arrastró" (no hay que re-sincronizar) de
    // "el valor cambió por otra razón externa" (sí hay que re-sincronizar).
    var lastEmitted by remember { mutableFloatStateOf(value) }

    // Medio paso de tolerancia: absorbe el redondeo que haga `onValueChange`
    // (p. ej. a Int) sin confundirlo con un cambio externo real.
    val step = config.inputType.unit.defaultStep.takeIf { it > 0f } ?: 1f
    val epsilon = step / 2f

    LaunchedEffect(value) {
        if (kotlin.math.abs(value - lastEmitted) > epsilon) {
            state.setValue(value, animate = false)
            lastEmitted = value
        }
    }

    RulerPicker(
        state = state,
        modifier = modifier,
        onValueChanged = { newValue, _ ->
            lastEmitted = newValue
            onValueChange(newValue)
        },
        onScrollEnd = { snapped, _ -> onScrollEnd?.invoke(snapped) }
    )
}
