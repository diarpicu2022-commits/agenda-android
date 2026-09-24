package com.dpinta.agenda.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpinta.agenda.MainActivity
import com.dpinta.agenda.ui.formulario.EtiquetasFormulario
import com.dpinta.agenda.ui.formulario.Validador
import com.dpinta.agenda.ui.navegacion.EtiquetasBarra
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Flujo 1 del anexo §7: crear una actividad semanal, con validación, resumen y guardado. */
@RunWith(AndroidJUnit4::class)
class FormularioFlujoTest {

    @get:Rule val regla = createAndroidComposeRule<MainActivity>()

    private fun campo(nombre: String) = regla.onNodeWithTag("campo-$nombre")

    @Test fun crearUnaClaseSemanal() {
        regla.onNodeWithTag(EtiquetasBarra.CREAR).performClick()
        regla.onNodeWithText("Nueva actividad").assertIsDisplayed()

        // Guardar vacío: errores con su texto (C10) y nada se cierra.
        regla.onNodeWithTag(EtiquetasFormulario.GUARDAR).performClick()
        regla.onNodeWithText(Validador.MSG_TITULO).performScrollTo().assertIsDisplayed()

        campo("Nombre").performScrollTo().performTextInput("Álgebra lineal")
        val hoy = LocalDate.now().dayOfWeek.name.lowercase()
        regla.onNodeWithTag("dia-$hoy").performScrollTo().performClick()
        campo("Empieza").performScrollTo().performTextInput("23:00")
        campo("Termina").performScrollTo().performTextInput("23:30")
        campo("Lugar").performScrollTo().performTextInput("Campus")
        campo("Salón").performScrollTo().performTextInput("C-101")

        regla.onNodeWithTag(EtiquetasFormulario.RESUMEN).performScrollTo()
        regla.onNode(hasText("Álgebra lineal: ", substring = true)).assertIsDisplayed()

        regla.onNodeWithTag(EtiquetasFormulario.GUARDAR).performClick()
        regla.waitUntil(5_000) { regla.onAllNodes(hasText("Guardado: Álgebra lineal")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun elMapaEstaPendiente() {
        regla.onNodeWithTag(EtiquetasBarra.CREAR).performClick()
        regla.onNodeWithTag(EtiquetasFormulario.MAPA).performScrollTo().performClick()
        regla.onNodeWithText("Mapa pendiente").assertIsDisplayed()
        regla.onNodeWithText("Escribir el lugar").performClick()
        regla.onNodeWithText("Nueva actividad").assertIsDisplayed()
    }
}
