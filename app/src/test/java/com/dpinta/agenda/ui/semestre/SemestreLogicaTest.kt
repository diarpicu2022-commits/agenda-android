package com.dpinta.agenda.ui.semestre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SemestreLogicaTest {

    private val hoy = LocalDate.of(2026, 9, 25)

    @Test
    fun `propone los festivos del semestre mientras no se toque la lista`() {
        val f = SemestreLogica.conFechas(SemestreFormulario(nombre = "2026-2", inicio = "3/8", fin = "28/11"), hoy)
        // Agosto–noviembre 2026: 7 ago, 17 ago, 12 oct, 2 nov y 16 nov.
        assertEquals(
            listOf(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16)),
            f.dias.keys.sorted(),
        )
        val sinUno = SemestreLogica.quitar(f, LocalDate.of(2026, 10, 12))
        // Tocada la lista, cambiar las fechas ya no la rehace.
        val otraFecha = SemestreLogica.conFechas(sinUno.copy(fin = "15/12"), hoy)
        assertEquals(4, otraFecha.dias.size)
    }

    @Test
    fun `el fin sin anio cae despues del inicio`() {
        val f = SemestreFormulario(inicio = "1/8", fin = "20/1")
        assertEquals(LocalDate.of(2027, 1, 20), SemestreLogica.fin(f, hoy))
        assertEquals(LocalDate.of(2026, 1, 20), SemestreLogica.fin(f.copy(fin = "20/1/2026"), hoy))
    }

    @Test
    fun `valida nombre, fechas y orden`() {
        val e = SemestreLogica.validar(SemestreFormulario(inicio = "32/8", fin = "1/8/2026"), hoy)
        assertEquals("Ponle un nombre, por ejemplo 2026-2", e.nombre)
        assertEquals("Escribe la fecha como 3/8", e.inicio)
        assertNull(SemestreLogica.validar(SemestreFormulario("2026-2", "3/8", "28/11"), hoy).fin)
        assertEquals("Termina antes de empezar", SemestreLogica.validar(SemestreFormulario("x", "3/8/2026", "1/8/2026"), hoy).fin)
    }

    @Test
    fun `anadir un dia dentro y fuera del semestre`() {
        val base = SemestreFormulario("2026-2", "3/8", "28/11", nuevoDia = "13/10", nuevoMotivo = " Receso ")
        val (con, error) = SemestreLogica.anadir(base, hoy)
        assertNull(error)
        assertEquals("Receso", con.dias[LocalDate.of(2026, 10, 13)])
        assertTrue(con.nuevoDia.isEmpty())
        assertEquals("Ese día no está dentro del semestre", SemestreLogica.anadir(base.copy(nuevoDia = "1/12"), hoy).second)
        assertEquals("Escribe la fecha como 12/10", SemestreLogica.anadir(base.copy(nuevoDia = "mañana"), hoy).second)
    }
}
