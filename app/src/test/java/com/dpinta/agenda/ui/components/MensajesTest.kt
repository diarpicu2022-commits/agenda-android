package com.dpinta.agenda.ui.components

import com.dpinta.agenda.domain.ColombianHolidays
import com.dpinta.agenda.ui.formulario.Validador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** La tabla de mensajes en inglés cubre todos los festivos y los errores del formulario. */
class MensajesTest {

    @Test fun todosLosFestivosTienenTraduccion() {
        for (anio in 2026..2028) for (f in ColombianHolidays.of(anio)) {
            assertTrue("falta ${f.name}", f.name in Mensajes.claves)
        }
    }

    @Test fun festivoConPrefijo() {
        assertEquals("Holiday: Navidad (Christmas)", Mensajes.de("Festivo: Navidad", Lengua.EN))
        assertEquals("Festivo: Navidad", Mensajes.de("Festivo: Navidad", Lengua.ES))
    }

    @Test fun motivoDeLaPersonaSeDejaIgual() {
        assertEquals("Semana de parciales", Mensajes.de("Semana de parciales", Lengua.EN))
    }

    @Test fun erroresDelFormularioEnIngles() {
        for (m in listOf(Validador.MSG_TITULO, Validador.MSG_TITULO_LARGO, Validador.MSG_DIAS, Validador.MSG_HORA,
            Validador.MSG_FIN_ANTES, Validador.MSG_FECHA, Validador.MSG_HASTA_ANTES, Validador.MSG_MARGEN, Validador.MSG_AVISO)) {
            assertNotEquals(m, Validador.mensaje(m, Lengua.EN))
        }
    }
}
