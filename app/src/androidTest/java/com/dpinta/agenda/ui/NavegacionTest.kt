package com.dpinta.agenda.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpinta.agenda.MainActivity
import com.dpinta.agenda.ui.navegacion.EtiquetasBarra
import com.dpinta.agenda.ui.navegacion.Pestana
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Navegación real (Hilt + NavHost) sobre MainActivity. */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule val regla = createAndroidComposeRule<MainActivity>()

    private fun pestana(p: Pestana) = regla.onNodeWithTag(EtiquetasBarra.pestana(p))

    @Test fun lasPestanasCambianDeDestino() {
        pestana(Pestana.Hoy).assertIsSelected()
        pestana(Pestana.Semana).performClick()
        pestana(Pestana.Semana).assertIsSelected()
        regla.onNodeWithText("Empieza por tu horario de clases").assertExists()
        pestana(Pestana.Actividades).performClick()
        pestana(Pestana.Actividades).assertIsSelected()
        regla.onNodeWithText("Actividades", useUnmergedTree = true).assertExists()
        pestana(Pestana.Hoy).performClick()
        pestana(Pestana.Hoy).assertIsSelected()
        regla.onNodeWithTag(EtiquetasBarra.CREAR).performClick()
        regla.onNodeWithText("Nueva actividad").assertExists()
    }

    /** C8.3: el cambio de pestaña es un corte; el destino nuevo está en el primer fotograma. */
    @Test fun cambiarDePestanaNoSeAnima() {
        regla.waitForIdle()
        regla.mainClock.autoAdvance = false
        pestana(Pestana.Semana).performClick()
        regla.mainClock.advanceTimeByFrame()
        regla.mainClock.advanceTimeByFrame()
        regla.onNodeWithText("Empieza por tu horario de clases").assertExists()
        regla.mainClock.autoAdvance = true
    }

    /** C9.2: toques ≥ 48 dp en toda la barra. */
    @Test fun tocablesDeLaBarraMidenAlMenos48dp() {
        Pestana.entries.forEach { pestana(it).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp) }
        regla.onNodeWithTag(EtiquetasBarra.CREAR).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    /** C9.4: con teclado, la barra se recorre Hoy → Semana → Actividades → Crear. */
    @OptIn(ExperimentalTestApi::class)
    @Test fun elFocoRecorreLaBarra() {
        fun SemanticsNodeInteraction.enfocado() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
        val orden = listOf(
            pestana(Pestana.Hoy),
            pestana(Pestana.Semana),
            pestana(Pestana.Actividades),
            regla.onNodeWithTag(EtiquetasBarra.CREAR),
        )
        orden.first().requestFocus()
        regla.waitForIdle()
        val recorrido = mutableListOf(orden.indexOfFirst { it.enfocado() })
        repeat(3) {
            regla.onRoot().performKeyInput { pressKey(Key.Tab) }
            regla.waitForIdle()
            recorrido += orden.indexOfFirst { it.enfocado() }
        }
        assertEquals(listOf(0, 1, 2, 3), recorrido)
    }
}
