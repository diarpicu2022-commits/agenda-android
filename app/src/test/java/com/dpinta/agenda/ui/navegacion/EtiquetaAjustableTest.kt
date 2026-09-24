package com.dpinta.agenda.ui.navegacion

import org.junit.Assert.assertEquals
import org.junit.Test

/** Enmienda 2026-09-24: la etiqueta se reduce lo justo, nunca por debajo de 14 sp efectivos. */
class EtiquetaAjustableTest {

    /** Medidor falso: el texto cabe si el tamaño efectivo (sp × escala) es ≤ [limite]. */
    private fun cabeHasta(limite: Float, escala: Float) = { sp: Float -> sp * escala <= limite }

    @Test fun siCabeNoCambia() {
        assertEquals(14f, tamanoQueCabe(14f, 1f, cabe = cabeHasta(20f, 1f)))
        assertEquals(14f, tamanoQueCabe(14f, 1.3f, cabe = cabeHasta(20f, 1.3f)))
    }

    @Test fun seReduceLoJustoEnPasosDeMedioPunto() {
        // Escala 2: 14 sp son 28 efectivos; solo caben 22 efectivos → 11 sp.
        assertEquals(11f, tamanoQueCabe(14f, 2f, cabe = cabeHasta(22f, 2f)))
        // 23 efectivos → 11,5 sp (23/2), el mayor paso de medio punto que cabe.
        assertEquals(11.5f, tamanoQueCabe(14f, 2f, cabe = cabeHasta(23f, 2f)))
    }

    @Test fun nuncaBajaDe14EfectivosAunqueNoQuepa() {
        // Escala 2: el mínimo del estilo es 7 sp (14 efectivos).
        assertEquals(7f, tamanoQueCabe(14f, 2f, cabe = cabeHasta(5f, 2f)))
    }

    @Test fun sinEscalaNoHayMargenParaReducir() {
        assertEquals(14f, tamanoQueCabe(14f, 1f, cabe = { false }))
    }
}
