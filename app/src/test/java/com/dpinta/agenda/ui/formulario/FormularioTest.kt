package com.dpinta.agenda.ui.formulario

import com.dpinta.agenda.data.agenda.AgendaEnMemoria
import com.dpinta.agenda.data.agenda.Cuando
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.ui.hoy.ConflictoFila
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Validación, interpretación, frase-resumen y conflictos del formulario (paso 5). */
class FormularioTest {

    private val hoy = LocalDate.of(2026, 9, 24) // jueves
    private val base = Formulario.nuevo(hoy)
    private val calculo = base.copy(
        titulo = "Cálculo diferencial",
        dias = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        inicio = "8:00",
        fin = "10:00",
        lugar = "Campus",
        salon = "B-204",
    )

    @Test fun horas() {
        assertEquals(LocalTime.of(8, 0), Interprete.hora("8"))
        assertEquals(LocalTime.of(8, 0), Interprete.hora("08:00"))
        assertEquals(LocalTime.of(8, 30), Interprete.hora("830"))
        assertEquals(LocalTime.of(20, 30), Interprete.hora("20.30"))
        assertEquals(LocalTime.of(20, 0), Interprete.hora("8:00 p. m."))
        assertEquals(LocalTime.of(0, 15), Interprete.hora("12:15 am"))
        assertNull(Interprete.hora("25:00"))
        assertNull(Interprete.hora("8:75"))
        assertNull(Interprete.hora("ocho"))
        assertNull(Interprete.hora(""))
    }

    @Test fun fechas() {
        assertEquals(LocalDate.of(2026, 9, 24), Interprete.fecha("24/9/2026", hoy))
        assertEquals(LocalDate.of(2026, 9, 24), Interprete.fecha("24-9", hoy))
        assertEquals(LocalDate.of(2027, 1, 20), Interprete.fecha("20/1/27", hoy))
        assertNull(Interprete.fecha("31/2/2026", hoy))
        assertNull(Interprete.fecha("mañana", hoy))
    }

    @Test fun nuevoTraeLasFechasEscritas() {
        assertEquals("24/9/2026", base.desde)
        assertEquals("21/1/2027", base.hasta) // 17 semanas
        assertEquals("10", base.margen)
        assertEquals("15", base.aviso)
    }

    @Test fun vacioDaTodosLosErroresConSuTexto() {
        val v = Validador.validar(base, hoy)
        assertEquals(Validador.MSG_TITULO, v.errores[Campo.TITULO])
        assertEquals(Validador.MSG_DIAS, v.errores[Campo.DIAS])
        assertEquals(Validador.MSG_HORA, v.errores[Campo.INICIO])
        assertEquals(Validador.MSG_HORA, v.errores[Campo.FIN])
        assertNull(v.aGuardar)
    }

    @Test fun finAntesDeInicioYSerieAlReves() {
        val v = Validador.validar(calculo.copy(fin = "7:00", hasta = "1/9/2026"), hoy)
        assertEquals(Validador.MSG_FIN_ANTES, v.errores[Campo.FIN])
        assertEquals(Validador.MSG_HASTA_ANTES, v.errores[Campo.HASTA])
    }

    @Test fun margenYAvisoFueraDeRango() {
        val v = Validador.validar(calculo.copy(margen = "500", aviso = "x"), hoy)
        assertEquals(Validador.MSG_MARGEN, v.errores[Campo.MARGEN])
        assertEquals(Validador.MSG_AVISO, v.errores[Campo.AVISO])
    }

    @Test fun validoProduceLoQueSeGuarda() {
        val a = Validador.validar(calculo, hoy).aGuardar!!
        assertEquals("Cálculo diferencial", a.titulo)
        assertEquals(ActivityKind.CLASE, a.tipo)
        assertEquals(Duration.ofMinutes(10), a.margen)
        val c = a.cuando as Cuando.Semanal
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), c.dias)
        assertEquals(LocalTime.of(8, 0), c.inicio)
    }

    @Test fun puntualNoPideDias() {
        val v = Validador.validar(calculo.copy(semanal = false, dias = emptySet(), fecha = "30/9/2026"), hoy)
        assertTrue(v.valida)
        assertEquals(LocalDate.of(2026, 9, 30), (v.aGuardar!!.cuando as Cuando.Puntual).fecha)
    }

    @Test fun resumenSemanal() {
        val a = Validador.validar(calculo, hoy).aGuardar!!
        assertEquals(
            "Cálculo diferencial: cada lunes y miércoles, de 8:00 a 10:00, en B-204 · Campus, del 24 de septiembre al 21 de enero de 2027.",
            resumen(a, es24h = true),
        )
        val laborables = Validador.validar(calculo.copy(dias = (1..5).map { DayOfWeek.of(it) }.toSet()), hoy).aGuardar!!
        assertTrue(resumen(laborables, true).contains("de lunes a viernes"))
    }

    @Test fun resumenPuntualEn12h() {
        val a = Validador.validar(calculo.copy(semanal = false, fecha = "30/9/2026", salon = "", lugar = ""), hoy).aGuardar!!
        assertEquals("Cálculo diferencial: el miércoles 30 de septiembre, de 8:00 a. m. a 10:00 a. m.", resumen(a, es24h = false))
    }

    @Test fun conflictosConElEjemplo() {
        val agenda = AgendaEnMemoria.ejemplo(hoy)
        val inventario = Validador.validar(
            base.copy(titulo = "Inventario", dias = setOf(DayOfWeek.MONDAY), inicio = "10:15", fin = "11:00", lugar = "Tienda centro"),
            hoy,
        ).aGuardar!!
        val avisos = conflictosDelBorrador(agenda, inventario) { a, b -> if (a != b) 35L else null }
        assertTrue(avisos.any { it.conflicto == ConflictoFila.Cruce("Física mecánica", LocalTime.of(10, 30)) })
        assertTrue(avisos.any { it.conflicto == ConflictoFila.Traslado(15, 35) && it.con == "Cálculo diferencial" })
        assertEquals(DayOfWeek.MONDAY, avisos.first().fecha.dayOfWeek)
        assertEquals("lunes: Se cruza con Física mecánica 10:30", textoAviso(avisos.first { it.conflicto is ConflictoFila.Cruce }, true))
    }

    @Test fun lugarNuevoNoAfirmaConflictosDeTraslado() {
        val agenda = AgendaEnMemoria.ejemplo(hoy)
        val a = Validador.validar(base.copy(titulo = "Gimnasio", dias = setOf(DayOfWeek.MONDAY), inicio = "10:10", fin = "10:25", lugar = "Gimnasio norte"), hoy).aGuardar!!
        assertTrue(conflictosDelBorrador(agenda, a) { _, _ -> 99L }.none { it.conflicto is ConflictoFila.Traslado })
    }

    @Test fun editarReconstruyeElFormulario() {
        val agenda = AgendaEnMemoria.ejemplo(hoy)
        val f = formularioDe(agenda, 1, hoy)!!
        assertEquals("Cálculo diferencial", f.titulo)
        assertEquals("8:00", f.inicio)
        assertEquals("Campus", f.lugar)
        assertEquals("B-204", f.salon)
        assertEquals(7, f.dias.size)
        assertNull(formularioDe(agenda, 999, hoy))
    }
}
