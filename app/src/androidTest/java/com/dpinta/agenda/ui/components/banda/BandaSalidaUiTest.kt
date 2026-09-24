package com.dpinta.agenda.ui.components.banda

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpinta.agenda.ui.theme.AgendaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BandaSalidaUiTest {

    @get:Rule val regla = createComposeRule()

    /** C2.2 / C8.2: con cifras tabulares la hora no cambia de ancho de un minuto a otro. */
    @Test fun laHoraMantieneElAncho() {
        val cuatroCifras = listOf("10:48", "11:11", "12:58", "20:00", "19:37")
        val tresCifras = listOf("7:32", "1:11", "8:04")
        regla.setContent {
            AgendaTheme(oscuro = false) {
                Column {
                    (cuatroCifras + tresCifras).forEach { hora ->
                        CifraTablero(
                            texto = hora,
                            estilo = AgendaTheme.tipo.horaSalida,
                            color = AgendaTheme.colores.tinta,
                            reducir = true,
                            modifier = Modifier.testTag(hora),
                        )
                    }
                }
            }
        }
        fun ancho(tag: String) = regla.onNodeWithTag(tag).getBoundsInRoot().let { it.right - it.left }
        val anchos4 = cuatroCifras.map(::ancho).toSet()
        val anchos3 = tresCifras.map(::ancho).toSet()
        assertEquals("4 cifras: $anchos4", 1, anchos4.size)
        assertEquals("3 cifras: $anchos3", 1, anchos3.size)
    }

    /** C1.2: «Voy saliendo» a la derecha de «+5 min»; C9.2: toques ≥ 48 dp; C9.3: frase única. */
    @Test fun accionesDeManoDerechaYToques() {
        var accion: AccionBanda? = null
        var modoTocado = false
        regla.setContent {
            AgendaTheme(oscuro = false) {
                BandaSalida(
                    modelo = BandaSalidaMuestras.para(EstadoBanda.Espera),
                    onAccion = { accion = it },
                    onCambiarModo = { modoTocado = true },
                    es24h = true,
                    reducirMovimiento = true,
                    bajoBarraDeEstado = false,
                )
            }
        }
        val primaria = regla.onNodeWithText("Voy saliendo", useUnmergedTree = true)
        val secundaria = regla.onNodeWithText("+5 min", useUnmergedTree = true)
        assertTrue(primaria.getBoundsInRoot().left > secundaria.getBoundsInRoot().right)

        regla.onNodeWithText("Voy saliendo").assertHeightIsAtLeast(48.dp).performClick()
        regla.onNodeWithText("+5 min").assertHeightIsAtLeast(48.dp)
        regla.onNodeWithTag(EtiquetasBanda.MODO).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(AccionBanda.VoySaliendo, accion)
        assertTrue(modoTocado)

        regla.onNodeWithTag(EtiquetasBanda.INFO).assertContentDescriptionEquals(
            "Sal a las 7 y 32 para Cálculo diferencial, salón B-204, 23 minutos en bus más 7 de margen, " +
                "calculado hace 3 minutos",
        )
    }

    /** C9.4: con teclado, dentro de la banda el orden es primaria → secundaria → modo. */
    @OptIn(ExperimentalTestApi::class)
    @Test fun ordenDeFocoConTeclado() {
        regla.setContent {
            AgendaTheme(oscuro = false) {
                BandaSalida(
                    modelo = BandaSalidaMuestras.para(EstadoBanda.Espera),
                    onAccion = {},
                    onCambiarModo = {},
                    es24h = true,
                    reducirMovimiento = true,
                    bajoBarraDeEstado = false,
                )
            }
        }
        fun SemanticsNodeInteraction.enfocado() =
            fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
        val controles = mapOf(
            "primaria" to regla.onNodeWithText("Voy saliendo"),
            "secundaria" to regla.onNodeWithText("+5 min"),
            "modo" to regla.onNodeWithTag(EtiquetasBanda.MODO),
        )
        val recorrido = mutableListOf<String>()
        repeat(4) {
            regla.onRoot().performKeyInput { pressKey(Key.Tab) }
            regla.waitForIdle()
            controles.entries.firstOrNull { it.value.enfocado() }?.let { recorrido += it.key }
        }
        val i = recorrido.indexOf("primaria")
        assertTrue("recorrido: $recorrido", i >= 0)
        assertEquals("recorrido: $recorrido", listOf("primaria", "secundaria", "modo"), recorrido.subList(i, i + 3))
    }

    private fun celdas(texto: String) =
        regla.onAllNodes(hasText(texto) and hasAnyAncestor(hasTestTag("cifra")), useUnmergedTree = true)
            .fetchSemanticsNodes().size

    /** C8.2: tablero de 180 ms que solo anima el dígito que cambia. */
    @Test fun tableroSoloAnimaElDigitoQueCambia() = tablero(reducir = false)

    /** C8.4: con movimiento reducido, el dígito cambia sin capa saliente. */
    @Test fun tableroConMovimientoReducidoCorta() = tablero(reducir = true)

    private fun tablero(reducir: Boolean) {
        var hora by mutableStateOf("7:32")
        regla.setContent {
            AgendaTheme(oscuro = false) {
                CifraTablero(hora, AgendaTheme.tipo.horaSalida, AgendaTheme.colores.tinta, reducir, Modifier.testTag("cifra"))
            }
        }
        regla.mainClock.autoAdvance = false
        hora = "7:33"
        regla.mainClock.advanceTimeBy(90)
        if (reducir) {
            assertEquals("reducido: sin capa saliente", 0, celdas("2"))
        } else {
            assertEquals("a mitad: sigue saliendo el 2", 1, celdas("2"))
        }
        assertEquals("el 7 no se anima ni se duplica", 1, celdas("7"))
        assertEquals("decena de minutos + unidad nueva", 2, celdas("3"))
        regla.mainClock.advanceTimeBy(200)
        assertEquals("terminado a los 290 ms: ya no está el 2", 0, celdas("2"))
    }

    /** C8.2: el cambio de estado es un fundido de 200 ms (conviven las dos capas). */
    @Test fun cambioDeEstadoFunde() = cambioDeEstado(reducir = false)

    /** C8.4: con movimiento reducido, el cambio de estado es un corte. */
    @Test fun cambioDeEstadoConMovimientoReducidoCorta() = cambioDeEstado(reducir = true)

    private fun cambioDeEstado(reducir: Boolean) {
        var estado by mutableStateOf(EstadoBanda.Espera)
        regla.setContent {
            AgendaTheme(oscuro = false) {
                BandaSalida(
                    modelo = BandaSalidaMuestras.para(estado),
                    onAccion = {},
                    onCambiarModo = {},
                    es24h = true,
                    reducirMovimiento = reducir,
                    bajoBarraDeEstado = false,
                )
            }
        }
        regla.mainClock.autoAdvance = false
        estado = EstadoBanda.EnCamino
        regla.mainClock.advanceTimeBy(100)
        val aMitad = regla.onAllNodes(hasText("+5 min"), useUnmergedTree = true).fetchSemanticsNodes().size
        regla.mainClock.advanceTimeBy(250)
        val alFinal = regla.onAllNodes(hasText("+5 min"), useUnmergedTree = true).fetchSemanticsNodes().size
        assertEquals(if (reducir) "reducido: corte" else "a mitad del fundido", if (reducir) 0 else 1, aMitad)
        assertEquals("a los 350 ms solo queda el estado nuevo", 0, alFinal)
    }
}
