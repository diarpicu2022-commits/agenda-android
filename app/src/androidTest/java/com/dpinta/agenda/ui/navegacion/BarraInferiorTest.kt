package com.dpinta.agenda.ui.navegacion

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpinta.agenda.ui.theme.AgendaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Enmienda 2026-09-24: con fuente 2,0 a 360 dp, «Actividades» cabe en una línea sin partirse. */
@RunWith(AndroidJUnit4::class)
class BarraInferiorTest {

    @get:Rule val regla = createComposeRule()

    @Test fun actividadesNoSeParteConFuenteDoble() {
        regla.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 2f)) {
                AgendaTheme(oscuro = false) {
                    BarraInferior(actual = Pestana.Hoy, onPestana = {}, onCrear = {}, modifier = Modifier.width(360.dp))
                }
            }
        }
        Pestana.entries.forEach { p ->
            val resultados = mutableListOf<TextLayoutResult>()
            regla.onNodeWithText(p.etiqueta, useUnmergedTree = true)
                .fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(resultados)
            val texto = resultados.single()
            assertEquals("${p.etiqueta} en una línea", 1, texto.lineCount)
            assertTrue("${p.etiqueta} sin recorte", !texto.hasVisualOverflow)
            // Nunca por debajo de 14 sp efectivos.
            assertTrue(texto.layoutInput.style.fontSize.value * 2f >= 14f)
        }
    }
}
