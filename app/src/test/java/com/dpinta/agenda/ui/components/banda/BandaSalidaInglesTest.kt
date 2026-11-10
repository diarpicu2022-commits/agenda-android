package com.dpinta.agenda.ui.components.banda

import com.dpinta.agenda.avisos.TextosAviso
import com.dpinta.agenda.ui.components.Lengua
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Las frases armadas en código, en inglés: sin restos del español y con el orden propio del inglés. */
class BandaSalidaInglesTest {

    private val en = Lengua.EN
    private val restos = Regex("""\b(Sal|salón|llegas|empieza|margen|hace|estimado|de la|en punto|minutos|Vas)\b""")

    @Test fun hora12hEnIngles() {
        assertEquals(HoraFormateada("7:32", "AM"), formatearHora(LocalTime.of(7, 32), es24h = false, l = en))
        assertEquals(HoraFormateada("7:32", "PM"), formatearHora(LocalTime.of(19, 32), es24h = false, l = en))
    }

    @Test fun horaHabladaEnIngles() {
        assertEquals("8 o'clock", horaHablada(LocalTime.of(8, 0), es24h = true, l = en))
        assertEquals("7:32 PM", horaHablada(LocalTime.of(19, 32), es24h = false, l = en))
    }

    @Test fun ningunEstadoDejaEspanol() {
        for (estado in EstadoBanda.entries) for (viejo in listOf(false, true)) {
            val t = textosBanda(BandaSalidaMuestras.para(estado, viejo), es24h = false, l = en)
            val todo = listOfNotNull(t.rotulo, t.cuentaAtras, t.sufijo, t.lineaEstado, t.modo, t.trayecto, t.frescura, t.fraseTalkBack)
                .joinToString(" | ")
            assertFalse("$estado viejo=$viejo: $todo", restos.containsMatchIn(todo))
        }
    }

    @Test fun rotulosEnIngles() {
        assertEquals("LEAVE NOW", textosBanda(BandaSalidaMuestras.para(EstadoBanda.SalYa, false), true, en).rotulo)
        assertEquals("RUNNING LATE", textosBanda(BandaSalidaMuestras.para(EstadoBanda.VasTarde, false), true, en).rotulo)
        assertTrue(fraseTalkBack(BandaSalidaMuestras.para(EstadoBanda.VasTarde, false), true, en).startsWith("Running late."))
    }

    @Test fun resumenDeLaMananaEnIngles() {
        assertEquals("Today: 3 things · first departure 7:32 AM", TextosAviso.resumen(3, LocalTime.of(7, 32), false, en).titulo)
        assertEquals("Today: 1 thing", TextosAviso.resumen(1, null, true, en).titulo)
    }
}
