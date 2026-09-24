package com.dpinta.agenda.ui.hoy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/** Texto literal de C10 para los dos conflictos (enmienda 2026-09-23 para el solape). */
class TextoConflictoTest {

    @Test fun traslado() {
        assertEquals("25 min de traslado, necesitas 35", textoConflicto(ConflictoFila.Traslado(25, 35), es24h = true))
    }

    @Test fun cruceEn24h() {
        assertEquals("Se cruza con Física 9:00", textoConflicto(ConflictoFila.Cruce("Física", LocalTime.of(9, 0)), es24h = true))
        assertEquals("Se cruza con Turno 14:30", textoConflicto(ConflictoFila.Cruce("Turno", LocalTime.of(14, 30)), es24h = true))
    }

    @Test fun cruceEn12h() {
        assertEquals("Se cruza con Física 9:00 a. m.", textoConflicto(ConflictoFila.Cruce("Física", LocalTime.of(9, 0)), es24h = false))
    }
}
