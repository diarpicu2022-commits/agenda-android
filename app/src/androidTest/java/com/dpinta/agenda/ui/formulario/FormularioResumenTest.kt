package com.dpinta.agenda.ui.formulario

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpinta.agenda.ui.theme.AgendaTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pendiente 1 de CONTINUAR §3: a 360 dp con fuente 2,0 la frase-resumen se puede llevar a la
 * vista entera (no se sale ni se corta). El fallo era del recorrido del script (tope de 5 fotogramas).
 */
@RunWith(AndroidJUnit4::class)
class FormularioResumenTest {

    @get:Rule val regla = createComposeRule()

    @Test fun resumenVisibleA360dpConFuenteDoble() {
        val hoy = LocalDate.of(2026, 9, 24)
        val f = Formulario.nuevo(hoy).copy(
            titulo = "Álgebra lineal",
            dias = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
            inicio = "14:00",
            fin = "16:00",
            lugar = "Campus",
            salon = "C-101",
        )
        val estado = FormularioUiState(false, true, false, f, emptyMap(), Validador.validar(f, hoy).aGuardar, emptyList(), FaseGuardado.Editando)
        regla.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                AgendaTheme(oscuro = false) {
                    FormularioPantalla(estado, AccionesFormulario({}, {}, {}, {}, {}, {}, {}, {}), Modifier.size(360.dp, 800.dp))
                }
            }
        }
        val resumen = regla.onNodeWithTag(EtiquetasFormulario.RESUMEN).performScrollTo().assertIsDisplayed()
        val capas = mutableListOf<TextLayoutResult>()
        resumen.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(capas)
        val texto = capas.single()
        assertTrue(texto.layoutInput.text.text.startsWith("Álgebra lineal: "))
        assertFalse("la frase no se recorta", texto.hasVisualOverflow)
    }
}
