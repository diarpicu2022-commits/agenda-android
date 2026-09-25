package com.dpinta.agenda.ui.actividades

import com.dpinta.agenda.data.agenda.Actividad
import com.dpinta.agenda.data.agenda.Agenda
import com.dpinta.agenda.data.agenda.Lugar
import com.dpinta.agenda.domain.ActivityKind
import com.dpinta.agenda.domain.Occurrence
import com.dpinta.agenda.domain.TransportMode
import com.dpinta.agenda.domain.WeeklyRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class ActividadesMapeadorTest {

    private val hoy = LocalDate.of(2026, 9, 25)
    private fun act(id: Long, titulo: String, tipo: ActivityKind, lugar: Long?) =
        Actividad(id, titulo, tipo, "S-$id", lugar, Duration.ofMinutes(10), TransportMode.TRANSPORTE_PUBLICO)
    private fun serie(id: Long, dia: DayOfWeek, h: Int) =
        WeeklyRule(id, setOf(dia), LocalTime.of(h, 0), LocalTime.of(h + 1, 0), hoy.minusDays(30), hoy.plusDays(60))
    private fun una(id: Long, fecha: LocalDate) = Occurrence(id, null, fecha.atTime(21, 0), fecha.atTime(21, 30))

    @Test
    fun `agrupa por tipo, ordena por semana y cuenta usos de cada lugar`() {
        val agenda = Agenda(
            actividades = listOf(
                act(1, "Física", ActivityKind.CLASE, 10),
                act(2, "Cálculo", ActivityKind.CLASE, 10),
                act(3, "Turno", ActivityKind.TRABAJO, 20),
                act(4, "Parcial", ActivityKind.EXAMEN, 10),
                act(5, "Informe viejo", ActivityKind.PUNTUAL, null),
                act(6, "Informe", ActivityKind.PUNTUAL, null),
            ).associateBy { it.id },
            lugares = mapOf(10L to Lugar(10, "Campus"), 20L to Lugar(20, "Tienda centro"), 30L to Lugar(30, "Casa")),
            reglas = listOf(serie(1, WEDNESDAY, 10), serie(2, MONDAY, 8), serie(3, FRIDAY, 14), serie(4, FRIDAY, 7)),
            puntuales = listOf(una(5, hoy.minusDays(3)), una(6, hoy.plusDays(2))),
        )
        val lista = ActividadesMapeador.estado(agenda, hoy) as ActividadesUiState.Lista
        assertEquals(listOf(Grupo.Clases, Grupo.Trabajo, Grupo.Puntuales), lista.secciones.map { it.grupo })
        assertEquals(listOf("Cálculo", "Física", "Parcial"), lista.secciones[0].filas.map { it.titulo })
        // La puntual que ya pasó va al final.
        assertEquals(listOf("Informe", "Informe viejo"), lista.secciones[2].filas.map { it.titulo })
        assertEquals("Campus", lista.secciones[0].filas[0].lugar)
        assertEquals(
            listOf(LugarFila(10, "Campus", 3), LugarFila(30, "Casa", 0), LugarFila(20, "Tienda centro", 1)),
            lista.lugares,
        )
    }

    @Test
    fun `sin actividades es el primer uso`() {
        assertEquals(ActividadesUiState.Vacia, ActividadesMapeador.estado(Agenda.VACIA, hoy))
    }

    @Test
    fun `dias en texto`() {
        assertEquals("lun, mié y vie", textoDias(listOf(FRIDAY, MONDAY, WEDNESDAY)))
        assertEquals("lun a vie", textoDias(DayOfWeek.entries.take(5)))
        assertEquals("todos los días", textoDias(DayOfWeek.entries))
        assertEquals("mié", textoDias(listOf(WEDNESDAY)))
    }
}
