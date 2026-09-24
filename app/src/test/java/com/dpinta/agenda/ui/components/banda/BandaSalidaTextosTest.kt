package com.dpinta.agenda.ui.components.banda

import com.dpinta.agenda.ui.theme.AgendaColoresClaro
import com.dpinta.agenda.ui.theme.AgendaColoresOscuro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

/** Mapeo estado → texto, acciones y colores. Texto literal del contrato (C1, C3, C9.3, C10). */
class BandaSalidaTextosTest {

    private fun t(estado: EstadoBanda, viejo: Boolean = false, es24h: Boolean = true) =
        textosBanda(BandaSalidaMuestras.para(estado, viejo), es24h)

    // ---- C1.1 reloj ----

    @Test fun hora24h() {
        assertEquals(HoraFormateada("7:32", null), formatearHora(LocalTime.of(7, 32), es24h = true))
        assertEquals(HoraFormateada("19:05", null), formatearHora(LocalTime.of(19, 5), es24h = true))
    }

    @Test fun hora12hConSufijoAparte() {
        assertEquals(HoraFormateada("7:32", "a. m."), formatearHora(LocalTime.of(7, 32), es24h = false))
        assertEquals(HoraFormateada("12:30", "p. m."), formatearHora(LocalTime.of(12, 30), es24h = false))
        assertEquals(HoraFormateada("12:05", "a. m."), formatearHora(LocalTime.of(0, 5), es24h = false))
        assertEquals(HoraFormateada("7:32", "p. m."), formatearHora(LocalTime.of(19, 32), es24h = false))
    }

    // ---- C10 textos por estado ----

    @Test fun espera() {
        val x = t(EstadoBanda.Espera)
        assertEquals("SAL A LAS", x.rotulo)
        assertEquals("7:32", x.cifra)
        assertNull(x.cuentaAtras)
        assertNull(x.lineaEstado)
        assertEquals("en bus", x.modo)
        assertEquals("23 min + 7 de margen", x.trayecto)
        assertEquals("hace 3 min", x.frescura)
    }

    @Test fun preparateLlevaCuentaAtras() {
        val x = t(EstadoBanda.Preparate)
        assertEquals("SAL A LAS", x.rotulo)
        assertEquals("en 12 min", x.cuentaAtras)
    }

    @Test fun salYa() {
        val x = t(EstadoBanda.SalYa)
        assertEquals("SAL YA", x.rotulo)
        assertEquals("Cálculo diferencial empieza 8:00", x.lineaEstado)
    }

    @Test fun vasTarde() {
        val x = t(EstadoBanda.VasTarde)
        assertEquals("VAS TARDE", x.rotulo)
        assertEquals("8:00", x.cifra)
        assertEquals("llegas 8:04 a B-204", x.lineaEstado)
    }

    @Test fun enCamino() {
        val x = t(EstadoBanda.EnCamino)
        assertEquals("LLEGAS", x.rotulo)
        assertEquals("7:58", x.cifra)
    }

    @Test fun sinTrasladoNoTieneRotuloNiTrayecto() {
        val x = t(EstadoBanda.SinTraslado)
        assertNull(x.rotulo)
        assertEquals("10:30", x.cifra)
        assertNull(x.modo)
        assertNull(x.trayecto)
        assertNull(x.frescura)
        assertNull(x.primaria)
    }

    @Test fun datoViejoMarcaAproxYEstimado() {
        val x = t(EstadoBanda.Espera, viejo = true)
        assertEquals("aprox.", x.sufijo)
        assertEquals("estimado hace 25 min", x.frescura)
        val x12 = t(EstadoBanda.Espera, viejo = true, es24h = false)
        assertEquals("a. m. aprox.", x12.sufijo)
    }

    @Test fun enDoceHorasElSufijoVaEnSuLinea() {
        val x = t(EstadoBanda.Espera, es24h = false)
        assertEquals("7:32", x.cifra)
        assertEquals("a. m.", x.sufijo)
        assertEquals("Cálculo diferencial empieza 8:00 a. m.", t(EstadoBanda.SalYa, es24h = false).lineaEstado)
    }

    // ---- C1.2 acciones ----

    @Test fun acciones() {
        assertEquals(AccionBanda.VoySaliendo, t(EstadoBanda.Espera).primaria)
        assertEquals(AccionBanda.MasCinco, t(EstadoBanda.Espera).secundaria)
        assertEquals(AccionBanda.MasCinco, t(EstadoBanda.SalYa).secundaria)
        assertNull(t(EstadoBanda.VasTarde).secundaria)
        assertEquals(AccionBanda.YaLlegue, t(EstadoBanda.EnCamino).primaria)
        assertEquals("Voy saliendo", AccionBanda.VoySaliendo.etiqueta)
        assertEquals("+5 min", AccionBanda.MasCinco.etiqueta)
    }

    // ---- C1.3 transporte ----

    @Test fun cuatroModosEnCiclo() {
        assertEquals(listOf("en bus", "a pie", "en carro", "en moto"), ModoTransporte.entries.map { it.etiqueta })
        assertEquals(ModoTransporte.Bus, ModoTransporte.Moto.siguiente())
    }

    // ---- C9.3 TalkBack ----

    @Test fun fraseDelContrato() {
        assertEquals(
            "Sal a las 7 y 32 para Cálculo diferencial, salón B-204, 23 minutos en bus más 7 de margen, " +
                "calculado hace 3 minutos",
            t(EstadoBanda.Espera).fraseTalkBack,
        )
    }

    @Test fun fraseEnDoceHoras() {
        assertEquals("8 en punto de la mañana", horaHablada(LocalTime.of(8, 0), es24h = false))
        assertEquals("7 y 32 de la noche", horaHablada(LocalTime.of(19, 32), es24h = false))
    }

    // ---- C3 colores ----

    @Test fun elAmbarSoloEnLaBandaYNuncaEnTardeNiSinTraslado() {
        for (colores in listOf(AgendaColoresClaro, AgendaColoresOscuro)) {
            for (e in listOf(EstadoBanda.Espera, EstadoBanda.Preparate, EstadoBanda.SalYa, EstadoBanda.EnCamino)) {
                assertEquals(ColoresBanda(colores.senal, colores.sobreSenal), colores.coloresBanda(e))
            }
            assertEquals(ColoresBanda(colores.tarde, colores.sobreTarde), colores.coloresBanda(EstadoBanda.VasTarde))
            assertEquals(ColoresBanda(colores.papel, colores.tinta), colores.coloresBanda(EstadoBanda.SinTraslado))
            assertNotEquals(colores.senal, colores.coloresBanda(EstadoBanda.SinTraslado).fondo)
        }
    }
}
