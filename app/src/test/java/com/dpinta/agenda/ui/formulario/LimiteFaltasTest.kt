package com.dpinta.agenda.ui.formulario

import com.dpinta.agenda.domain.ActivityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Límite de faltas en el formulario: opcional, de 0 a 99, solo en clases y exámenes. */
class LimiteFaltasTest {

    private val hoy = LocalDate.of(2026, 11, 10)
    private val base = Formulario.nuevo(hoy).copy(titulo = "Cálculo", dias = setOf(DayOfWeek.MONDAY), inicio = "8:00", fin = "10:00")

    @Test fun vacioEsSinLimite() {
        val v = Validador.validar(base, hoy)
        assertNull(v.aGuardar!!.faltasMax)
    }

    @Test fun guardaElLimite() {
        assertEquals(4, Validador.validar(base.copy(faltas = "4"), hoy).aGuardar!!.faltasMax)
    }

    @Test fun fueraDeRangoEsError() {
        assertEquals(Validador.MSG_FALTAS, Validador.validar(base.copy(faltas = "120"), hoy).errores[Campo.FALTAS])
        assertEquals(Validador.MSG_FALTAS, Validador.validar(base.copy(faltas = "x"), hoy).errores[Campo.FALTAS])
    }

    @Test fun enUnTurnoNoAplica() {
        val v = Validador.validar(base.copy(tipo = ActivityKind.TRABAJO, faltas = "4"), hoy)
        assertNull(v.aGuardar!!.faltasMax)
    }
}
